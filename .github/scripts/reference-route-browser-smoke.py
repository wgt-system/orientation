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


def decode_png_rgb(png: bytes) -> tuple[int, int, list[bytes]]:
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
    rows: list[bytes] = []
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

        if channels == 3:
            rows.append(bytes(row))
        else:
            rgb = bytearray(width * 3)
            for x in range(width):
                source = x * 4
                target = x * 3
                rgb[target : target + 3] = row[source : source + 3]
            rows.append(bytes(rgb))
        previous = row

    return width, height, rows


def route_pixel_count(png: bytes, min_x: int, tolerance: int = 20) -> int:
    width, _height, rows = decode_png_rgb(png)
    scan_start = max(0, min(width - 1, min_x))
    count = 0
    for row in rows:
        for x in range(scan_start, width):
            index = x * 3
            rgb = row[index], row[index + 1], row[index + 2]
            if all(abs(rgb[channel] - ROUTE_COLOR[channel]) <= tolerance for channel in range(3)):
                count += 1
    return count


def map_pixel_difference(first: bytes, second: bytes, min_x: int, channel_threshold: int = 12) -> int:
    first_width, first_height, first_rows = decode_png_rgb(first)
    second_width, second_height, second_rows = decode_png_rgb(second)
    if (first_width, first_height) != (second_width, second_height):
        raise AssertionError(
            f"Screenshot dimensions changed unexpectedly: {(first_width, first_height)} != {(second_width, second_height)}"
        )
    scan_start = max(0, min(first_width - 1, min_x))
    changed = 0
    for first_row, second_row in zip(first_rows, second_rows, strict=True):
        for x in range(scan_start, first_width):
            index = x * 3
            if max(
                abs(first_row[index + channel] - second_row[index + channel])
                for channel in range(3)
            ) >= channel_threshold:
                changed += 1
    return changed


def assert_visible_change(route_png: bytes, clear_png: bytes, min_x: int, stability_pixels: int, label: str) -> int:
    changed = map_pixel_difference(route_png, clear_png, min_x)
    required = max(250, stability_pixels * 3 + 100)
    if changed < required:
        route_colors = route_pixel_count(route_png, min_x)
        clear_colors = route_pixel_count(clear_png, min_x)
        raise AssertionError(
            f"{label} did not visibly change the map enough: changed={changed} required={required} "
            f"stability={stability_pixels} routeColorPixels={route_colors} clearColorPixels={clear_colors}"
        )
    return changed


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

        execute(
            session,
            "document.querySelector('#place-query').value='Smoke Place'; document.querySelector('#place-search').requestSubmit(); return true;",
        )
        wait_for(session, "general place result", "return document.querySelectorAll('#place-results .place-result').length === 1;")
        select_first_result(session, "#place-results")
        wait_for(session, "general place selection", "return document.querySelector('#selection')?.textContent?.includes('Smoke Place');")

        execute(session, "document.querySelector('#identify-center').click(); return true;")
        wait_for(session, "reverse lookup", "return document.querySelector('#reverse-status')?.textContent === 'Map center identified.';")
        wait_for(session, "reverse selection", "return document.querySelector('#selection')?.textContent?.includes('Smoke Reverse');")
        execute(session, "document.querySelector('#set-location').click(); return true;")
        wait_for(session, "current position", "return document.querySelector('#location-status')?.textContent?.includes('Sample position');")

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

        # Real Valhalla-backed driving route.
        execute(session, "document.querySelector('#request-route').click(); return true;")
        wait_for(session, "driving route", "return document.querySelector('#route-status')?.textContent === 'Route ready.';", timeout=30)
        summary = text(session, "#route-summary")
        if not all(value in summary for value in ("Smoke Start", "Smoke Destination", "Driving")):
            raise AssertionError(f"Unexpected driving route summary: {summary}")

        # Let route fit + basemap tiles settle, then measure normal screenshot drift before clearing.
        time.sleep(3)
        driving_a = screenshot(session, "/tmp/reference-driving-route-a.png")
        time.sleep(0.5)
        driving_b = screenshot(session, "/tmp/reference-driving-route-b.png")
        driving_stability = map_pixel_difference(driving_a, driving_b, map_left)

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
        time.sleep(0.35)
        cleared = screenshot(session, "/tmp/reference-cleared-route.png")
        driving_clear_diff = assert_visible_change(
            driving_b,
            cleared,
            map_left,
            driving_stability,
            "Driving route clear",
        )

        # Request a replacement route through the same real provider path.
        execute(session, "document.querySelector('#request-route').click(); return true;")
        wait_for(session, "cycling route", "return document.querySelector('#route-status')?.textContent === 'Route ready.';", timeout=30)
        cycling_summary = text(session, "#route-summary")
        if "Cycling" not in cycling_summary:
            raise AssertionError(f"Replacement route summary did not reflect Cycling: {cycling_summary}")
        time.sleep(3)
        cycling_a = screenshot(session, "/tmp/reference-cycling-route-a.png")
        time.sleep(0.5)
        cycling_b = screenshot(session, "/tmp/reference-cycling-route-b.png")
        cycling_stability = map_pixel_difference(cycling_a, cycling_b, map_left)

        # Explicit clear removes the replacement route while preserving the fitted viewport.
        execute(session, "document.querySelector('#clear-route').click(); return true;")
        wait_for(session, "explicit route clear", "return document.querySelector('#route-status')?.textContent === 'No route requested.';")
        time.sleep(0.35)
        final = screenshot(session, "/tmp/reference-final-clear.png")
        cycling_clear_diff = assert_visible_change(
            cycling_b,
            final,
            map_left,
            cycling_stability,
            "Cycling route clear",
        )

        desktop_scroll = execute(
            session,
            "const p=document.querySelector('.panel'); const s=getComputedStyle(p); return p.scrollHeight>p.clientHeight && ['auto','scroll'].includes(s.overflowY);",
        )
        if not desktop_scroll:
            raise AssertionError("Reference panel is not scrollable at constrained desktop height")

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
                    "drivingSummary": summary,
                    "cyclingSummary": cycling_summary,
                    "driving": {
                        "stabilityPixels": driving_stability,
                        "clearDifferencePixels": driving_clear_diff,
                        "routeColorPixels": route_pixel_count(driving_b, map_left),
                    },
                    "cycling": {
                        "stabilityPixels": cycling_stability,
                        "clearDifferencePixels": cycling_clear_diff,
                        "routeColorPixels": route_pixel_count(cycling_b, map_left),
                    },
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
