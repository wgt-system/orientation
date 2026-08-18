#!/usr/bin/env python3
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse


def match(name: str, match_id: str, longitude: float, latitude: float) -> dict:
    return {
        "type": "PLACE",
        "name": name,
        "id": match_id,
        "lat": latitude,
        "lon": longitude,
        "country": "DE",
        "tokens": [],
        "areas": [],
        "score": 1.0,
    }


class Handler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        params = parse_qs(parsed.query)

        if parsed.path == "/api/v1/geocode":
            query = params.get("text", [""])[0].lower()
            if "start" in query:
                result = match("Smoke Start", "smoke:start", 10.0067, 53.5526)
            elif "destination" in query:
                result = match("Smoke Destination", "smoke:destination", 9.9921, 53.5504)
            else:
                result = match("Smoke Place", "smoke:place", 10.0005, 53.5515)
            self.respond([result])
            return

        if parsed.path == "/api/v1/reverse-geocode":
            try:
                latitude, longitude = [float(value) for value in params.get("place", ["53.5515,10.0005"])[0].split(",", 1)]
            except (TypeError, ValueError):
                self.send_error(400)
                return
            self.respond([match("Smoke Reverse", "smoke:reverse", longitude, latitude)])
            return

        if parsed.path == "/api/v1/health":
            self.respond({"status": "UP"})
            return

        self.send_error(404)

    def respond(self, payload) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format: str, *args: object) -> None:
        print(format % args, flush=True)


if __name__ == "__main__":
    server = ThreadingHTTPServer(("127.0.0.1", 8999), Handler)
    print("MOTIS geocode smoke stub listening on 127.0.0.1:8999", flush=True)
    server.serve_forever()
