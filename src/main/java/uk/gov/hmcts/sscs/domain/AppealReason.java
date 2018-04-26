package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppealReason {
    private AppealReasonDetails value;

    public AppealReason(AppealReasonDetails value) {
        this.value = value;
    }

}
