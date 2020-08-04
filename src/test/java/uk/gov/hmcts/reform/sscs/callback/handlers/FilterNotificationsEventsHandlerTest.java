package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

@RunWith(JUnitParamsRunner.class)
public class FilterNotificationsEventsHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private NotificationService notificationService;
    private FilterNotificationsEventsHandler handler;

    @Before
    public void setUp() {
        handler = new FilterNotificationsEventsHandler(notificationService);
    }

    @Test
    @Parameters({"DWP_UPLOAD_RESPONSE_NOTIFICATION", "DIRECTION_ISSUED", "VALID_APPEAL_CREATED", "NON_COMPLIANT_NOTIFICATION", "REISSUE_DOCUMENT", "STRUCK_OUT", "ISSUE_ADJOURNMENT_NOTICE"})
    public void willHandleEvents(NotificationEventType notificationEventType) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder().notificationEventType(notificationEventType).build();
        handler.handle(callback);
        verify(notificationService).manageNotificationAndSubscription(eq(new CcdNotificationWrapper(callback)));
    }

    @Test
    @Parameters({"ADJOURNED_NOTIFICATION", "DECISION_ISSUED", "ISSUE_FINAL_DECISION"})
    public void willNotHandleEvents(NotificationEventType notificationEventType) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder().notificationEventType(notificationEventType).build();
        assertFalse(handler.canHandle(callback));
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ADJOURNED_NOTIFICATION", "APPEAL_LAPSED_NOTIFICATION"})
    public void willThrowExceptionIfTriesToHandleEvents(NotificationEventType notificationEventType) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder().notificationEventType(notificationEventType).build();
        handler.handle(callback);
    }

}
