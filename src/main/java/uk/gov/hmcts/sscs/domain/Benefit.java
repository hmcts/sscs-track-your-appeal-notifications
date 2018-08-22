package uk.gov.hmcts.sscs.domain;

import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.hmcts.sscs.exception.BenefitMappingException;

public enum Benefit {

    ESA("Employment and Support Allowance"),
    JSA("Job Seekers Allowance"),
    PIP("Personal Independence Payment");

    private String description;

    private static final org.slf4j.Logger LOG = getLogger(Benefit.class);

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
        if (b == null) {
            BenefitMappingException benefitMappingException =
                    new BenefitMappingException(new Exception(code + " is not a recognised benefit type"));
            LOG.error("Benefit type mapping error", benefitMappingException);
            throw benefitMappingException;
        }
        return b;
    }

    public String getDescription() {
        return description;
    }
}
