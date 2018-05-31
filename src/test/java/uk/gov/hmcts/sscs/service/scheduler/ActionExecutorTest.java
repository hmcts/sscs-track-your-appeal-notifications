package uk.gov.hmcts.sscs.service.scheduler;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.deserialize.CcdResponseWrapperDeserializer;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.service.NotificationService;
import uk.gov.hmcts.sscs.service.ccd.SearchCcdService;
import uk.gov.hmcts.sscs.service.ccd.UpdateCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamService;

public class ActionExecutorTest {

    //TODO: Implement this

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

    @Before
    public void setup() {
        initMocks(this);

        actionExecutor = new ActionExecutor(notificationService, searchCcdService, updateCcdService, idamService, deserializer);

        caseDetails = CaseDetails.builder().caseTypeId("123").build();

        wrapper = null;
    }

    @Test
    public void givenAReminderIsTriggered_thenActionExecutorShouldProcessTheJob() {
        actionExecutor.execute("1", "jobName", "123456");
//
        when(searchCcdService.getByCaseId(eq("123456"), any())).thenReturn(caseDetails);
        when(deserializer.buildCcdResponseWrapper(any())).thenReturn(wrapper);

        verify(notificationService, times(1)).createAndSendNotification(wrapper);
        verify(updateCcdService, times(1)).update(any(), any(), any(), any());
    }

}