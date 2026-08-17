#!/usr/bin/env python3
import math
import struct
import sys
import zlib
from collections import Counter
from pathlib import Path

TARGET = (196, 81, 58)


def pixels(path: Path):
    png = path.read_bytes()
    if not png.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError(f"{path}: not a PNG")
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
        raise ValueError(
            f"{path}: unsupported PNG width={width} height={height} depth={bit_depth} type={color_type} interlace={interlace}"
        )
    channels = 3 if color_type == 2 else 4
    stride = width * channels
    raw = zlib.decompress(bytes(compressed))
    previous = bytearray(stride)
    position = 0
    result = []

    def paeth(a, b, c):
        p = a + b - c
        pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
        if pa <= pb and pa <= pc:
            return a
        return b if pb <= pc else c

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
                raise ValueError(f"{path}: unsupported PNG filter {filter_type}")
        for x in range(width):
            i = x * channels
            result.append((row[i], row[i + 1], row[i + 2]))
        previous = row
    return width, height, result


def main():
    for name in sys.argv[1:]:
        path = Path(name)
        if not path.exists():
            continue
        width, height, colors = pixels(path)
        nearest = min(colors, key=lambda rgb: sum((rgb[i] - TARGET[i]) ** 2 for i in range(3)))
        distance = math.sqrt(sum((nearest[i] - TARGET[i]) ** 2 for i in range(3)))
        counts = {
            tolerance: sum(
                1
                for rgb in colors
                if all(abs(rgb[i] - TARGET[i]) <= tolerance for i in range(3))
            )
            for tolerance in (10, 20, 30, 50, 80)
        }
        common = Counter(colors).most_common(12)
        print(
            path,
            "size=", f"{width}x{height}",
            "target=", TARGET,
            "nearest=", nearest,
            "distance=", round(distance, 2),
            "counts=", counts,
            "common=", common,
            flush=True,
        )


if __name__ == "__main__":
    main()
