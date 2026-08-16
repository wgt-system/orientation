package system.wgt.orientation.host.place;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import system.wgt.orientation.application.place.PlaceProviderException;
import system.wgt.orientation.application.place.ProviderFailureKind;
import system.wgt.orientation.application.routing.RoutingFailureKind;
import system.wgt.orientation.application.routing.RoutingProviderException;

@RestControllerAdvice
public class PlaceApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> invalidInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse("invalid-input", exception.getMessage()));
    }

    @ExceptionHandler(PlaceProviderException.class)
    ResponseEntity<ErrorResponse> providerFailure(PlaceProviderException exception) {
        return switch (exception.kind()) {
            case RATE_LIMITED -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("provider.rate-limited", "The place provider rate limit was reached."));
            case INVALID_RESPONSE -> ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse("provider.invalid-response", "The place provider returned an invalid response."));
            case TIMEOUT -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("provider.timeout", "The place provider timed out."));
            case UNAVAILABLE -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("provider.unavailable", "The place provider is unavailable."));
        };
    }

    @ExceptionHandler(RoutingProviderException.class)
    ResponseEntity<ErrorResponse> routingFailure(RoutingProviderException exception) {
        return switch (exception.kind()) {
            case NO_ROUTE_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("routing.no-route", "No route was found."));
            case RATE_LIMITED -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("routing.rate-limited", "The routing provider rate limit was reached."));
            case INVALID_PROVIDER_RESPONSE -> ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse("routing.invalid-provider-response", "The routing provider returned an invalid response."));
            case TIMEOUT -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("routing.timeout", "The routing provider timed out."));
            case PROVIDER_UNAVAILABLE -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("routing.provider-unavailable", "The routing provider is unavailable."));
        };
    }

    public record ErrorResponse(String code, String message) {
    }
}
