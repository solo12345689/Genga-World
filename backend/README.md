# Movie Proxy Backend (Python/FastAPI)

This is a high-performance proxy and scraper for official movie APIs, designed to be used with the Genga World Android app.

## Features
- **Automatic Signature Generation**: Handles `x-tr-signature` cryptographic headers.
- **Dummy Video Bypass**: Decodes `urlprefix` cookies to recover real streaming links.
- **Data Flattening**: Simplifies complex nested JSON responses for mobile consumption.
- **Async Architecture**: Built with FastAPI and HTTPX for maximum concurrency.

## Local Development

1. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

2. Run the server:
   ```bash
   python main.py
   ```
   The API will be available at `http://localhost:8000`.

## Deployment to Render

1. Create a new **Web Service** on [Render](https://render.com).
2. Connect your GitHub repository.
3. Render will automatically detect the `Dockerfile` and deploy the service.

## API Endpoints
- `GET /trending`: Get trending items.
- `GET /search?q=query`: Search for titles.
- `GET /detail/{id}`: Get full metadata for a subject.
- `GET /stream/{id}?quality=720p`: Resolve real playback links.
