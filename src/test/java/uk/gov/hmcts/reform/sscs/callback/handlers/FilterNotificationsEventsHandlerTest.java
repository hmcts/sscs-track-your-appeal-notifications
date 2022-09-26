package uk.gov.hmcts.reform.sscs.callback.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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
    @Parameters({
        "ACTION_HEARING_RECORDING_REQUEST",
        "ACTION_POSTPONEMENT_REQUEST_WELSH",
        "ADJOURNED",
        "ADMIN_APPEAL_WITHDRAWN",
        "APPEAL_DORMANT",
        "APPEAL_LAPSED",
        "APPEAL_RECEIVED",
        "APPEAL_WITHDRAWN",
        "DECISION_ISSUED",
        "DECISION_ISSUED_WELSH",
        "DIRECTION_ISSUED",
        "DIRECTION_ISSUED_WELSH",
        "DIRECTION_ISSUED_WELSH",
        "DRAFT_TO_NON_COMPLIANT",
        "DRAFT_TO_VALID_APPEAL_CREATED",
        "DWP_APPEAL_LAPSED",
        "DWP_RESPONSE_RECEIVED",
        "DWP_UPLOAD_RESPONSE",
        "EVIDENCE_RECEIVED",
        "HEARING_BOOKED",
        "ISSUE_ADJOURNMENT_NOTICE",
        "ISSUE_FINAL_DECISION",
        "ISSUE_FINAL_DECISION_WELSH",
        "JOINT_PARTY_ADDED",
        "NON_COMPLIANT",
        "POSTPONEMENT",
        "PROCESS_AUDIO_VIDEO",
        "PROCESS_AUDIO_VIDEO_WELSH",
        "REISSUE_DOCUMENT",
        "REQUEST_INFO_INCOMPLETE",
        "RESEND_APPEAL_CREATED",
        "REVIEW_CONFIDENTIALITY_REQUEST",
        "STRUCK_OUT",
        "SUBSCRIPTION_UPDATED",
        "UPDATE_OTHER_PARTY_DATA",
        "VALID_APPEAL_CREATED"
    })
    public void willHandleEvents(NotificationEventType notificationEventType) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder().notificationEventType(notificationEventType).build();
        assertTrue(handler.canHandle(callback));
        handler.handle(callback);
        verify(notificationService).manageNotificationAndSubscription(eq(new CcdNotificationWrapper(callback)), eq(false));
        verifyNoInteractions(retryNotificationService);
    }

    @Test
    @Parameters({
        "DO_NOT_SEND",
        "SYA_APPEAL_CREATED",
        "null"})
    public void willNotHandleEvents(@Nullable NotificationEventType notificationEventType) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder().notificationEventType(notificationEventType).build();
        assertFalse(handler.canHandle(callback));
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"DO_NOT_SEND", "SYA_APPEAL_CREATED"})
    public void willThrowExceptionIfTriesToHandleEvents(NotificationEventType notificationEventType) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder().notificationEventType(notificationEventType).build();
        handler.handle(callback);
    }

    @Test(expected = NotificationServiceException.class)
    public void shouldCallToRescheduleNotificationWhenErrorIsNotificationServiceExceptionError() {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder().notificationEventType(VALID_APPEAL_CREATED).build();
        doThrow(new NotificationServiceException("123", new RuntimeException("error"))).when(notificationService).manageNotificationAndSubscription(any(), eq(false));
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
        doThrow(new RuntimeException("error")).when(notificationService).manageNotificationAndSubscription(any(), eq(false));
        try {
            handler.handle(callback);
        } catch (RuntimeException e) {
            verifyNoInteractions(retryNotificationService);
            throw e;
        }
    }

    @Test
    @Parameters({"grant", "refuse"})
    public void willHandleActionPostponementRequestEvents(String actionSelected) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder()
                .notificationEventType(ACTION_POSTPONEMENT_REQUEST)
                .oldSscsCaseData(SscsCaseData.builder()
                        .postponementRequest(PostponementRequest.builder()
                                .actionPostponementRequestSelected(actionSelected)
                                .build())
                        .build())
                .build();
        assertTrue(handler.canHandle(callback));
        handler.handle(callback);
        verify(notificationService).manageNotificationAndSubscription(eq(new CcdNotificationWrapper(callback)), eq(false));
        verifyNoInteractions(retryNotificationService);
    }

    @Test
    @Parameters({"grant", "refuse"})
    public void willNotHandleActionPostponementRequestEvents_sendToJudgeAction(String actionSelected) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder()
                .notificationEventType(ACTION_POSTPONEMENT_REQUEST)
                .oldSscsCaseData(SscsCaseData.builder()
                        .postponementRequest(PostponementRequest.builder()
                                .actionPostponementRequestSelected("sendToJudge")
                                .build())
                        .build())
                .build();
        assertFalse(handler.canHandle(callback));
    }

    @Test
    @Parameters(method = "eventTypeAndNewAppointees")
    public void willHandleDeathOfAppellantEventsWithNewAppointee(NotificationEventType notificationEventType, Appointee existing, Appointee newlyAdded) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder()
                .notificationEventType(notificationEventType)
                .oldSscsCaseData(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder()
                                        .appointee(existing)
                                        .isAppointee(existing != null ? "yes" : "no")
                                        .build())
                                .build())
                        .build())
                .newSscsCaseData(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder()
                                        .appointee(newlyAdded)
                                        .isAppointee(newlyAdded != null ? "yes" : "no")
                                        .build())
                                .build())
                        .build())
                .build();
        assertTrue(handler.canHandle(callback));
        handler.handle(callback);
        verify(notificationService).manageNotificationAndSubscription(eq(new CcdNotificationWrapper(callback)), eq(false));
        verifyNoInteractions(retryNotificationService);
    }

    @Test
    @Parameters(method = "eventTypeAndNoNewAppointees")
    public void willNotHandleDeathOfAppellantEventsWithoutNewAppointee(NotificationEventType notificationEventType, Appointee existing, Appointee newlyAdded) {
        SscsCaseDataWrapper callback = SscsCaseDataWrapper.builder()
                .notificationEventType(notificationEventType)
                .oldSscsCaseData(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder()
                                        .appointee(existing)
                                        .isAppointee(existing != null ? "yes" : "no")
                                        .build())
                                .build())
                        .build())
                .newSscsCaseData(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder()
                                        .appointee(newlyAdded)
                                        .isAppointee(newlyAdded != null ? "yes" : "no")
                                        .build())
                                .build())
                        .build())
                .build();
        assertFalse(handler.canHandle(callback));
    }

    private Object[] eventTypeAndNewAppointees() {
        Appointee appointeeBefore = Appointee.builder().name(Name.builder().firstName("John").build()).build();
        Appointee appointeeAfter = Appointee.builder().name(Name.builder().firstName("Harry").build()).build();
        return new Object[]{
            new Object[]{DEATH_OF_APPELLANT, null, Appointee.builder().build()},
            new Object[]{DEATH_OF_APPELLANT, appointeeBefore, appointeeAfter},
            new Object[]{PROVIDE_APPOINTEE_DETAILS, null, Appointee.builder().build()},
            new Object[]{PROVIDE_APPOINTEE_DETAILS, appointeeBefore, appointeeAfter},
        };
    }

    private Object[] eventTypeAndNoNewAppointees() {
        Appointee appointee = Appointee.builder().name(Name.builder().firstName("John").build()).build();
        return new Object[]{
            new Object[]{DEATH_OF_APPELLANT, null, null},
            new Object[]{DEATH_OF_APPELLANT, appointee, appointee},
            new Object[]{PROVIDE_APPOINTEE_DETAILS, null, null},
            new Object[]{PROVIDE_APPOINTEE_DETAILS, appointee, appointee},
        };
    }
}
