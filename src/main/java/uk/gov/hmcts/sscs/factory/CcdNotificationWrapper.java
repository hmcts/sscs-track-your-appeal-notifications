package uk.gov.hmcts.sscs.factory;

import java.util.Objects;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.EventType;

public class CcdNotificationWrapper implements NotificationWrapper {
    private final CcdResponseWrapper responseWrapper;

    public CcdNotificationWrapper(CcdResponseWrapper responseWrapper) {
        this.responseWrapper = responseWrapper;
    }

    @Override
    public EventType getNotificationType() {
        return responseWrapper.getNewCcdResponse().getNotificationType();
    }

    @Override
    public CcdResponse getNewCcdResponse() {
        return responseWrapper.getNewCcdResponse();
    }

    @Override
    public Subscription getAppellantSubscription() {
        return responseWrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription();
    }

    @Override
    public CcdResponseWrapper getCcdResponseWrapper() {
        return responseWrapper;
    }

    @Override
    public String getCaseId() {
        return responseWrapper.getNewCcdResponse().getCaseId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CcdNotificationWrapper that = (CcdNotificationWrapper) o;
        return Objects.equals(responseWrapper, that.responseWrapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseWrapper);
    }
}
