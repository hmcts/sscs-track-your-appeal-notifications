package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppealReasonDetails {
    private String reason;
    private String description;

    public AppealReasonDetails(String reason,
                               String description) {
        this.reason = reason;
        this.description = description;
    }
}
