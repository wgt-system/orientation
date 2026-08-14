# Valhalla Runtime

Valhalla is the selected upstream routing engine behind an Orientation adapter.

Bootstrap deliberately does not pin a container image/configuration before the routing slice is implemented and tested.

When introduced:

- pin an explicit upstream version/image digest;
- keep configuration reproducible;
- document map-data acquisition/build steps;
- expose Valhalla only through the Orientation adapter boundary to WGT-system consumers;
- test timeout/unavailable/invalid-response behavior.
