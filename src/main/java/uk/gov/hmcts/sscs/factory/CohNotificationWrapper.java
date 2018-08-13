package uk.gov.hmcts.sscs.factory;

import java.util.Objects;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;

public class CohNotificationWrapper extends CcdNotificationWrapper {
    private final IdamTokens idamTokens;
    private final String onlineHearingId;

    public CohNotificationWrapper(IdamTokens idamTokens, String onlineHearingId, CcdResponseWrapper responseWrapper) {
        super(responseWrapper);
        this.idamTokens = idamTokens;
        this.onlineHearingId = onlineHearingId;
    }

    public IdamTokens getIdamTokens() {
        return idamTokens;
    }

    public String getOnlineHearingId() {
        return onlineHearingId;
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
