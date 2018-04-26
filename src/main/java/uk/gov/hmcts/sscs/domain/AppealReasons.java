package uk.gov.hmcts.sscs.domain;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppealReasons {
    private List<AppealReason> reasons;
    private String otherReasons;

    public AppealReasons(List<AppealReason> reasons,
                         String otherReasons) {
        this.reasons = reasons;
        this.otherReasons = otherReasons;
    }
}
