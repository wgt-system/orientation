package system.wgt.orientation.domain.place;

import java.util.Optional;

public record AddressComponents(
        Optional<String> name,
        Optional<String> street,
        Optional<String> houseNumber,
        Optional<String> postcode,
        Optional<String> city,
        Optional<String> county,
        Optional<String> state,
        Optional<String> country,
        Optional<String> countryCode) {

    public AddressComponents {
        name = normalize(name);
        street = normalize(street);
        houseNumber = normalize(houseNumber);
        postcode = normalize(postcode);
        city = normalize(city);
        county = normalize(county);
        state = normalize(state);
        country = normalize(country);
        countryCode = normalize(countryCode);
    }

    public static AddressComponents empty() {
        return new AddressComponents(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static Optional<String> normalize(Optional<String> value) {
        return value == null ? Optional.empty() : value.map(String::trim).filter(text -> !text.isEmpty());
    }
}
