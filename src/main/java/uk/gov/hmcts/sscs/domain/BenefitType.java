package uk.gov.hmcts.sscs.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class BenefitType {
    private String code;

    public BenefitType(String code) {
        this.code = code;
    }
}
