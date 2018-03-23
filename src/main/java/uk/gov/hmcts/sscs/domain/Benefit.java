package uk.gov.hmcts.sscs.domain;

public enum Benefit {

    ESA("Employment Support Allowance"),
    JSA("Job Seekers Allowance"),
    PIP("Personal Independence Payment");

    private String description;

    Benefit(String description) {
        this.description = description;
    }

    public static Benefit getBenefitByCode(String code) {
        Benefit b = null;
        for (Benefit type : Benefit.values()) {
            if (type.name().equals(code)) {
                b = type;
            }
        }
        return b;
    }

    public String getDescription() {
        return description;
    }
}
