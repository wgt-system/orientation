package system.wgt.orientation.application.place;

import system.wgt.orientation.domain.place.Place;
import system.wgt.orientation.domain.place.ReverseGeocodeQuery;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReverseGeocodingService {
    private final ReverseGeocodingPort port;

    public ReverseGeocodingService(ReverseGeocodingPort port) {
        this.port = port;
    }

    public Optional<Place> reverse(ReverseGeocodeQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Reverse query is required.");
        }
        return port.reverse(query);
    }
}
