package uk.gov.hmcts.reform.sscs.service.scheduler;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.EVIDENCE_REMINDER;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.ccd.SearchCcdService;
import uk.gov.hmcts.reform.sscs.service.ccd.UpdateCcdService;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.NotificationService;
import uk.gov.hmcts.reform.sscs.service.ccd.SearchCcdService;
import uk.gov.hmcts.reform.sscs.service.ccd.UpdateCcdService;

public class ActionExecutorTest {

    private ActionExecutor actionExecutor;

    @Mock
    private NotificationService notificationService;
    @Mock
    private SearchCcdService searchCcdService;
    @Mock
    private UpdateCcdService updateCcdService;
    @Mock
    private IdamService idamService;
    @Mock
    private SscsCaseDataWrapperDeserializer deserializer;

    private CaseDetails caseDetails;
    private SscsCaseDataWrapper wrapper;
    private SscsCaseData newSscsCaseData;

    @Before
    public void setup() {
        initMocks(this);

        actionExecutor = new ActionExecutor(notificationService, searchCcdService, updateCcdService, idamService, deserializer);

        caseDetails = CaseDetails.builder().caseTypeId("123").build();

        newSscsCaseData = SscsCaseData.builder().notificationType(EVIDENCE_REMINDER).build();

        wrapper = SscsCaseDataWrapper.builder().newSscsCaseData(newSscsCaseData).build();
    }

    @Test
    public void givenAReminderIsTriggered_thenActionExecutorShouldProcessTheJob() {
        when(searchCcdService.getByCaseId(eq("123456"), any())).thenReturn(caseDetails);
        when(deserializer.buildSscsCaseDataWrapper(any())).thenReturn(wrapper);
        when(updateCcdService.update(eq(newSscsCaseData), eq(123456L), eq(EVIDENCE_REMINDER.getCcdType()), any())).thenReturn(caseDetails);

        actionExecutor.execute("1", "group", EVIDENCE_REMINDER.getCcdType(), "123456");

        verify(notificationService, times(1)).createAndSendNotification(new CcdNotificationWrapper(wrapper));
        verify(updateCcdService, times(1)).update(any(), any(), any(), any());
    }

}