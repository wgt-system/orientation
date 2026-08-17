#!/usr/bin/env python3

import csv
import io
import json
import sys
import zipfile
from collections import defaultdict
from datetime import date, datetime, time, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo


def rows(archive: zipfile.ZipFile, name: str):
    try:
        raw = archive.read(name)
    except KeyError as exc:
        raise RuntimeError(f"GTFS fixture is missing {name}") from exc
    text = io.TextIOWrapper(io.BytesIO(raw), encoding="utf-8-sig", newline="")
    return list(csv.DictReader(text))


def parse_date(value: str) -> date:
    return datetime.strptime(value, "%Y%m%d").date()


def parse_gtfs_time(value: str) -> timedelta:
    parts = value.split(":")
    if len(parts) != 3:
        raise ValueError(f"Invalid GTFS time: {value}")
    hours, minutes, seconds = (int(part) for part in parts)
    if hours < 0 or minutes not in range(60) or seconds not in range(60):
        raise ValueError(f"Invalid GTFS time: {value}")
    return timedelta(hours=hours, minutes=minutes, seconds=seconds)


def active_date_by_service(archive: zipfile.ZipFile, service_ids: set[str]):
    exceptions: dict[str, dict[date, int]] = defaultdict(dict)
    try:
        calendar_dates = rows(archive, "calendar_dates.txt")
    except RuntimeError:
        calendar_dates = []
    for row in calendar_dates:
        service_id = row.get("service_id", "")
        if service_id not in service_ids:
            continue
        exceptions[service_id][parse_date(row["date"])] = int(row["exception_type"])

    result: dict[str, date] = {}
    try:
        calendar = rows(archive, "calendar.txt")
    except RuntimeError:
        calendar = []

    weekday_columns = [
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
    ]
    for row in calendar:
        service_id = row.get("service_id", "")
        if service_id not in service_ids:
            continue
        current = parse_date(row["start_date"])
        end = parse_date(row["end_date"])
        while current <= end:
            exception = exceptions.get(service_id, {}).get(current)
            regular = row.get(weekday_columns[current.weekday()], "0") == "1"
            if exception == 1 or (regular and exception != 2):
                result[service_id] = current
                break
            current += timedelta(days=1)

    for service_id, changes in exceptions.items():
        additions = sorted(day for day, kind in changes.items() if kind == 1)
        if additions and (service_id not in result or additions[0] < result[service_id]):
            result[service_id] = additions[0]

    return result


def prepare(gtfs_zip: str, request_path: str, metadata_path: str):
    with zipfile.ZipFile(gtfs_zip) as archive:
        agencies = rows(archive, "agency.txt")
        timezone_name = next((row.get("agency_timezone") for row in agencies if row.get("agency_timezone")), None)
        if not timezone_name:
            raise RuntimeError("GTFS fixture has no agency_timezone")
        timezone = ZoneInfo(timezone_name)

        trips = rows(archive, "trips.txt")
        trip_service = {row["trip_id"]: row["service_id"] for row in trips if row.get("trip_id") and row.get("service_id")}
        service_dates = active_date_by_service(archive, set(trip_service.values()))
        eligible_trip_ids = {trip_id for trip_id, service_id in trip_service.items() if service_id in service_dates}
        if not eligible_trip_ids:
            raise RuntimeError("GTFS fixture has no trip with an active service date")

        stop_times_by_trip: dict[str, list[dict[str, str]]] = defaultdict(list)
        for row in rows(archive, "stop_times.txt"):
            trip_id = row.get("trip_id", "")
            if trip_id in eligible_trip_ids:
                stop_times_by_trip[trip_id].append(row)

        stops = {
            row["stop_id"]: row
            for row in rows(archive, "stops.txt")
            if row.get("stop_id") and row.get("stop_lat") and row.get("stop_lon")
        }

        chosen = None
        for trip in trips:
            trip_id = trip.get("trip_id", "")
            service_id = trip_service.get(trip_id)
            if not service_id or service_id not in service_dates:
                continue
            stop_times = stop_times_by_trip.get(trip_id, [])
            stop_times.sort(key=lambda row: int(row.get("stop_sequence", "0")))
            usable = [row for row in stop_times if row.get("stop_id") in stops and (row.get("departure_time") or row.get("arrival_time"))]
            if len(usable) < 2:
                continue
            first = usable[0]
            last = usable[-1]
            first_time = first.get("departure_time") or first.get("arrival_time")
            last_time = last.get("arrival_time") or last.get("departure_time")
            if not first_time or not last_time:
                continue
            if parse_gtfs_time(last_time) <= parse_gtfs_time(first_time):
                continue
            chosen = (trip, service_id, first, last, first_time, last_time)
            break

        if chosen is None:
            raise RuntimeError("GTFS fixture has no usable active trip with two geocoded stops")

        trip, service_id, first, last, first_time, last_time = chosen
        service_date = service_dates[service_id]
        midnight = datetime.combine(service_date, time.min, tzinfo=timezone)
        departure = midnight + parse_gtfs_time(first_time)
        arrival = midnight + parse_gtfs_time(last_time)
        request_time = departure - timedelta(minutes=1)
        origin_stop = stops[first["stop_id"]]
        destination_stop = stops[last["stop_id"]]

        request = {
            "origin": {
                "longitude": float(origin_stop["stop_lon"]),
                "latitude": float(origin_stop["stop_lat"]),
            },
            "destination": {
                "longitude": float(destination_stop["stop_lon"]),
                "latitude": float(destination_stop["stop_lat"]),
            },
            "timeMode": "DEPART_AT",
            "time": request_time.isoformat(),
        }
        metadata = {
            "agencyTimezone": timezone_name,
            "serviceDate": service_date.isoformat(),
            "serviceId": service_id,
            "tripId": trip["trip_id"],
            "originStopId": first["stop_id"],
            "originStopName": origin_stop.get("stop_name"),
            "destinationStopId": last["stop_id"],
            "destinationStopName": destination_stop.get("stop_name"),
            "scheduledTripDeparture": departure.isoformat(),
            "scheduledTripArrival": arrival.isoformat(),
            "requestTime": request_time.isoformat(),
        }

        Path(request_path).write_text(json.dumps(request, separators=(",", ":")), encoding="utf-8")
        Path(metadata_path).write_text(json.dumps(metadata, indent=2), encoding="utf-8")
        print(json.dumps(metadata, indent=2))


def verify(response_path: str):
    response = json.loads(Path(response_path).read_text(encoding="utf-8"))
    journeys = response.get("journeys")
    if not isinstance(journeys, list) or not journeys:
        raise RuntimeError("Orientation returned no Journey alternatives")
    if len(journeys) > 8:
        raise RuntimeError("Orientation returned more than the accepted Journey alternative bound")

    saw_transit = False
    saw_geometry = False
    for journey in journeys:
        legs = journey.get("legs")
        if not isinstance(legs, list) or not legs:
            raise RuntimeError("Orientation returned a Journey without legs")
        for leg in legs:
            mode = leg.get("mode")
            if mode != "WALK":
                saw_transit = True
                service = leg.get("transitService")
                if not isinstance(service, dict) or not service.get("label"):
                    raise RuntimeError("Transit Journey leg has no provider-neutral service label")
            departure = leg.get("departure") or {}
            arrival = leg.get("arrival") or {}
            if not departure.get("scheduledTime") or not arrival.get("scheduledTime"):
                raise RuntimeError("Journey leg lost scheduled timing")
            geometry = leg.get("geometry")
            if isinstance(geometry, list) and len(geometry) >= 2:
                saw_geometry = True

    if not saw_transit:
        raise RuntimeError("Orientation real-provider smoke returned no transit leg")
    if not saw_geometry:
        raise RuntimeError("Orientation real-provider smoke returned no decoded Journey leg geometry")

    forbidden = ("motis", "transitous", "tripId", "routeId", "cursor", "rental", "rideSharing")
    serialized = json.dumps(response, separators=(",", ":")).lower()
    for token in forbidden:
        if token.lower() in serialized:
            raise RuntimeError(f"Provider-specific field leaked through Orientation response: {token}")

    print(f"PASS: {len(journeys)} provider-neutral Journey alternative(s), transit and decoded geometry present")


def main():
    if len(sys.argv) < 2:
        raise SystemExit("usage: motis-journey-smoke.py prepare <gtfs.zip> <request.json> <metadata.json> | verify <response.json>")
    command = sys.argv[1]
    if command == "prepare" and len(sys.argv) == 5:
        prepare(sys.argv[2], sys.argv[3], sys.argv[4])
    elif command == "verify" and len(sys.argv) == 3:
        verify(sys.argv[2])
    else:
        raise SystemExit("invalid arguments")


if __name__ == "__main__":
    main()
