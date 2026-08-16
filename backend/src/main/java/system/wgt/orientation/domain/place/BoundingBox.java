package system.wgt.orientation.domain.place;

public record BoundingBox(double west, double south, double east, double north) {

    public BoundingBox {
        new Coordinate(west, south);
        new Coordinate(east, north);
        if (west > east || south > north) {
            throw new IllegalArgumentException("Bounding box limits are not ordered.");
        }
    }
}
