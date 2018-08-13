package uk.gov.hmcts.sscs.factory;

import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.notify.EventType;

public interface NotificationWrapper {
    EventType getNotificationType();

    CcdResponse getNewCcdResponse();

    String getCaseId();

    Subscription getAppellantSubscription();

    CcdResponseWrapper getCcdResponseWrapper();
}
