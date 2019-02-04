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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
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

    private static final String CASE_ID = "12345";
    private static final String eventType = "question_round_issued";

    private NotificationController notificationController;

    private SscsCaseDataWrapper sscsCaseDataWrapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuthorisationService authorisationService;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @Mock
    private SscsCaseDataWrapperDeserializer deserializer;

    private IdamTokens idamTokens;

    @Before
    public void setUp() {
        initMocks(this);

        notificationController = new NotificationController(notificationService, authorisationService, ccdService, deserializer, idamService);
        sscsCaseDataWrapper = SscsCaseDataWrapper.builder().newSscsCaseData(SscsCaseData.builder().build()).oldSscsCaseData(SscsCaseData.builder().build()).build();

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void shouldCreateAndSendNotificationForSscsCaseData() {
        notificationController.sendNotification("", sscsCaseDataWrapper);
        verify(notificationService).manageNotificationAndSubscription(new CcdNotificationWrapper(sscsCaseDataWrapper));
    }

    @Test
    public void shouldCreateAndSendNotificationForCohResponse() {

        long caseDetailsId = 123L;
        String onlineHearingId = "onlineHearingId";

        SscsCaseDetails caseDetails = SscsCaseDetails.builder().id(caseDetailsId).build();

        when(ccdService.getByCaseId(Long.valueOf(CASE_ID), idamTokens)).thenReturn(caseDetails);
        SscsCaseDataWrapper sscsCaseDataWrapper = SscsCaseDataWrapper.builder().build();
        when(deserializer.buildSscsCaseDataWrapper(hasFields(eventType, caseDetailsId))).thenReturn(sscsCaseDataWrapper);

        CohEvent cohEvent = CohEvent.builder().caseId(CASE_ID).onlineHearingId(onlineHearingId).eventType(eventType).build();
        notificationController.sendCohNotification("", cohEvent);

        verify(notificationService).manageNotificationAndSubscription(argThat(argument ->
                argument instanceof CohNotificationWrapper
                        && ((CohNotificationWrapper) argument).getOnlineHearingId().equals(onlineHearingId)
                        && argument.getSscsCaseDataWrapper().equals(sscsCaseDataWrapper)));
    }

    @Test
    public void handlesNotFindingCcdDetails() {
        when(ccdService.getByCaseId(eq(Long.valueOf(CASE_ID)), eq(idamTokens))).thenReturn(null);

        notificationController.sendCohNotification("", CohEvent.builder().caseId(CASE_ID).eventType(eventType).build());

        verifyZeroInteractions(notificationService);
    }

    private JsonNode hasFields(String eventType, long caseDetailsId) {
        return argThat(argument -> {
            return argument.get("event_id").textValue().equals(eventType)
                    && argument.get("case_details").get("id").longValue() == caseDetailsId;
        });
    }
}