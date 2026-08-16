package system.wgt.orientation.domain.place;

import java.util.Optional;

public record PlaceSearchQuery(
        String text,
        int limit,
        Optional<String> language,
        Optional<LocationBias> locationBias) {

    public static final int DEFAULT_LIMIT = 5;
    public static final int MAX_LIMIT = 10;

    public PlaceSearchQuery {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Search query must not be blank.");
        }
        text = text.trim();
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("Search limit must be between 1 and 10.");
        }
        language = language == null ? Optional.empty() : language.map(String::trim).filter(value -> !value.isEmpty());
        locationBias = locationBias == null ? Optional.empty() : locationBias;
    }

    public record LocationBias(Coordinate coordinate, int zoom) {
        public LocationBias {
            if (coordinate == null) {
                throw new IllegalArgumentException("Location bias coordinate is required.");
            }
            if (zoom < 0 || zoom > 24) {
                throw new IllegalArgumentException("Location bias zoom must be between 0 and 24.");
            }
        }
    }
}
