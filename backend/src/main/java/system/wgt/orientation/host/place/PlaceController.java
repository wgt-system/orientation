package system.wgt.orientation.host.place;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import system.wgt.orientation.application.place.PlaceSearchService;
import system.wgt.orientation.application.place.ReverseGeocodingService;
import system.wgt.orientation.domain.place.AddressComponents;
import system.wgt.orientation.domain.place.BoundingBox;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.place.Place;
import system.wgt.orientation.domain.place.PlaceSearchQuery;
import system.wgt.orientation.domain.place.ReverseGeocodeQuery;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_QUERY_LENGTH = 200;
    private static final int MAX_LANGUAGE_LENGTH = 32;

    private final PlaceSearchService searchService;
    private final ReverseGeocodingService reverseService;

    public PlaceController(PlaceSearchService searchService, ReverseGeocodingService reverseService) {
        this.searchService = searchService;
        this.reverseService = reverseService;
    }

    @GetMapping("/search")
    public PlaceSearchResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) Double biasLat,
            @RequestParam(required = false) Double biasLon,
            @RequestParam(required = false) Integer biasZoom) {
        String query = boundedText(q, "q", MAX_QUERY_LENGTH);
        int resultLimit = limit == null ? DEFAULT_LIMIT : limit;
        Optional<String> language = optionalLanguage(lang);
        Optional<PlaceSearchQuery.LocationBias> bias = locationBias(biasLat, biasLon, biasZoom);
        return new PlaceSearchResponse(searchService.search(new PlaceSearchQuery(query, resultLimit, language, bias))
                .stream().map(PlaceDto::from).toList());
    }

    @GetMapping("/reverse")
    public ReverseGeocodeResponse reverse(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) String lang) {
        if (lat == null || lon == null) {
            throw new IllegalArgumentException("lat and lon are required.");
        }
        Coordinate coordinate = new Coordinate(lon, lat);
        return new ReverseGeocodeResponse(reverseService.reverse(new ReverseGeocodeQuery(coordinate, optionalLanguage(lang)))
                .map(PlaceDto::from).orElse(null));
    }

    private Optional<PlaceSearchQuery.LocationBias> locationBias(Double latitude, Double longitude, Integer zoom) {
        if (latitude == null && longitude == null && zoom == null) {
            return Optional.empty();
        }
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("biasLat and biasLon must be supplied together.");
        }
        return Optional.of(new PlaceSearchQuery.LocationBias(new Coordinate(longitude, latitude), zoom == null ? 12 : zoom));
    }

    private Optional<String> optionalLanguage(String language) {
        if (language == null || language.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(boundedText(language, "lang", MAX_LANGUAGE_LENGTH));
    }

    private String boundedText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(name + " is too long.");
        }
        return trimmed;
    }

    public record PlaceSearchResponse(List<PlaceDto> places) {
        public PlaceSearchResponse {
            places = List.copyOf(places);
        }
    }

    public record ReverseGeocodeResponse(PlaceDto place) {
    }

    public record PlaceDto(
            String providerReference,
            String displayLabel,
            CoordinateDto coordinate,
            BoundingBoxDto extent,
            String kind,
            AddressDto address) {

        static PlaceDto from(Place place) {
            return new PlaceDto(place.providerReference(), place.displayLabel(), CoordinateDto.from(place.coordinate()),
                    place.extent().map(BoundingBoxDto::from).orElse(null), place.kind().orElse(null), AddressDto.from(place.address()));
        }
    }

    public record CoordinateDto(double longitude, double latitude) {
        static CoordinateDto from(Coordinate coordinate) {
            return new CoordinateDto(coordinate.longitude(), coordinate.latitude());
        }
    }

    public record BoundingBoxDto(double west, double south, double east, double north) {
        static BoundingBoxDto from(BoundingBox box) {
            return new BoundingBoxDto(box.west(), box.south(), box.east(), box.north());
        }
    }

    public record AddressDto(String name, String street, String houseNumber, String postcode, String city,
                             String county, String state, String country, String countryCode) {
        static AddressDto from(AddressComponents address) {
            return new AddressDto(address.name().orElse(null), address.street().orElse(null), address.houseNumber().orElse(null),
                    address.postcode().orElse(null), address.city().orElse(null), address.county().orElse(null),
                    address.state().orElse(null), address.country().orElse(null), address.countryCode().orElse(null));
        }
    }
}
