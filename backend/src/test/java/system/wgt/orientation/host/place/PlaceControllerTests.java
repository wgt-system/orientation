package system.wgt.orientation.host.place;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import system.wgt.orientation.application.place.PlaceProviderException;
import system.wgt.orientation.application.place.PlaceSearchPort;
import system.wgt.orientation.application.place.PlaceSearchService;
import system.wgt.orientation.application.place.ProviderFailureKind;
import system.wgt.orientation.application.place.ReverseGeocodingPort;
import system.wgt.orientation.application.place.ReverseGeocodingService;
import system.wgt.orientation.domain.place.Coordinate;
import system.wgt.orientation.domain.place.Place;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PlaceControllerTests {

    @Test
    void searchReturnsOrientationDtoAndTrimsInput() throws Exception {
        PlaceSearchPort searchPort = query -> List.of(new Place("N:1", query.text(), new Coordinate(9.99, 53.55),
                Optional.empty(), Optional.of("city"), null));
        ReverseGeocodingPort reversePort = query -> Optional.empty();
        MockMvc mvc = standaloneSetup(new PlaceController(new PlaceSearchService(searchPort), new ReverseGeocodingService(reversePort)))
                .setControllerAdvice(new PlaceApiExceptionHandler()).build();

        mvc.perform(get("/api/v1/places/search").param("q", " Hamburg ").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"providerReference\":\"N:1\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"displayLabel\":\"Hamburg\"")));
    }

    @Test
    void invalidInputIsBadRequest() throws Exception {
        PlaceSearchPort searchPort = query -> List.of();
        ReverseGeocodingPort reversePort = query -> Optional.empty();
        MockMvc mvc = standaloneSetup(new PlaceController(new PlaceSearchService(searchPort), new ReverseGeocodingService(reversePort)))
                .setControllerAdvice(new PlaceApiExceptionHandler()).build();

        mvc.perform(get("/api/v1/places/search").param("q", " "))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("invalid-input")));
        mvc.perform(get("/api/v1/places/reverse").param("lat", "91").param("lon", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void providerFailureMapsToServiceUnavailable() throws Exception {
        PlaceSearchPort searchPort = query -> {
            throw new PlaceProviderException(ProviderFailureKind.TIMEOUT, "internal");
        };
        ReverseGeocodingPort reversePort = query -> Optional.empty();
        MockMvc mvc = standaloneSetup(new PlaceController(new PlaceSearchService(searchPort), new ReverseGeocodingService(reversePort)))
                .setControllerAdvice(new PlaceApiExceptionHandler()).build();

        mvc.perform(get("/api/v1/places/search").param("q", "Hamburg"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("provider.timeout")));
    }
}
