package system.wgt.orientation.domain.place;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaceDomainTests {

    @Test
    void trimsSearchTextAndAcceptsBoundedLimit() {
        PlaceSearchQuery query = new PlaceSearchQuery("  Hamburg  ", 5, Optional.empty(), Optional.empty());

        assertEquals("Hamburg", query.text());
        assertEquals(5, query.limit());
    }

    @Test
    void rejectsBlankSearchAndInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> new PlaceSearchQuery("  ", 5, Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new PlaceSearchQuery("Hamburg", 0, Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new PlaceSearchQuery("Hamburg", 11, Optional.empty(), Optional.empty()));
    }

    @Test
    void validatesCoordinateRanges() {
        assertThrows(IllegalArgumentException.class, () -> new Coordinate(181, 0));
        assertThrows(IllegalArgumentException.class, () -> new Coordinate(0, 91));
        assertEquals(new Coordinate(10, 50), new Coordinate(10, 50));
    }
}
