package system.wgt.orientation.domain.research;

import system.wgt.orientation.domain.place.Coordinate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public record SpatialResearchQuestion(
        String questionRef,
        String text,
        String centerLabel,
        Optional<Coordinate> centerCoordinate,
        int radiusMeters,
        List<SpatialResearchCriterion> criteria) {

    private static final Pattern LOCAL_REF = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,79}");

    public SpatialResearchQuestion {
        if (questionRef == null || !LOCAL_REF.matcher(questionRef).matches()) {
            throw new IllegalArgumentException("questionRef must be a valid local reference.");
        }
        text = requireText(text, "text", 2000);
        centerLabel = requireText(centerLabel, "centerLabel", 500);
        centerCoordinate = centerCoordinate == null ? Optional.empty() : centerCoordinate;
        if (radiusMeters < 1 || radiusMeters > 500_000) {
            throw new IllegalArgumentException("radiusMeters must be between 1 and 500000.");
        }
        if (criteria == null || criteria.isEmpty() || criteria.size() > 20) {
            throw new IllegalArgumentException("criteria must contain between 1 and 20 entries.");
        }
        criteria = List.copyOf(criteria);
        var refs = new LinkedHashSet<String>();
        for (var criterion : criteria) {
            if (criterion == null || !refs.add(criterion.criterionRef())) {
                throw new IllegalArgumentException("criterionRef values must be unique.");
            }
        }
    }

    private static String requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(name + " is too long.");
        }
        return trimmed;
    }
}
