import base64

def bypass_dummy_video(url: str, cookie: str) -> str:
    """
    Decodes the 'urlprefix' from a cookie to bypass anti-piracy dummy video traps.
    """
    if "urlprefix=" not in cookie:
        return url
        
    try:
        start_key = "urlprefix="
        start_idx = cookie.find(start_key) + len(start_key)
        
        # Find end of the prefix (colon or semicolon)
        end_idx = cookie.find(":", start_idx)
        if end_idx == -1:
            end_idx = cookie.find(";", start_idx)
        if end_idx == -1:
            end_idx = len(cookie)
            
        base64_str = cookie[start_idx:end_idx]
        
        # Add padding if needed
        padding = (4 - len(base64_str) % 4) % 4
        base64_str += "=" * padding
        
        decoded_path = base64.b64decode(base64_str).decode('utf-8')
        
        # Construct the final .m3u8 URL
        if decoded_path.endswith("/"):
            return f"{decoded_path}index.m3u8"
        else:
            return f"{decoded_path}/index.m3u8"
    except Exception as e:
        print(f"Bypass failed: {e}")
        return url

def clean_stream_list(streams: list, global_cookie: str = "") -> list:
    """
    Filters and cleans the stream list from the official API.
    """
    cleaned = []
    for st in streams:
        url = st.get("url", "")
        cookie = st.get("signCookie", global_cookie)
        
        # Skip known dummy host if no bypass is possible (though usually bypass handles it)
        is_dummy = "macdn.aoneroom.com" in url or "please_update" in url
        
        real_url = url
        if is_dummy and "urlprefix=" in cookie:
            real_url = bypass_dummy_video(url, cookie)
            is_dummy = False # No longer dummy after bypass
            
        if not is_dummy and real_url:
            quality = st.get("quality") or st.get("resolutions", "Auto")
            if quality and not str(quality).endswith("p") and str(quality).isdigit():
                quality = f"{quality}p"
                
            cleaned.append({
                "quality": quality,
                "url": real_url,
                "cookie": cookie,
                "size": st.get("size") or st.get("fileSize", "Dynamic")
            })
    return cleaned
