import httpx
from fastapi import FastAPI, Query, HTTPException
from typing import Optional
import json
import time
import uuid
from urllib.parse import quote

from utils.crypto import generate_signature, get_client_info_json
from utils.extractor import clean_stream_list

app = FastAPI(title="Movie Proxy API")

OFFICIAL_BASE_URL = "https://api.inmoviebox.com"
DEFAULT_HOST = "api6.aoneroom.com"

# Global state for server session
class AppSession:
    def __init__(self):
        # Initial fallback token
        self.token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjcwNjU5NDg0MTAyMTM4MTYyMzIsInV0cCI6MSwiZXhwIjoxNzkxNzMyMjMzLCJpYXQiOjE3ODM5NTU5MzN9.7iyEzTj4vWAbOF0oXwNnZ0p3Nc1QaO6K9eMiGFyVfGs"
        self.device_id = uuid.uuid4().hex
        self.last_refreshed = 0

session = AppSession()

async def bootstrap_token():
    """Mirror of Android's bootstrapGuestToken() with strict header compliance."""
    endpoint = "/wefeed-mobile-bff/tab-operating"
    params = {"host": "api.inmoviebox.com", "page": "1", "pageSize": "24", "tabId": "1"}
    
    # Sort keys for signature parity
    sorted_keys = sorted(params.keys())
    query_parts = [f"{k}={quote(str(params[k]))}" for k in sorted_keys]
    query_str = "?" + "&".join(query_parts)
    
    full_path_for_sig = f"{endpoint}{query_str}"
    
    timestamp, signature, client_token = generate_signature("GET", full_path_for_sig)
    client_info = get_client_info_json(session.device_id)
    
    headers = {
        "User-Agent": "MovieBoxPro/16.2.1 (Android 14; com.community.mbox.in)",
        "Accept": "application/json",
        "Content-Type": "application/json;charset=UTF-8",
        "X-Sign-Version": "2.0",
        "appid": "4U01pxRu278GqCZKY9",
        "region": "IN",
        "lang": "en",
        "os": "android",
        "X-Timestamp": timestamp,
        "Referer": "https://api.inmoviebox.com/",
        "X-Client-Token": client_token,
        "x-tr-signature": signature,
        "X-Play-Mode": "2",
        "X-Client-Info": client_info,
        "X-Client-Status": "1"
    }

    async with httpx.AsyncClient(timeout=15.0) as client:
        try:
            print(f"Attempting token bootstrap for device: {session.device_id}...")
            response = await client.get(f"{OFFICIAL_BASE_URL}{endpoint}", params=params, headers=headers)
            
            # Extract token from headers
            raw_token = response.headers.get("x-user") or response.headers.get("X-User")
            if raw_token:
                if raw_token.startswith("{"):
                    data = json.loads(raw_token)
                    new_token = data.get("token")
                    if new_token:
                        session.token = new_token
                        print(f"Successfully bootstrapped new token: {session.token[:20]}...")
                else:
                    session.token = raw_token
                    print(f"Successfully bootstrapped new raw token: {session.token[:20]}...")
                
                session.last_refreshed = time.time()
                return True
            else:
                print(f"Bootstrap failed: x-user header not found. Status: {response.status_code}")
                # Log response snippet for debugging
                print(f"Response Headers: {response.headers}")
        except Exception as e:
            print(f"Token bootstrap exception: {str(e)}")
    return False

async def make_official_request(method: str, endpoint: str, params: dict = None, body: dict = None, retry_on_401=True):
    url = f"{OFFICIAL_BASE_URL}{endpoint}"
    params = params or {}
    if "host" not in params:
        params["host"] = DEFAULT_HOST
        
    sorted_keys = sorted(params.keys())
    query_parts = [f"{k}={quote(str(params[k]))}" for k in sorted_keys]
    query_str = "?" + "&".join(query_parts) if query_parts else ""
    
    full_path_for_sig = f"{endpoint}{query_str}"
    body_bytes = json.dumps(body, separators=(',', ':')).encode('utf-8') if body else b""
    
    timestamp, signature, client_token = generate_signature(method, full_path_for_sig, body_bytes)
    client_info = get_client_info_json(session.device_id)
    
    headers = {
        "User-Agent": "MovieBoxPro/16.2.1 (Android 14; com.community.mbox.in)",
        "Accept": "application/json",
        "Content-Type": "application/json;charset=UTF-8",
        "X-Sign-Version": "2.0",
        "appid": "4U01pxRu278GqCZKY9",
        "region": "IN",
        "lang": "en",
        "os": "android",
        "X-Timestamp": timestamp,
        "Referer": "https://api.inmoviebox.com/",
        "X-Client-Token": client_token,
        "x-tr-signature": signature,
        "X-Play-Mode": "2",
        "X-Client-Info": client_info,
        "X-Client-Status": "1",
        "Authorization": f"Bearer {session.token}"
    }
    
    async with httpx.AsyncClient(timeout=20.0) as client:
        try:
            if method.upper() == "POST":
                response = await client.post(url, params=params, content=body_bytes, headers=headers)
            else:
                response = await client.get(url, params=params, headers=headers)
                
            if response.status_code == 401 and retry_on_401:
                print("401 Unauthorized: JWT Expired. Retrying token bootstrap...")
                if await bootstrap_token():
                    return await make_official_request(method, endpoint, params, body, retry_on_401=False)
            
            response.raise_for_status()
            return response.json()
        except httpx.HTTPStatusError as e:
            # Return detailed error from official API
            err_body = response.text
            raise HTTPException(status_code=e.response.status_code, detail=f"Official API Error: {err_body}")
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Proxy error: {str(e)}")

@app.get("/health")
def health_check():
    return {"status": "ok", "device": session.device_id, "token_age": int(time.time() - session.last_refreshed)}

@app.get("/trending")
async def get_trending():
    data = await make_official_request("POST", "/wefeed-mobile-bff/subject-api/trending/v2", body={})
    items = data.get("data", {}).get("list", []) or data.get("data", {}).get("items", []) or []
    return {"code": 0, "data": items}

@app.get("/search")
async def search(q: str, page: int = 1):
    body = {"keyword": q, "page": page, "pageSize": 20}
    data = await make_official_request("POST", "/wefeed-mobile-bff/subject-api/search", body=body)
    items = data.get("data", {}).get("list", []) or data.get("data", {}).get("items", []) or []
    return {"code": 0, "data": {"items": items}}

@app.get("/detail/{subject_id}")
async def get_detail(subject_id: str):
    data = await make_official_request("GET", "/wefeed-mobile-bff/subject-api/get", params={"subjectId": subject_id})
    return data

@app.get("/stream/{subject_id}")
async def get_stream(
    subject_id: str, 
    season: int = 1, 
    episode: int = 1, 
    quality: str = "720p",
    resource_id: Optional[str] = None
):
    params = {"subjectId": subject_id, "se": season, "ep": episode, "quality": quality}
    if resource_id:
        params["resourceId"] = resource_id
        
    data = await make_official_request("GET", "/wefeed-mobile-bff/subject-api/play-info", params=params)
    raw_streams = data.get("data", {}).get("streamList", []) or data.get("data", {}).get("streams", []) or []
    global_cookie = data.get("signCookie", "")
    cleaned = clean_stream_list(raw_streams, global_cookie)
    first = cleaned[0] if cleaned else {}
    
    return {
        "code": 0,
        "url": first.get("url", ""),
        "quality": quality,
        "cookie": first.get("cookie", global_cookie),
        "streamList": cleaned,
        "subtitles": []
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
