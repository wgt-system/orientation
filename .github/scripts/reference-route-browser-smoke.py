#!/usr/bin/env python3
import base64
import json
import struct
import time
import urllib.error
import urllib.request
import zlib
from pathlib import Path

WEBDRIVER = "http://127.0.0.1:9515"
ELEMENT_KEY = "element-6066-11e4-a52e-4f735466cecf"
ROUTE_COLOR = (196, 81, 58)


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


def wait_for(session: str, description: str, script: str, timeout: float = 20.0):
    deadline = time.time() + timeout
    last = None
    while time.time() < deadline:
        last = execute(session, script)
        if last:
            return last
        time.sleep(0.25)
    raise AssertionError(f"Timed out waiting for {description}; last={last!r}")


def screenshot(session: str, path: str) -> bytes:
    encoded = webdriver("GET", f"/session/{session}/screenshot")
    data = base64.b64decode(encoded)
    Path(path).write_bytes(data)
    return data


def route_pixel_count(png: bytes, min_x: int) -> int:
    if not png.startswith(b"\x89PNG\r\n\x1a\n"):
        raise AssertionError("WebDriver screenshot is not PNG")

    offset = 8
    width = height = bit_depth = color_type = interlace = None
    compressed = bytearray()
    while offset < len(png):
        length = struct.unpack(">I", png[offset : offset + 4])[0]
        kind = png[offset + 4 : offset + 8]
        payload = png[offset + 8 : offset + 8 + length]
        offset += 12 + length
        if kind == b"IHDR":
            width, height, bit_depth, color_type, _, _, interlace = struct.unpack(">IIBBBBB", payload)
        elif kind == b"IDAT":
            compressed.extend(payload)
        elif kind == b"IEND":
            break

    if width is None or height is None or bit_depth != 8 or color_type not in (2, 6) or interlace != 0:
        raise AssertionError(
            f"Unsupported screenshot PNG: width={width} height={height} depth={bit_depth} type={color_type} interlace={interlace}"
        )

    channels = 3 if color_type == 2 else 4
    stride = width * channels
    raw = zlib.decompress(bytes(compressed))
    expected = height * (stride + 1)
    if len(raw) != expected:
        raise AssertionError(f"Unexpected PNG payload length: {len(raw)} != {expected}")

    def paeth(a: int, b: int, c: int) -> int:
        p = a + b - c
        pa = abs(p - a)
        pb = abs(p - b)
        pc = abs(p - c)
        if pa <= pb and pa <= pc:
            return a
        if pb <= pc:
            return b
        return c

    previous = bytearray(stride)
    position = 0
    count = 0
    tolerance = 10
    scan_start = max(0, min(width - 1, min_x))

    for _ in range(height):
        filter_type = raw[position]
        position += 1
        row = bytearray(raw[position : position + stride])
        position += stride

        for index in range(stride):
            left = row[index - channels] if index >= channels else 0
            up = previous[index]
            up_left = previous[index - channels] if index >= channels else 0
            if filter_type == 1:
                row[index] = (row[index] + left) & 0xFF
            elif filter_type == 2:
                row[index] = (row[index] + up) & 0xFF
            elif filter_type == 3:
                row[index] = (row[index] + ((left + up) // 2)) & 0xFF
            elif filter_type == 4:
                row[index] = (row[index] + paeth(left, up, up_left)) & 0xFF
            elif filter_type != 0:
                raise AssertionError(f"Unsupported PNG filter {filter_type}")

        for x in range(scan_start, width):
            index = x * channels
            rgb = row[index], row[index + 1], row[index + 2]
            if all(abs(rgb[channel] - ROUTE_COLOR[channel]) <= tolerance for channel in range(3)):
                count += 1
        previous = row

    return count


def text(session: str, selector: str) -> str:
    return execute(
        session,
        f"return document.querySelector({json.dumps(selector)})?.textContent?.trim() ?? '';",
    )


def set_value_and_click(session: str, input_selector: str, value: str, button_selector: str) -> None:
    execute(
        session,
        f"""
        const input = document.querySelector({json.dumps(input_selector)});
        const button = document.querySelector({json.dumps(button_selector)});
        if (!input || !button) return false;
        input.value = {json.dumps(value)};
        input.dispatchEvent(new Event('input', {{ bubbles: true }}));
        button.click();
        return true;
        """,
    )


def select_first_result(session: str, list_selector: str) -> None:
    result = execute(
        session,
        f"const button=document.querySelector({json.dumps(list_selector + ' .place-result')}); if(!button) return false; button.click(); return true;",
    )
    if not result:
        raise AssertionError(f"No selectable result in {list_selector}")


def main() -> None:
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
                            "--window-size=1440,800",
                        ]
                    },
                }
            }
        },
    )
    session = created["sessionId"]
    print("Chrome session", session)

    try:
        webdriver("POST", f"/session/{session}/window/rect", {"width": 1440, "height": 800})
        webdriver("POST", f"/session/{session}/url", {"url": "http://127.0.0.1:4173/"})

        wait_for(
            session,
            "MapLibre canvas",
            "return Boolean(document.querySelector('#map .maplibregl-canvas'));",
            timeout=30,
        )
        time.sleep(4)
        if execute(session, "return !document.querySelector('#map-status').hidden;"):
            raise AssertionError(f"Map reported error: {text(session, '#map-status')}")

        # Existing Reference Host place search still works.
        execute(
            session,
            "document.querySelector('#place-query').value='Smoke Place'; document.querySelector('#place-search').requestSubmit(); return true;",
        )
        wait_for(session, "general place result", "return document.querySelectorAll('#place-results .place-result').length === 1;")
        select_first_result(session, "#place-results")
        wait_for(session, "general place selection", "return document.querySelector('#selection')?.textContent?.includes('Smoke Place');")

        # Reverse lookup and host-supplied current position regressions remain alive.
        execute(session, "document.querySelector('#identify-center').click(); return true;")
        wait_for(session, "reverse lookup", "return document.querySelector('#reverse-status')?.textContent === 'Map center identified.';")
        wait_for(session, "reverse selection", "return document.querySelector('#selection')?.textContent?.includes('Smoke Reverse');")
        execute(session, "document.querySelector('#set-location').click(); return true;")
        wait_for(session, "current position", "return document.querySelector('#location-status')?.textContent?.includes('Sample position');")

        # Explicit route endpoint searches and selections.
        set_value_and_click(session, "#route-origin-query", "Smoke Start", "#route-origin-search")
        wait_for(session, "origin result", "return document.querySelectorAll('#route-origin-results .place-result').length === 1;")
        select_first_result(session, "#route-origin-results")
        wait_for(session, "origin selection", "return document.querySelector('#route-origin-status')?.textContent?.includes('Selected: Smoke Start');")

        set_value_and_click(
            session,
            "#route-destination-query",
            "Smoke Destination",
            "#route-destination-search",
        )
        wait_for(session, "destination result", "return document.querySelectorAll('#route-destination-results .place-result').length === 1;")
        select_first_result(session, "#route-destination-results")
        wait_for(session, "destination selection", "return document.querySelector('#route-destination-status')?.textContent?.includes('Selected: Smoke Destination');")
        if execute(session, "return document.querySelector('#request-route').disabled;"):
            raise AssertionError("Route button remained disabled after explicit endpoint selection")

        map_left = int(execute(session, "return Math.floor(document.querySelector('#map').getBoundingClientRect().left);"))
        before = route_pixel_count(screenshot(session, "/tmp/reference-before-route.png"), map_left)

        # Real Valhalla-backed driving route.
        execute(session, "document.querySelector('#request-route').click(); return true;")
        wait_for(session, "driving route", "return document.querySelector('#route-status')?.textContent === 'Route ready.';", timeout=30)
        summary = text(session, "#route-summary")
        if not all(value in summary for value in ("Smoke Start", "Smoke Destination", "Driving")):
            raise AssertionError(f"Unexpected driving route summary: {summary}")
        time.sleep(1)
        driving = route_pixel_count(screenshot(session, "/tmp/reference-driving-route.png"), map_left)
        if driving < before + 75:
            raise AssertionError(f"Route line was not visibly rendered: before={before} after={driving}")
        print("route pixels driving", driving, "baseline", before, "summary", summary)

        # Profile change must cancel/clear stale route rather than silently re-request.
        execute(
            session,
            "const select=document.querySelector('#route-profile'); select.value='CYCLING'; select.dispatchEvent(new Event('change',{bubbles:true})); return true;",
        )
        wait_for(
            session,
            "profile invalidation",
            "return document.querySelector('#route-status')?.textContent === 'Travel profile changed. Request the route again.';",
        )
        time.sleep(0.5)
        cleared = route_pixel_count(screenshot(session, "/tmp/reference-cleared-route.png"), map_left)
        if driving < cleared + 75:
            raise AssertionError(f"Profile change did not visibly clear the route: route={driving} cleared={cleared}")

        # Request a replacement route through the same real provider path.
        execute(session, "document.querySelector('#request-route').click(); return true;")
        wait_for(session, "cycling route", "return document.querySelector('#route-status')?.textContent === 'Route ready.';", timeout=30)
        cycling_summary = text(session, "#route-summary")
        if "Cycling" not in cycling_summary:
            raise AssertionError(f"Replacement route summary did not reflect Cycling: {cycling_summary}")
        time.sleep(1)
        cycling = route_pixel_count(screenshot(session, "/tmp/reference-cycling-route.png"), map_left)
        if cycling < cleared + 75:
            raise AssertionError(f"Replacement route line was not visibly rendered: cleared={cleared} cycling={cycling}")

        # Explicit clear removes the route again.
        execute(session, "document.querySelector('#clear-route').click(); return true;")
        wait_for(session, "explicit route clear", "return document.querySelector('#route-status')?.textContent === 'No route requested.';")
        time.sleep(0.5)
        final = route_pixel_count(screenshot(session, "/tmp/reference-final-clear.png"), map_left)
        if cycling < final + 75:
            raise AssertionError(f"Explicit clear did not visibly remove the route: cycling={cycling} final={final}")

        # Constrained desktop height must remain usable via panel scrolling.
        desktop_scroll = execute(
            session,
            "const p=document.querySelector('.panel'); const s=getComputedStyle(p); return p.scrollHeight>p.clientHeight && ['auto','scroll'].includes(s.overflowY);",
        )
        if not desktop_scroll:
            raise AssertionError("Reference panel is not scrollable at constrained desktop height")

        # Existing mobile layout keeps page scrolling rather than nesting the panel scroller.
        webdriver("POST", f"/session/{session}/window/rect", {"width": 700, "height": 900})
        mobile_flow = execute(
            session,
            "const p=getComputedStyle(document.querySelector('.panel')); const b=getComputedStyle(document.body); return p.overflowY === 'visible' && b.overflowY === 'auto';",
        )
        if not mobile_flow:
            raise AssertionError("Reference Host mobile scrolling flow is not preserved")

        print(
            "Reference route browser smoke PASS",
            json.dumps(
                {
                    "routePixels": {"before": before, "driving": driving, "cleared": cleared, "cycling": cycling, "final": final},
                    "drivingSummary": summary,
                    "cyclingSummary": cycling_summary,
                },
                sort_keys=True,
            ),
        )
    finally:
        try:
            webdriver("DELETE", f"/session/{session}")
        except Exception as error:
            print("Could not close Chrome session cleanly:", error)


if __name__ == "__main__":
    main()
