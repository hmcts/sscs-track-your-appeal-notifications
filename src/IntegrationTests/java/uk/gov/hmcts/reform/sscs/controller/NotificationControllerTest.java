package uk.gov.hmcts.reform.sscs.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.CohEvent;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

@ActiveProfiles("integration")
public class NotificationControllerTest {

    private NotificationController notificationController;

    private SscsCaseDataWrapper sscsCaseDataWrapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuthorisationService authorisationService;

    @Mock
    private CcdClient ccdClient;

    @Mock
    private IdamService idamService;

    @Mock
    private SscsCaseDataWrapperDeserializer deserializer;

    @Before
    public void setUp() {
        initMocks(this);

        notificationController = new NotificationController(notificationService, authorisationService, ccdClient, deserializer);
        sscsCaseDataWrapper = SscsCaseDataWrapper.builder().newSscsCaseData(SscsCaseData.builder().build()).oldSscsCaseData(SscsCaseData.builder().build()).build();
    }

    @Test
    public void shouldCreateAndSendNotificationForSscsCaseData() {
        notificationController.sendNotification("", sscsCaseDataWrapper);
        verify(notificationService).createAndSendNotification(new CcdNotificationWrapper(sscsCaseDataWrapper));
    }

    @Test
    public void shouldCreateAndSendNotificationForCohResponse() {
        String caseId = "caseId";
        String eventType = "eventType";
        long caseDetailsId = 123L;
        String onlineHearingId = "onlineHearingId";

        CaseDetails caseDetails = CaseDetails.builder().id(caseDetailsId).build();
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(ccdClient.getByCaseId(caseId)).thenReturn(caseDetails);
        SscsCaseDataWrapper sscsCaseDataWrapper = SscsCaseDataWrapper.builder().build();
        when(deserializer.buildSscsCaseDataWrapper(hasFields(eventType, caseDetailsId))).thenReturn(sscsCaseDataWrapper);

        CohEvent cohEvent = CohEvent.builder().caseId(caseId).onlineHearingId(onlineHearingId).eventType(eventType).build();
        notificationController.sendCohNotification("", cohEvent);

        verify(notificationService).createAndSendNotification(argThat(argument ->
                argument instanceof CohNotificationWrapper
                        && ((CohNotificationWrapper) argument).getOnlineHearingId().equals(onlineHearingId)
                        && argument.getSscsCaseDataWrapper().equals(sscsCaseDataWrapper)));
    }

    @Test
    public void handlesNotFindingCcdDetails() {
        String caseId = "caseId";
        String eventType = "eventType";

        when(ccdClient.getByCaseId(eq(caseId))).thenReturn(null);

        notificationController.sendCohNotification("", CohEvent.builder().caseId(caseId).eventType(eventType).build());

        verifyZeroInteractions(notificationService);
    }

    private JsonNode hasFields(String eventType, long caseDetailsId) {
        return argThat(argument -> {
            return argument.get("event_id").textValue().equals(eventType)
                    && argument.get("case_details").get("id").longValue() == caseDetailsId;
        });
    }
}