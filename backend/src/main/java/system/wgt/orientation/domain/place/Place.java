package system.wgt.orientation.domain.place;

import java.util.Optional;

public record Place(
        String providerReference,
        String displayLabel,
        Coordinate coordinate,
        Optional<BoundingBox> extent,
        Optional<String> kind,
        AddressComponents address) {

    public Place {
        providerReference = requireText(providerReference, "Provider reference");
        displayLabel = requireText(displayLabel, "Display label");
        if (coordinate == null) {
            throw new IllegalArgumentException("Coordinate is required.");
        }
        extent = extent == null ? Optional.empty() : extent;
        kind = kind == null ? Optional.empty() : kind.map(String::trim).filter(text -> !text.isEmpty());
        if (address == null) {
            address = AddressComponents.empty();
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value.trim();
    }
}
