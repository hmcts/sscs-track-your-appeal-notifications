package uk.gov.hmcts.sscs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.deserialize.CcdResponseWrapperDeserializer;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.CohEvent;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;
import uk.gov.hmcts.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.sscs.service.AuthorisationService;
import uk.gov.hmcts.sscs.service.NotificationService;
import uk.gov.hmcts.sscs.service.ccd.SearchCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamTokensService;

@ActiveProfiles("integration")
public class NotificationControllerTest {

    private NotificationController notificationController;

    private CcdResponseWrapper ccdResponseWrapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuthorisationService authorisationService;

    @Mock
    private SearchCcdService searchCcdService;

    @Mock
    private IdamTokensService idamTokensService;

    @Mock
    private CcdResponseWrapperDeserializer deserializer;

    @Before
    public void setUp() {
        initMocks(this);

        notificationController = new NotificationController(notificationService, authorisationService, searchCcdService, idamTokensService, deserializer);
        ccdResponseWrapper = CcdResponseWrapper.builder().newCcdResponse(CcdResponse.builder().build()).oldCcdResponse(CcdResponse.builder().build()).build();
    }

    @Test
    public void shouldCreateAndSendNotificationForCcdResponse() {
        notificationController.sendNotification("", ccdResponseWrapper);
        verify(notificationService).createAndSendNotification(new CcdNotificationWrapper(ccdResponseWrapper));
    }

    @Test
    public void shouldCreateAndSendNotificationForCohResponse() {
        String caseId = "caseId";
        String eventType = "eventType";
        long caseDetailsId = 123L;
        String onlineHearingId = "onlineHearingId";

        CaseDetails caseDetails = CaseDetails.builder().id(caseDetailsId).build();
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamTokensService.getIdamTokens()).thenReturn(idamTokens);
        when(searchCcdService.getByCaseId(caseId, idamTokens)).thenReturn(caseDetails);
        CcdResponseWrapper ccdResponseWrapper = CcdResponseWrapper.builder().build();
        when(deserializer.buildCcdResponseWrapper(hasFields(eventType, caseDetailsId))).thenReturn(ccdResponseWrapper);

        CohEvent cohEvent = CohEvent.builder().caseId(caseId).onlineHearingId(onlineHearingId).eventType(eventType).build();
        notificationController.sendCohNotification("", cohEvent);

        verify(notificationService).createAndSendNotification(argThat(argument ->
                argument instanceof CohNotificationWrapper
                        && ((CohNotificationWrapper) argument).getOnlineHearingId().equals(onlineHearingId)
                        && argument.getCcdResponseWrapper().equals(ccdResponseWrapper)));
    }

    @Test
    public void handlesNotFindingCcdDetails() {
        String caseId = "caseId";
        String eventType = "eventType";

        when(searchCcdService.getByCaseId(eq(caseId), any(IdamTokens.class))).thenReturn(null);

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