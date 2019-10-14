package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADJOURNED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

@RunWith(JUnitParamsRunner.class)
public class AdminAppealWithdrawnNotificationsEventsHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminAppealWithdrawnNotificationsEventsHandler handler;

    @Test
    @Parameters({"ADMIN_APPEAL_WITHDRAWN, true", "ADJOURNED_NOTIFICATION, false", "SYA_APPEAL_CREATED_NOTIFICATION, false"})
    public void canHandle(NotificationEventType notificationEventType, boolean expectedResult) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder()
            .notificationEventType(notificationEventType)
            .build();
        assertEquals(expectedResult, handler.canHandle(callback));
    }

    @Test
    public void handle() {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder()
            .notificationEventType(ADMIN_APPEAL_WITHDRAWN)
            .build();
        Mockito.doNothing().when(notificationService).manageNotificationAndSubscription(any(CcdNotificationWrapper.class));

        handler.handle(callback);

        Mockito.verify(notificationService, Mockito.times(1))
            .manageNotificationAndSubscription(any(CcdNotificationWrapper.class));
    }

    @Test(expected = IllegalStateException.class)
    public void handleThrowException() {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder()
            .notificationEventType(ADJOURNED_NOTIFICATION)
            .build();
        handler.handle(callback);
    }

}