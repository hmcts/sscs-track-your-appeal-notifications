package uk.gov.hmcts.sscs.service.scheduler;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.domain.notify.EventType.EVIDENCE_REMINDER;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.deserialize.CcdResponseWrapperDeserializer;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.sscs.service.NotificationService;
import uk.gov.hmcts.sscs.service.ccd.SearchCcdService;
import uk.gov.hmcts.sscs.service.ccd.UpdateCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamService;

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
    private CcdResponseWrapperDeserializer deserializer;

    private CaseDetails caseDetails;
    private CcdResponseWrapper wrapper;
    private CcdResponse newCcdResponse;

    @Before
    public void setup() {
        initMocks(this);

        actionExecutor = new ActionExecutor(notificationService, searchCcdService, updateCcdService, idamService, deserializer);

        caseDetails = CaseDetails.builder().caseTypeId("123").build();

        newCcdResponse = CcdResponse.builder().notificationType(EVIDENCE_REMINDER).build();

        wrapper = CcdResponseWrapper.builder().newCcdResponse(newCcdResponse).build();
    }

    @Test
    public void givenAReminderIsTriggered_thenActionExecutorShouldProcessTheJob() {
        when(searchCcdService.getByCaseId(eq("123456"), any())).thenReturn(caseDetails);
        when(deserializer.buildCcdResponseWrapper(any())).thenReturn(wrapper);
        when(updateCcdService.update(eq(newCcdResponse), eq(123456L), eq(EVIDENCE_REMINDER.getId()), any())).thenReturn(caseDetails);

        actionExecutor.execute("1", "group", EVIDENCE_REMINDER.getId(), "123456");

        verify(notificationService, times(1)).createAndSendNotification(new CcdNotificationWrapper(wrapper));
        verify(updateCcdService, times(1)).update(any(), any(), any(), any());
    }

}