package uk.gov.hmcts.reform.sscs.service.scheduler;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.EVIDENCE_REMINDER;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

public class ActionExecutorTest {

    private ActionExecutor actionExecutor;

    @Mock
    private NotificationService notificationService;
    @Mock
    private CcdClient ccdClient;
    @MockBean
    private CcdRequestDetails ccdRequestDetails;
    @Mock
    private SscsCaseDataWrapperDeserializer deserializer;

    private CaseDetails caseDetails;
    private SscsCaseDetails sscsCaseDetails;
    private SscsCaseDataWrapper wrapper;
    private SscsCaseData newSscsCaseData;

    @Before
    public void setup() {
        initMocks(this);

        actionExecutor = new ActionExecutor(notificationService, ccdClient, deserializer);

        caseDetails = CaseDetails.builder().caseTypeId("123").build();

        newSscsCaseData = SscsCaseData.builder().notificationType(EVIDENCE_REMINDER).build();

        wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).build();
    }

    @Test
    public void givenAReminderIsTriggered_thenActionExecutorShouldProcessTheJob() {
        when(ccdClient.getByCaseId(eq("123456"))).thenReturn(caseDetails);
        when(deserializer.buildSscsCaseDataWrapper(any())).thenReturn(wrapper);
        when(ccdClient.updateCase(eq(newSscsCaseData), eq(123456L), eq(EVIDENCE_REMINDER.getCcdType()), any(), any())).thenReturn(sscsCaseDetails);

        actionExecutor.execute("1", "group", EVIDENCE_REMINDER.getCcdType(), "123456");

        verify(notificationService, times(1)).createAndSendNotification(new CcdNotificationWrapper(wrapper));
        verify(ccdClient, times(1)).updateCase(any(), any(), any(), any(), any());
    }

}