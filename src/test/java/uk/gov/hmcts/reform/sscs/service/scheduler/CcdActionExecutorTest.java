package uk.gov.hmcts.reform.sscs.service.scheduler;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_REMINDER_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SYA_APPEAL_CREATED_NOTIFICATION;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

public class CcdActionExecutorTest {

    private CcdActionExecutor ccdActionExecutor;

    @Mock
    private NotificationService notificationService;
    @Mock
    private IdamService idamService;
    @Mock
    private CcdService ccdService;

    private SscsCaseCallbackDeserializer deserializer;
    private SscsCaseData newSscsCaseData;
    private SscsCaseDetails caseDetails;
    private SscsCaseDataWrapper wrapper;

    private IdamTokens idamTokens;

    private ObjectMapper mapper;

    @Before
    public void setup() {
        initMocks(this);

        Jackson2ObjectMapperBuilder objectMapperBuilder =
                new Jackson2ObjectMapperBuilder()
                        .featuresToEnable(READ_ENUMS_USING_TO_STRING)
                        .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                        .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
                        .serializationInclusion(JsonInclude.Include.NON_ABSENT);

        mapper = objectMapperBuilder.createXmlMapper(false).build();
        mapper.registerModule(new JavaTimeModule());

        deserializer = new SscsCaseCallbackDeserializer(mapper);

        ccdActionExecutor = new CcdActionExecutor(notificationService, ccdService, idamService, deserializer);

        caseDetails = SscsCaseDetails.builder().id(456L).caseTypeId("123").state("appealCreated").build();

        newSscsCaseData = SscsCaseData.builder().ccdCaseId("456").build();
        caseDetails.setData(newSscsCaseData);

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void givenAReminderIsTriggered_thenActionExecutorShouldProcessTheJob() {
        wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).notificationEventType(EVIDENCE_REMINDER_NOTIFICATION).build();
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);

        ccdActionExecutor.execute("1", "group", EVIDENCE_REMINDER_NOTIFICATION.getId(), "123456");

        verify(notificationService).manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(ccdService).updateCase(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void givenAReminderIsTriggeredAndNotificationIsNotAReminderType_thenActionExecutorShouldProcessTheJobButNotWriteBackToCcd() {
        wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).notificationEventType(SYA_APPEAL_CREATED_NOTIFICATION).build();
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);

        ccdActionExecutor.execute("1", "group", SYA_APPEAL_CREATED_NOTIFICATION.getId(), "123456");

        verify(notificationService, times(1)).manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(ccdService, times(0)).updateCase(any(), any(), any(), any(), any(), any());
    }

}