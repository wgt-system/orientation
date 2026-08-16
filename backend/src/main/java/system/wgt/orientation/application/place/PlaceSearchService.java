package system.wgt.orientation.application.place;

import system.wgt.orientation.domain.place.Place;
import system.wgt.orientation.domain.place.PlaceSearchQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaceSearchService {
    private final PlaceSearchPort port;

    public PlaceSearchService(PlaceSearchPort port) {
        this.port = port;
    }

    public List<Place> search(PlaceSearchQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Search query is required.");
        }
        return List.copyOf(port.search(query));
    }
}
