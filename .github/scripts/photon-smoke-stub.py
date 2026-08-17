#!/usr/bin/env python3
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse


def feature(name: str, osm_id: str, longitude: float, latitude: float) -> dict:
    return {
        "type": "Feature",
        "geometry": {"type": "Point", "coordinates": [longitude, latitude]},
        "properties": {
            "osm_type": "N",
            "osm_id": osm_id,
            "name": name,
            "city": "Hamburg",
            "state": "Hamburg",
            "country": "Germany",
            "countrycode": "DE",
        },
    }


class Handler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        params = parse_qs(parsed.query)

        if parsed.path == "/api":
            query = params.get("q", [""])[0].lower()
            if "start" in query:
                result = feature("Smoke Start", "9001", 10.0067, 53.5526)
            elif "destination" in query:
                result = feature("Smoke Destination", "9002", 9.9921, 53.5504)
            else:
                result = feature("Smoke Place", "9003", 10.0005, 53.5515)
            self.respond({"type": "FeatureCollection", "features": [result]})
            return

        if parsed.path == "/reverse":
            try:
                latitude = float(params.get("lat", ["53.5515"])[0])
                longitude = float(params.get("lon", ["10.0005"])[0])
            except ValueError:
                self.send_error(400)
                return
            self.respond(
                {
                    "type": "FeatureCollection",
                    "features": [feature("Smoke Reverse", "9004", longitude, latitude)],
                }
            )
            return

        self.send_error(404)

    def respond(self, payload: dict) -> None:
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
    print("Photon smoke stub listening on 127.0.0.1:8999", flush=True)
    server.serve_forever()
