package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.VALID_APPEAL_CREATED;

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
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationService;
import uk.gov.hmcts.reform.sscs.service.RetryNotificationService;

@RunWith(JUnitParamsRunner.class)
public class FilterNotificationsEventsHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private NotificationService notificationService;

    @Mock
    private RetryNotificationService retryNotificationService;

    private FilterNotificationsEventsHandler handler;

    @Before
    public void setUp() {
        handler = new FilterNotificationsEventsHandler(notificationService, retryNotificationService);
    }

    @Test
    @Parameters({"ADJOURNED_NOTIFICATION", "ADMIN_APPEAL_WITHDRAWN", "APPEAL_DORMANT_NOTIFICATION",
            "APPEAL_LAPSED_NOTIFICATION", "APPEAL_RECEIVED_NOTIFICATION",
            "APPEAL_WITHDRAWN_NOTIFICATION", "DECISION_ISSUED", "DIRECTION_ISSUED",
            "DWP_APPEAL_LAPSED_NOTIFICATION", "DWP_RESPONSE_RECEIVED_NOTIFICATION",
            "DWP_UPLOAD_RESPONSE_NOTIFICATION", "EVIDENCE_RECEIVED_NOTIFICATION",
            "HEARING_BOOKED_NOTIFICATION", "ISSUE_ADJOURNMENT_NOTICE", "ISSUE_FINAL_DECISION",
            "NON_COMPLIANT_NOTIFICATION", "POSTPONEMENT_NOTIFICATION", "REISSUE_DOCUMENT",
            "REQUEST_INFO_INCOMPLETE", "RESEND_APPEAL_CREATED_NOTIFICATION", "STRUCK_OUT",
            "SUBSCRIPTION_UPDATED_NOTIFICATION", "VALID_APPEAL_CREATED"})
    public void willHandleEvents(NotificationEventType notificationEventType) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder().notificationEventType(notificationEventType).build();
        assertTrue(handler.canHandle(callback));
        handler.handle(callback);
        verify(notificationService).manageNotificationAndSubscription(eq(new CcdNotificationWrapper(callback)));
        verifyNoInteractions(retryNotificationService);
    }

    @Test
    @Parameters({"DO_NOT_SEND", "SYA_APPEAL_CREATED_NOTIFICATION"})
    public void willNotHandleEvents(NotificationEventType notificationEventType) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder().notificationEventType(notificationEventType).build();
        assertFalse(handler.canHandle(callback));
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"DO_NOT_SEND", "SYA_APPEAL_CREATED_NOTIFICATION"})
    public void willThrowExceptionIfTriesToHandleEvents(NotificationEventType notificationEventType) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder().notificationEventType(notificationEventType).build();
        handler.handle(callback);
    }

    @Test(expected = NotificationServiceException.class)
    public void shouldCallToRescheduleNotificationWhenErrorIsNotificationServiceExceptionError() {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder().notificationEventType(VALID_APPEAL_CREATED).build();
        doThrow(new NotificationServiceException("123", new RuntimeException("error"))).when(notificationService).manageNotificationAndSubscription(any());
        try {
            handler.handle(callback);
        } catch (NotificationServiceException e) {
            verify(retryNotificationService).rescheduleIfHandledGovNotifyErrorStatus(eq(1), eq(new CcdNotificationWrapper(callback)), any());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldRescheduleNotificationWhenErrorIsNotANotificationServiceException() {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder().notificationEventType(VALID_APPEAL_CREATED).build();
        doThrow(new RuntimeException("error")).when(notificationService).manageNotificationAndSubscription(any());
        try {
            handler.handle(callback);
        } catch (RuntimeException e) {
            verifyNoInteractions(retryNotificationService);
            throw e;
        }
    }

}
