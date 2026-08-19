#!/usr/bin/env python3
import argparse
import json
import time
import urllib.error
import urllib.request
from datetime import datetime

WEBDRIVER = "http://127.0.0.1:9515"
APP_URL = "http://127.0.0.1:4174/app.html"


def load_json(path: str) -> dict:
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


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


def local_datetime_value(offset_timestamp: str) -> str:
    return datetime.fromisoformat(offset_timestamp).strftime("%Y-%m-%dT%H:%M")


def verify_workspace_layout(session: str) -> None:
    desktop = execute(
        session,
        """
        const research=document.querySelector('#research-panel');
        const navigate=document.querySelector('#navigate-card');
        const map=document.querySelector('#app-map');
        const nav=document.querySelector('.app-jump-nav');
        if(!research || !navigate || !map || !nav) return null;
        return {
          researchOverflow:getComputedStyle(research).overflowY,
          researchScrollable:research.scrollHeight > research.clientHeight,
          navigateBeforeMap:navigate.getBoundingClientRect().top < map.getBoundingClientRect().top,
          jumpLinks:nav.querySelectorAll('a').length
        };
        """,
    )
    if not desktop or desktop["researchOverflow"] != "auto" or not desktop["researchScrollable"]:
        raise AssertionError(f"Desktop Research column is not independently scrollable: {desktop}")
    if not desktop["navigateBeforeMap"] or desktop["jumpLinks"] != 3:
        raise AssertionError(f"Desktop Navigate discoverability regression: {desktop}")

    webdriver("POST", f"/session/{session}/window/rect", {"width": 390, "height": 844})
    mobile = wait_for(
        session,
        "mobile document-scroll layout",
        """
        const research=document.querySelector('#research-panel');
        const collections=document.querySelector('#collections-panel');
        const navigate=document.querySelector('#navigate-card');
        const nav=document.querySelector('.app-jump-nav');
        if(!research || !collections || !navigate || !nav) return null;
        const bodyOverflow=getComputedStyle(document.body).overflowY;
        const researchOverflow=getComputedStyle(research).overflowY;
        const navigateTop=navigate.getBoundingClientRect().top;
        const navigationFirst=navigateTop < research.getBoundingClientRect().top && navigateTop < collections.getBoundingClientRect().top;
        const firstJump=nav.querySelector('a')?.getAttribute('href');
        return (document.documentElement.scrollHeight > window.innerHeight && researchOverflow !== 'auto' && navigationFirst && firstJump === '#navigate-card')
          ? {bodyOverflow, researchOverflow, scrollHeight:document.documentElement.scrollHeight, navigationFirst, firstJump}
          : null;
        """,
        timeout=10,
    )
    click(session, '.app-jump-nav a[href="#navigate-card"]')
    wait_for(
        session,
        "mobile Navigate jump",
        "return location.hash === '#navigate-card' && document.querySelector('#navigate-card').getBoundingClientRect().top < window.innerHeight;",
        timeout=5,
    )
    print("PASS: standalone layout desktop scroll + mobile document navigation", desktop, mobile)
    webdriver("POST", f"/session/{session}/window/rect", {"width": 1440, "height": 1000})


def open_app(session: str) -> None:
    webdriver("POST", f"/session/{session}/window/rect", {"width": 1440, "height": 1000})
    webdriver("POST", f"/session/{session}/url", {"url": APP_URL})
    wait_for(session, "standalone Orientation app", "return Boolean(document.querySelector('.orientation-app'));", timeout=15)
    wait_for(session, "MapLibre canvas", "return Boolean(document.querySelector('#app-map .maplibregl-canvas'));", timeout=30)
    if execute(session, "return !document.querySelector('#app-map-status').hidden;"):
        raise AssertionError(f"Map reported error: {text(session, '#app-map-status')}")
    verify_workspace_layout(session)


def search_and_select(session: str, query_selector: str, button_selector: str, results_selector: str, status_selector: str, query: str) -> None:
    set_value(session, query_selector, query)
    click(session, button_selector)
    wait_for(session, f"place results for {query}", f"return document.querySelectorAll({json.dumps(results_selector + ' .place-result')}).length >= 1;", timeout=30)
    click(session, results_selector + " .place-result")
    wait_for(session, f"place selection for {query}", f"return document.querySelector({json.dumps(status_selector)})?.textContent?.startsWith('Selected');")


def select_origin_and_destination(session: str, request: dict, metadata: dict) -> None:
    origin = request["origin"]
    execute(
        session,
        f"""
        Object.defineProperty(navigator, 'geolocation', {{
          configurable: true,
          value: {{
            getCurrentPosition(success) {{
              success({{
                coords: {{
                  longitude: {origin['longitude']},
                  latitude: {origin['latitude']},
                  accuracy: 12.4
                }},
                timestamp: Date.now()
              }});
            }}
          }}
        }});
        return true;
        """,
    )
    click(session, "#route-use-current-location")
    wait_for(
        session,
        "current-location origin selection",
        "return document.querySelector('#route-origin-status')?.textContent?.startsWith('Selected: Current location');",
    )
    origin_status = text(session, "#route-origin-status")
    if "±12 m" not in origin_status:
        raise AssertionError(f"Current-location accuracy was not shown: {origin_status}")

    destination_name = metadata.get("destinationStopName") or "Aachen"
    search_and_select(
        session,
        "#route-destination-query",
        "#route-destination-search",
        "#route-destination-results",
        "#route-destination-status",
        destination_name,
    )
    selected = text(session, "#selected-destination")
    if not selected.startswith("Destination:"):
        raise AssertionError(f"Destination summary is not visible after direct search: {selected}")
    if execute(session, "return document.querySelectorAll('#candidate-list .candidate-button').length;") != 0:
        raise AssertionError("Quick Journey acceptance unexpectedly depends on a Discovery Candidate")


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


def run_browser(request_path: str, metadata_path: str, _example_path: str) -> None:
    request = load_json(request_path)
    metadata = load_json(metadata_path)
    session = new_session()
    print("Chrome session", session, "standalone ad-hoc Journey acceptance")
    try:
        open_app(session)
        select_origin_and_destination(session, request, metadata)
        alternatives = request_and_show_journey(session, request)

        if alternatives > 1:
            click(session, "#journey-results .journey-alternative:nth-child(2) button")
            wait_for(session, "replacement Journey selection", "return document.querySelector('#journey-results .journey-alternative:nth-child(2)')?.getAttribute('aria-current') === 'true';")

        set_value(session, "#route-profile", "DRIVING")
        wait_for(session, "Journey clear on direct-mode switch", "return document.querySelector('#journey-results').hidden;")
        if not execute(session, "return document.querySelector('#journey-time-controls').hidden;"):
            raise AssertionError("Transit time controls remained visible after switching to direct routing")

        set_value(session, "#route-profile", "TRANSIT")
        request_and_show_journey(session, request)
        click(session, "#clear-route")
        wait_for(session, "Journey clear", "return document.querySelector('#route-status')?.textContent === 'No navigation requested.';")
        if not execute(session, "return document.querySelector('#journey-results').hidden;"):
            raise AssertionError("Journey alternatives remained visible after clear")
        if not text(session, "#route-origin-status").startswith("Selected:") or not text(session, "#route-destination-status").startswith("Selected:"):
            raise AssertionError("Clearing navigation unexpectedly cleared selected endpoints")

        print(f"PASS: standalone app planned and rendered {alternatives} real current-location-to-place public-transit Journey alternative(s) without research import")
    finally:
        try:
            webdriver("DELETE", f"/session/{session}")
        except Exception as error:
            print("Warning: failed to close Chrome session:", error)


def main() -> None:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    run = subparsers.add_parser("run")
    run.add_argument("request")
    run.add_argument("metadata")
    run.add_argument("example")
    args = parser.parse_args()
    run_browser(args.request, args.metadata, args.example)


if __name__ == "__main__":
    main()
