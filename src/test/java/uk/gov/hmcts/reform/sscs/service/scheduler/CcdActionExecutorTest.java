package uk.gov.hmcts.reform.sscs.service.scheduler;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_REMINDER_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.NotificationService;
import uk.gov.hmcts.reform.sscs.service.RetryNotificationService;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public class CcdActionExecutorTest {

    private static final String JOB_GROUP = "group";
    private static final String JOB_ID = "1";
    private CcdActionExecutor ccdActionExecutor;

    @Mock
    private NotificationService notificationService;

    @Mock
    private IdamService idamService;

    @Mock
    private CcdService ccdService;

    @Mock
    private RetryNotificationService retryNotificationService;

    private SscsCaseData newSscsCaseData;
    private SscsCaseDetails caseDetails;
    private SscsCaseDataWrapper wrapper;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        initMocks(this);

        Jackson2ObjectMapperBuilder objectMapperBuilder =
                new Jackson2ObjectMapperBuilder()
                        .featuresToEnable(READ_ENUMS_USING_TO_STRING)
                        .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                        .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
                        .serializationInclusion(JsonInclude.Include.NON_ABSENT);

        final ObjectMapper mapper = objectMapperBuilder.createXmlMapper(false).build();
        mapper.registerModule(new JavaTimeModule());

        final SscsCaseCallbackDeserializer deserializer = new SscsCaseCallbackDeserializer(mapper);

        ccdActionExecutor = new CcdActionExecutor(notificationService, retryNotificationService, ccdService, idamService, deserializer);

        caseDetails = SscsCaseDetails.builder().id(456L).caseTypeId("123").state("appealCreated").build();

        newSscsCaseData = SscsCaseData.builder().ccdCaseId("456").esaSscsCaseData(SscsEsaCaseData.builder().build()).build();
        caseDetails.setData(newSscsCaseData);

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void givenAReminderIsTriggered_thenActionExecutorShouldProcessTheJob() {
        wrapper = SscsCaseDataWrapper.builder().state(State.APPEAL_CREATED).newSscsCaseData(newSscsCaseData).notificationEventType(EVIDENCE_REMINDER_NOTIFICATION).build();
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);

        ccdActionExecutor.execute(JOB_ID, JOB_GROUP, EVIDENCE_REMINDER_NOTIFICATION.getId(), "123456");

        verify(notificationService).manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(ccdService).updateCase(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void givenAReminderIsTriggeredAndNotificationIsNotAReminderType_thenActionExecutorShouldProcessTheJobButNotWriteBackToCcd() {
        wrapper = SscsCaseDataWrapper.builder().state(State.APPEAL_CREATED).newSscsCaseData(newSscsCaseData).notificationEventType(SYA_APPEAL_CREATED_NOTIFICATION).build();
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);

        ccdActionExecutor.execute(JOB_ID, JOB_GROUP, SYA_APPEAL_CREATED_NOTIFICATION.getId(), "123456");

        verify(notificationService, times(1)).manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(ccdService, times(0)).updateCase(any(), any(), any(), any(), any(), any());
    }

    @Test
    @Parameters({"123, 0", "124,1", "333, 3"})
    public void shouldReturnTheCorrectCaseAndRetryValueFromAPayload(long caseId, int retry) {
        String payload = (retry == 0) ? String.format("%s", caseId) : String.format("%s,%s", caseId, retry);
        long actualCaseId = ccdActionExecutor.getCaseId(payload);
        long actualRetry = ccdActionExecutor.getRetry(payload);
        assertEquals(caseId, actualCaseId);
        assertEquals(retry, actualRetry);
    }

    @Test
    public void shouldHandlePayloadWhenAlreadyRetriedOnceToSendNotification() {
        wrapper = SscsCaseDataWrapper.builder().state(State.APPEAL_CREATED).newSscsCaseData(newSscsCaseData).notificationEventType(SYA_APPEAL_CREATED_NOTIFICATION).build();
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);
        ccdActionExecutor.execute(JOB_ID, JOB_GROUP, SYA_APPEAL_CREATED_NOTIFICATION.getId(), "123456,1");

        verify(notificationService, times(1)).manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(ccdService, times(0)).updateCase(any(), eq(123456L), any(), any(), any(), any());
    }

    @Test
    @Parameters({"1", "2", "3"})
    public void shouldScheduleToRetryAgainWhenNotificationFails(int retry) {
        wrapper = SscsCaseDataWrapper.builder().state(State.APPEAL_CREATED).newSscsCaseData(newSscsCaseData).notificationEventType(SYA_APPEAL_CREATED_NOTIFICATION).build();
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);
        doThrow(new NotificationServiceException(caseDetails.getId().toString(), new NotificationClientException(new NullPointerException("error")))).when(notificationService).manageNotificationAndSubscription(eq(new CcdNotificationWrapper(wrapper)));
        final String payload = (retry == 0) ? "123456" : "123456," + retry;
        ccdActionExecutor.execute(JOB_ID, JOB_GROUP, SYA_APPEAL_CREATED_NOTIFICATION.getId(), payload);

        verify(retryNotificationService).rescheduleIfHandledGovNotifyErrorStatus(eq(retry + 1), any(), any(NotificationServiceException.class));
        verify(ccdService, times(0)).updateCase(any(), any(), any(), any(), any(), any());
    }

}