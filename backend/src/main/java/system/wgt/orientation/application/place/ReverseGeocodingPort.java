package system.wgt.orientation.application.place;

import system.wgt.orientation.domain.place.Place;
import system.wgt.orientation.domain.place.ReverseGeocodeQuery;

import java.util.Optional;

public interface ReverseGeocodingPort {
    Optional<Place> reverse(ReverseGeocodeQuery query);
}
