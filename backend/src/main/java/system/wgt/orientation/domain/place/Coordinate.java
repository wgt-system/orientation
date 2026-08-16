package system.wgt.orientation.domain.place;

public record Coordinate(double longitude, double latitude) {

    public Coordinate {
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be finite and between -180 and 180.");
        }
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be finite and between -90 and 90.");
        }
    }
}
