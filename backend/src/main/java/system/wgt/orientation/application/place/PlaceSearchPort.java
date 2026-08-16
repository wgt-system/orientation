package system.wgt.orientation.application.place;

import system.wgt.orientation.domain.place.Place;
import system.wgt.orientation.domain.place.PlaceSearchQuery;

import java.util.List;

public interface PlaceSearchPort {
    List<Place> search(PlaceSearchQuery query);
}
