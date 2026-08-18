package system.wgt.orientation.domain.journey;

public enum JourneyLegMode {
    WALK,
    RAIL,
    SUBURBAN_RAIL,
    SUBWAY,
    TRAM,
    BUS,
    COACH,
    FERRY,
    OTHER_TRANSIT;

    public boolean isTransit() {
        return this != WALK;
    }
}
