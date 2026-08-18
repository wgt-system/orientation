#!/usr/bin/env python3
import argparse
import json
import time
import urllib.error
import urllib.request
from pathlib import Path

WEBDRIVER = "http://127.0.0.1:9515"
APP_URL = "http://127.0.0.1:4173/app.html"


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


def wait_for(session: str, description: str, script: str, timeout: float = 25.0):
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
                            "--window-size=1440,900",
                        ]
                    },
                }
            }
        },
    )
    return created["sessionId"]


def open_app(session: str) -> None:
    webdriver("POST", f"/session/{session}/window/rect", {"width": 1440, "height": 900})
    webdriver("POST", f"/session/{session}/url", {"url": APP_URL})
    wait_for(session, "standalone app", "return Boolean(document.querySelector('.orientation-app'));", timeout=15)
    wait_for(session, "MapLibre canvas", "return Boolean(document.querySelector('#app-map .maplibregl-canvas'));", timeout=30)
    if execute(session, "return !document.querySelector('#app-map-status').hidden;"):
        raise AssertionError(f"Map reported error: {text(session, '#app-map-status')}")


def import_phase(session: str) -> None:
    open_app(session)
    wait_for(session, "empty collection state", "return document.querySelector('#collections-status')?.textContent?.includes('No saved discoveries yet.');")

    set_value(session, "#research-question", "Find suitable restaurants near Hamburg Hauptbahnhof.")
    set_value(session, "#research-center", "Hamburg Hauptbahnhof")
    set_value(session, "#research-radius", "5")
    set_value(session, ".criterion-description", "A current source documents vegetarian options.")
    execute(
        session,
        "const select=document.querySelector('.criterion-mode'); select.value='EVIDENCE_REQUIRED'; select.dispatchEvent(new Event('change',{bubbles:true})); return true;",
    )
    execute(session, "document.querySelector('#research-form').requestSubmit(); return true;")
    wait_for(session, "generated research prompt", "return document.querySelector('#prompt-status')?.textContent?.startsWith('Prompt ready');")
    prompt = execute(session, "return document.querySelector('#prompt-text')?.value ?? '';")
    if "orientation.spatial-research-bundle" not in prompt or "EVIDENCE_REQUIRED" not in prompt:
        raise AssertionError("Generated prompt is not bound to Spatial Research Bundle 1.0")

    valid_bundle = json.loads(Path("contracts/examples/spatial-research-v1.valid.json").read_text(encoding="utf-8"))
    invalid_bundle = dict(valid_bundle)
    invalid_bundle["version"] = "9.9"
    set_value(session, "#bundle-json", json.dumps(invalid_bundle))
    click(session, "#import-bundle")
    wait_for(session, "rejected incompatible import", "return document.querySelector('#import-status')?.textContent?.includes('Bundle rejected');")
    if execute(session, "return document.querySelectorAll('#collections-list .collection-button').length;") != 0:
        raise AssertionError("Rejected bundle changed persistent collection state")

    set_value(session, "#bundle-json", json.dumps(valid_bundle))
    click(session, "#import-bundle")
    wait_for(session, "successful import", "return document.querySelector('#import-status')?.textContent?.includes('Collection imported');")
    wait_for(session, "persisted collection", "return document.querySelectorAll('#collections-list .collection-button').length === 1;")
    wait_for(session, "opened candidate", "return document.querySelectorAll('#candidate-list .candidate-button').length === 1;")
    collection_id = execute(session, "return document.querySelector('#collections-list .collection-button')?.dataset.collectionId ?? '';" )
    if not collection_id:
        raise AssertionError("Imported collection did not expose its Orientation collection id")
    Path("/tmp/orientation-standalone-collection-id").write_text(collection_id, encoding="utf-8")
    print("Imported collection", collection_id)


def reopen_phase(session: str) -> None:
    expected_id = Path("/tmp/orientation-standalone-collection-id").read_text(encoding="utf-8").strip()
    open_app(session)
    wait_for(session, "persisted collection after backend restart", "return document.querySelectorAll('#collections-list .collection-button').length === 1;")
    actual_id = execute(session, "return document.querySelector('#collections-list .collection-button')?.dataset.collectionId ?? '';" )
    if actual_id != expected_id:
        raise AssertionError(f"Reopened collection id changed across restart: {actual_id} != {expected_id}")

    click(session, "#collections-list .collection-button")
    wait_for(session, "reopened candidate list", "return document.querySelectorAll('#candidate-list .candidate-button').length === 1;")
    click(session, "#candidate-list .candidate-button")
    wait_for(session, "candidate evidence detail", "return document.querySelector('#candidate-detail-title')?.textContent === 'Example Garden Restaurant';")
    if not execute(session, "return document.querySelector('#candidate-detail a[href=" + json.dumps("https://example.org/restaurant") + "]') !== null;"):
        raise AssertionError("Candidate provenance link was not rendered")
    if not text(session, "#selected-destination").startswith("Destination: Example Garden Restaurant"):
        raise AssertionError("Selected candidate did not become route destination")

    set_value(session, "#route-origin-query", "Smoke Start")
    click(session, "#route-origin-search")
    wait_for(session, "origin place result", "return document.querySelectorAll('#route-origin-results .place-result').length === 1;")
    click(session, "#route-origin-results .place-result")
    wait_for(session, "origin selection", "return document.querySelector('#route-origin-status')?.textContent?.includes('Selected: Smoke Start');")
    if execute(session, "return document.querySelector('#request-route').disabled;"):
        raise AssertionError("Route button remained disabled with selected start and mapped destination")

    click(session, "#request-route")
    wait_for(session, "real standalone route", "return document.querySelector('#route-status')?.textContent === 'Route ready.';", timeout=30)
    summary = text(session, "#route-summary")
    if "Smoke Start" not in summary or "Example Garden Restaurant" not in summary or "Driving" not in summary:
        raise AssertionError(f"Unexpected standalone route summary: {summary}")

    click(session, "#clear-route")
    wait_for(session, "route clear", "return document.querySelector('#route-status')?.textContent === 'No navigation requested.';")
    if execute(session, "return document.querySelectorAll('#candidate-list .candidate-button').length;") != 1:
        raise AssertionError("Clearing route corrupted discovery state")
    print("Reopened and routed collection", expected_id)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("phase", choices=("import", "reopen"))
    args = parser.parse_args()
    session = new_session()
    print("Chrome session", session, "phase", args.phase)
    try:
        if args.phase == "import":
            import_phase(session)
        else:
            reopen_phase(session)
    finally:
        try:
            webdriver("DELETE", f"/session/{session}")
        except Exception as error:
            print("Warning: failed to close Chrome session:", error)


if __name__ == "__main__":
    main()
