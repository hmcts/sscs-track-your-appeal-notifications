package uk.gov.hmcts.reform.sscs.service.scheduler;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_REMINDER_NOTIFICATION;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

public class ActionExecutorTest {

    private ActionExecutor actionExecutor;

    @Mock
    private NotificationService notificationService;
    @Mock
    private IdamService idamService;
    @Mock
    private CcdService ccdService;
    @Mock
    private SscsCaseDataWrapperDeserializer deserializer;

    private CaseDetails caseDetails;
    private SscsCaseDetails sscsCaseDetails;
    private SscsCaseDataWrapper wrapper;
    private SscsCaseData newSscsCaseData;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        initMocks(this);

        actionExecutor = new ActionExecutor(notificationService, ccdService, deserializer, idamService);

        caseDetails = CaseDetails.builder().caseTypeId("123").build();

        newSscsCaseData = SscsCaseData.builder().build();

        wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).notificationEventType(EVIDENCE_REMINDER_NOTIFICATION).build();

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void givenAReminderIsTriggered_thenActionExecutorShouldProcessTheJob() {
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);
        when(deserializer.buildSscsCaseDataWrapper(any())).thenReturn(wrapper);
        when(ccdService.updateCase(eq(newSscsCaseData), eq(123456L), eq(EVIDENCE_REMINDER_NOTIFICATION.getId()), any(), any(), eq(idamTokens))).thenReturn(sscsCaseDetails);

        actionExecutor.execute("1", "group", EVIDENCE_REMINDER_NOTIFICATION.getId(), "123456");

        verify(notificationService, times(1)).createAndSendNotification(new CcdNotificationWrapper(wrapper));
        verify(ccdService, times(1)).updateCase(any(), any(), any(), any(), any(), any());
    }

}