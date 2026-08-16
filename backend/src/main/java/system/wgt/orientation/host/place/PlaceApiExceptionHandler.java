package system.wgt.orientation.host.place;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import system.wgt.orientation.application.place.PlaceProviderException;
import system.wgt.orientation.application.place.ProviderFailureKind;

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

    public record ErrorResponse(String code, String message) {
    }
}
