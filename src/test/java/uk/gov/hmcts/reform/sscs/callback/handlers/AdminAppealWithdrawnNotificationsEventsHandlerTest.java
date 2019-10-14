package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertEquals;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

@RunWith(JUnitParamsRunner.class)
public class AdminAppealWithdrawnNotificationsEventsHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private NotificationService notificationService;
    private AdminAppealWithdrawnNotificationsEventsHandler handler =
        new AdminAppealWithdrawnNotificationsEventsHandler(notificationService);

    @Test
    @Parameters({"ADMIN_APPEAL_WITHDRAWN, true", "ADJOURNED_NOTIFICATION, false", "SYA_APPEAL_CREATED_NOTIFICATION, false"})
    public void canHandle(NotificationEventType notificationEventType, boolean expectedResult) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder()
            .notificationEventType(notificationEventType)
            .build();

        boolean currentResult = handler.canHandle(callback);

        assertEquals(expectedResult, currentResult);
    }

    @Test
    public void handle() {
    }

    @Test
    public void getPriority() {
    }
}