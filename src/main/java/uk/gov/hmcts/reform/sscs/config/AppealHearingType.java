package uk.gov.hmcts.reform.sscs.config;

public enum AppealHearingType {
    ONLINE,
    REGULAR,
    PAPER("paper");

    private String name;

    AppealHearingType(String name) {
        this.name = name;
    }

    AppealHearingType() {

    }

    public String getName() {
        return name;
    }
}
