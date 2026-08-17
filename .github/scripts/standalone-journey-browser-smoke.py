#!/usr/bin/env python3
import argparse
import json
import time
import urllib.error
import urllib.request
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

WEBDRIVER = "http://127.0.0.1:9515"
APP_URL = "http://127.0.0.1:4174/app.html"


def load_json(path: str) -> dict:
    return json.loads(Path(path).read_text(encoding="utf-8"))


def photon_feature(name: str, longitude: float, latitude: float) -> dict:
    return {
        "type": "Feature",
        "geometry": {"type": "Point", "coordinates": [longitude, latitude]},
        "properties": {
            "osm_type": "N",
            "osm_id": "9501",
            "name": name,
            "city": "Aachen",
            "state": "North Rhine-Westphalia",
            "country": "Germany",
            "countrycode": "DE",
        },
    }


def serve_photon(request_path: str) -> None:
    request = load_json(request_path)
    origin = request["origin"]

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self) -> None:
            if urlparse(self.path).path != "/api":
                self.send_error(404)
                return
            payload = {
                "type": "FeatureCollection",
                "features": [photon_feature("Journey Start", origin["longitude"], origin["latitude"])],
            }
            body = json.dumps(payload).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def log_message(self, format: str, *args: object) -> None:
            print(format % args, flush=True)

    server = ThreadingHTTPServer(("127.0.0.1", 8998), Handler)
    print("Journey Photon stub listening on 127.0.0.1:8998", flush=True)
    server.serve_forever()


def webdriver(method: str, path: str, payload: dict | None = None):
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        WEBDRIVER + path,
        data=data,
        headers={"Content-Type": "application/json"},
        method=method,
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            body = json.load(response)
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"WebDriver {method} {path} failed: {error.code} {detail}") from error
    return body.get("value")


def execute(session: str, script: str):
    return webdriver("POST", f"/session/{session}/execute/sync", {"script": script, "args": []})


def wait_for(session: str, description: str, script: str, timeout: float = 30.0):
    deadline = time.time() + timeout
    last = None
    while time.time() < deadline:
        last = execute(session, script)
        if last:
            return last
        time.sleep(0.25)
    raise AssertionError(f"Timed out waiting for {description}; last={last!r}")


def text(session: str, selector: str) -> str:
    return execute(session, f"return document.querySelector({json.dumps(selector)})?.textContent?.trim() ?? '';" )


def set_value(session: str, selector: str, value: str) -> None:
    ok = execute(
        session,
        f"""
        const element=document.querySelector({json.dumps(selector)});
        if(!element) return false;
        element.value={json.dumps(value)};
        element.dispatchEvent(new Event('input', {{bubbles:true}}));
        element.dispatchEvent(new Event('change', {{bubbles:true}}));
        return true;
        """,
    )
    if not ok:
        raise AssertionError(f"Missing input {selector}")


def click(session: str, selector: str) -> None:
    ok = execute(
        session,
        f"const element=document.querySelector({json.dumps(selector)}); if(!element) return false; element.click(); return true;",
    )
    if not ok:
        raise AssertionError(f"Missing clickable element {selector}")


def new_session() -> str:
    created = webdriver(
        "POST",
        "/session",
        {
            "capabilities": {
                "alwaysMatch": {
                    "browserName": "chrome",
                    "goog:chromeOptions": {
                        "args": [
                            "--headless=new",
                            "--no-sandbox",
                            "--disable-dev-shm-usage",
                            "--enable-webgl",
                            "--ignore-gpu-blocklist",
                            "--window-size=1440,1000",
                        ]
                    },
                }
            }
        },
    )
    return created["sessionId"]


def make_bundle(request: dict, metadata: dict, example_path: str) -> dict:
    bundle = load_json(example_path)
    origin = request["origin"]
    destination = request["destination"]
    origin_name = metadata.get("originStopName") or "Aachen origin"
    destination_name = metadata.get("destinationStopName") or "Aachen destination"
    bundle["researchedAt"] = "2026-08-17T20:00:00Z"
    bundle["question"]["questionRef"] = "aachen-journey-smoke"
    bundle["question"]["text"] = "Deterministic Aachen public-transit Journey acceptance destination."
    bundle["question"]["area"]["center"] = {
        "label": origin_name,
        "coordinate": origin,
    }
    bundle["question"]["area"]["radiusMeters"] = 25000
    candidate = bundle["candidates"][0]
    candidate["candidateRef"] = "journey-destination"
    candidate["displayName"] = "Journey Destination"
    candidate["identity"]["canonicalUri"] = "https://example.org/orientation/journey-destination"
    candidate["researchedLocation"] = {
        "label": destination_name,
        "coordinate": destination,
        "sourceRefs": ["source-official"],
    }
    return bundle


def local_datetime_value(offset_timestamp: str) -> str:
    parsed = datetime.fromisoformat(offset_timestamp)
    return parsed.strftime("%Y-%m-%dT%H:%M")


def open_app(session: str) -> None:
    webdriver("POST", f"/session/{session}/window/rect", {"width": 1440, "height": 1000})
    webdriver("POST", f"/session/{session}/url", {"url": APP_URL})
    wait_for(session, "standalone Orientation app", "return Boolean(document.querySelector('.orientation-app'));", timeout=15)
    wait_for(session, "MapLibre canvas", "return Boolean(document.querySelector('#app-map .maplibregl-canvas'));", timeout=30)
    if execute(session, "return !document.querySelector('#app-map-status').hidden;"):
        raise AssertionError(f"Map reported error: {text(session, '#app-map-status')}")


def select_origin_and_destination(session: str, bundle: dict) -> None:
    set_value(session, "#bundle-json", json.dumps(bundle))
    click(session, "#import-bundle")
    wait_for(session, "Journey discovery import", "return document.querySelector('#import-status')?.textContent?.includes('Collection imported');")
    wait_for(session, "Journey candidate list", "return document.querySelectorAll('#candidate-list .candidate-button').length === 1;")
    click(session, "#candidate-list .candidate-button")
    wait_for(session, "Journey destination selection", "return document.querySelector('#selected-destination')?.textContent?.startsWith('Journey Destination');")

    set_value(session, "#route-origin-query", "Journey Start")
    click(session, "#route-origin-search")
    wait_for(session, "Journey origin result", "return document.querySelectorAll('#route-origin-results .place-result').length === 1;")
    click(session, "#route-origin-results .place-result")
    wait_for(session, "Journey origin selection", "return document.querySelector('#route-origin-status')?.textContent?.includes('Selected: Journey Start');")


def request_and_show_journey(session: str, request: dict) -> int:
    set_value(session, "#route-profile", "TRANSIT")
    if execute(session, "return document.querySelector('#journey-time-controls').hidden;"):
        raise AssertionError("Public-transit time controls remained hidden")
    set_value(session, "#journey-time-mode", request["timeMode"])
    set_value(session, "#journey-time", local_datetime_value(request["time"]))
    if execute(session, "return document.querySelector('#request-route').disabled;"):
        raise AssertionError("Journey request button remained disabled with valid origin, destination and time")

    click(session, "#request-route")
    count = wait_for(
        session,
        "public-transit Journey alternatives",
        "return document.querySelectorAll('#journey-results .journey-alternative').length;",
        timeout=40,
    )
    status = text(session, "#route-status")
    if "public-transit journey" not in status:
        raise AssertionError(f"Unexpected Journey status: {status}")
    first = text(session, "#journey-results .journey-alternative")
    if "leg" not in first or "scheduled" not in first:
        raise AssertionError(f"Journey alternative does not expose ordered timing/leg information: {first}")

    click(session, "#journey-results .journey-alternative button")
    wait_for(session, "selected Journey map state", "return document.querySelector('#journey-results .journey-alternative')?.getAttribute('aria-current') === 'true';")
    wait_for(session, "Journey map selection status", "return document.querySelector('#route-status')?.textContent === 'Journey 1 shown on the map.';")
    if not execute(session, "return document.querySelector('#route-summary').hidden;"):
        raise AssertionError("Direct Route summary leaked into Journey presentation")
    return int(count)


def run_browser(request_path: str, metadata_path: str, example_path: str) -> None:
    request = load_json(request_path)
    metadata = load_json(metadata_path)
    bundle = make_bundle(request, metadata, example_path)
    session = new_session()
    print("Chrome session", session, "standalone Journey acceptance")
    try:
        open_app(session)
        select_origin_and_destination(session, bundle)
        alternatives = request_and_show_journey(session, request)

        if alternatives > 1:
            click(session, "#journey-results .journey-alternative:nth-child(2) button")
            wait_for(session, "replacement Journey selection", "return document.querySelector('#journey-results .journey-alternative:nth-child(2)')?.getAttribute('aria-current') === 'true';")

        set_value(session, "#route-profile", "DRIVING")
        wait_for(session, "Journey clear on direct-mode switch", "return document.querySelector('#journey-results').hidden;")
        if not execute(session, "return document.querySelector('#journey-time-controls').hidden;"):
            raise AssertionError("Transit time controls remained visible after switching to direct routing")
        if execute(session, "return document.querySelectorAll('#candidate-list .candidate-button').length;") != 1:
            raise AssertionError("Switching navigation modes corrupted discovery state")

        set_value(session, "#route-profile", "TRANSIT")
        request_and_show_journey(session, request)
        click(session, "#clear-route")
        wait_for(session, "Journey clear", "return document.querySelector('#route-status')?.textContent === 'No navigation requested.';")
        if not execute(session, "return document.querySelector('#journey-results').hidden;"):
            raise AssertionError("Journey alternatives remained visible after clear")
        if execute(session, "return document.querySelectorAll('#candidate-list .candidate-button').length;") != 1:
            raise AssertionError("Clearing Journey corrupted discovery state")

        print(f"PASS: standalone app planned and rendered {alternatives} real public-transit Journey alternative(s)")
    finally:
        try:
            webdriver("DELETE", f"/session/{session}")
        except Exception as error:
            print("Warning: failed to close Chrome session:", error)


def main() -> None:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    photon = subparsers.add_parser("photon")
    photon.add_argument("request")
    run = subparsers.add_parser("run")
    run.add_argument("request")
    run.add_argument("metadata")
    run.add_argument("example")
    args = parser.parse_args()
    if args.command == "photon":
        serve_photon(args.request)
    else:
        run_browser(args.request, args.metadata, args.example)


if __name__ == "__main__":
    main()
