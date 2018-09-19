package uk.gov.hmcts.reform.sscs.factory;

import static java.lang.Long.valueOf;

import java.util.Objects;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.service.scheduler.CohActionSerializer;
import uk.gov.hmcts.reform.sscs.service.scheduler.CohJobPayload;

public class CohNotificationWrapper extends CcdNotificationWrapper {
    private final String onlineHearingId;

    public CohNotificationWrapper(String onlineHearingId, SscsCaseDataWrapper responseWrapper) {
        super(responseWrapper);
        this.onlineHearingId = onlineHearingId;
    }

    public String getOnlineHearingId() {
        return onlineHearingId;
    }

    @Override
    public String getSchedulerPayload() {
        return new CohActionSerializer().serialize(new CohJobPayload(valueOf(getCaseId()), getOnlineHearingId()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CohNotificationWrapper that = (CohNotificationWrapper) o;
        return Objects.equals(onlineHearingId, that.onlineHearingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), onlineHearingId);
    }
}
