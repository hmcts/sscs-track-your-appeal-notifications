package uk.gov.hmcts.reform.sscs.callback.handlers;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

public class HandlerHelper {

    private HandlerHelper() {

    }

    public static SscsCaseDataWrapper buildTestCallbackForGivenData(SscsCaseData sscsCaseData, State state, NotificationEventType eventType) {
        return SscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .oldSscsCaseData(sscsCaseData)
                .notificationEventType(eventType)
                .state(state).build();
    }
}
