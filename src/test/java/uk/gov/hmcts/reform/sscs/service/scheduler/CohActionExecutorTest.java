package uk.gov.hmcts.reform.sscs.service.scheduler;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_REMINDER_NOTIFICATION;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

public class CohActionExecutorTest {

    private CohActionExecutor cohActionExecutor;

    @Mock
    private NotificationService notificationService;
    @Mock
    private IdamService idamService;
    @Mock
    private CcdService ccdService;
    @Mock
    private SscsCaseDataWrapperDeserializer deserializer;
    @Mock
    private Environment environment;

    private SscsCaseDetails caseDetails;
    private SscsCaseDataWrapper wrapper;
    private SscsCaseData newSscsCaseData;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        initMocks(this);

        cohActionExecutor = new CohActionExecutor(notificationService, ccdService, deserializer, idamService, environment);

        caseDetails = SscsCaseDetails.builder().caseTypeId("123").build();

        newSscsCaseData = SscsCaseData.builder().build();

        wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).notificationEventType(EVIDENCE_REMINDER_NOTIFICATION).build();

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void givenAReminderIsTriggered_thenActionExecutorShouldProcessTheJobOnProductionSlot() {
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);
        when(deserializer.buildSscsCaseDataWrapper(any())).thenReturn(wrapper);
        when(environment.getProperty("infrastructure.env.name")).thenReturn("PROD");
        when(environment.getProperty("slot.name")).thenReturn("PRODUCTION");

        String onlineHearingId = UUID.randomUUID().toString();
        cohActionExecutor.execute("1", "group", EVIDENCE_REMINDER_NOTIFICATION.getId(), new CohJobPayload(123456L, onlineHearingId));

        verify(notificationService, times(1)).createAndSendNotification(new CohNotificationWrapper(onlineHearingId, wrapper));
    }

    @Test
    public void givenAReminderIsTriggered_thenActionExecutorShouldNotProcessTheJobOnStagingSlot() {
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);
        when(deserializer.buildSscsCaseDataWrapper(any())).thenReturn(wrapper);
        when(environment.getProperty("infrastructure.env.name")).thenReturn("PROD");
        when(environment.getProperty("slot.name")).thenReturn("STAGING");

        String onlineHearingId = UUID.randomUUID().toString();
        cohActionExecutor.execute("1", "group", EVIDENCE_REMINDER_NOTIFICATION.getId(), new CohJobPayload(123456L, onlineHearingId));

        verify(notificationService, times(0)).createAndSendNotification(new CohNotificationWrapper(onlineHearingId, wrapper));
    }
}