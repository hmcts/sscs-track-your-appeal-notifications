package uk.gov.hmcts.reform.sscs.factory;

import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;

public interface NotificationWrapper {
    EventType getNotificationType();

    SscsCaseData getNewSscsCaseData();

    String getCaseId();

    Subscription getAppellantSubscription();

    SscsCaseDataWrapper getSscsCaseDataWrapper();
}
