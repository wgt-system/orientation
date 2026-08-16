package system.wgt.orientation.domain.place;

import java.util.Optional;

public record ReverseGeocodeQuery(Coordinate coordinate, Optional<String> language) {

    public ReverseGeocodeQuery {
        if (coordinate == null) {
            throw new IllegalArgumentException("Coordinate is required.");
        }
        language = language == null ? Optional.empty() : language.map(String::trim).filter(value -> !value.isEmpty());
    }
}
