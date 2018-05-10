package uk.gov.hmcts.sscs.functional;

import static org.junit.Assert.*;
import static uk.gov.hmcts.sscs.CcdResponseUtils.*;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;
import uk.gov.hmcts.sscs.service.ccd.CreateCcdService;
import uk.gov.hmcts.sscs.service.ccd.SearchCcdService;
import uk.gov.hmcts.sscs.service.ccd.UpdateCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SendNotificationsFunctionalTest {

    @Autowired
    private CreateCcdService createCcdService;
    @Autowired
    private UpdateCcdService updateCcdService;

    @Autowired
    private IdamService idamService;

    private CcdResponse caseData;
    private IdamTokens idamTokens;
    private Long caseId;

    @Before
    public void setup() {
        idamTokens = IdamTokens.builder()
                .authenticationService(idamService.generateServiceAuthorization())
                .idamOauth2Token(idamService.getIdamOauth2Token())
                .build();

        caseData = buildCcdResponse("SC068/17/00022", "Yes", "Yes");

        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);

        assertNotNull(caseDetails);
        assertEquals("COMPLETED", caseDetails.getCallbackResponseStatus());
        caseId = caseDetails.getId();
    }

    @Test
    public void shouldSendAppealReceivedNotification() {
        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, APPEAL_RECEIVED.getId(), idamTokens);

        //FIXME: Bug in CCD tracked in ticket RDM-2189 which returns null for a success when it is an update. Should be COMPLETED. Fix for all tests
        assertNull(updatedCaseDetails.getCallbackResponseStatus());
    }

    @Test
    public void shouldSendResponseReceivedNotification() {
        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, DWP_RESPONSE_RECEIVED.getId(), idamTokens);

        assertNull(updatedCaseDetails.getCallbackResponseStatus());
    }

//    @Test
//    public void shouldSendEvidenceReceivedNotification() {
//        //FIXME: Think need to add correct evidence structure
//        addEvidence(caseData);
//        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, EVIDENCE_RECEIVED.getId(), idamTokens);
//
//        assertNull(updatedCaseDetails.getCallbackResponseStatus());
//    }

//    addEventTypeToCase(caseData, APPEAL_WITHDRAWN);


    @Test
    public void shouldSendHearingAdjournedNotification() {
        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, ADJOURNED.getId(), idamTokens);

        assertNull(updatedCaseDetails.getCallbackResponseStatus());
    }

    @Test
    public void shouldSendHearingPostponedNotification() {
        addEventTypeToCase(caseData, POSTPONEMENT);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, POSTPONEMENT.getId(), idamTokens);

        assertNull(updatedCaseDetails.getCallbackResponseStatus());
    }

    @Test
    public void shouldSendAppealLapsedNotification() {
        addEventTypeToCase(caseData, APPEAL_LAPSED);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, APPEAL_LAPSED.getId(), idamTokens);

        assertNull(updatedCaseDetails.getCallbackResponseStatus());
    }

    @Test
    public void shouldSendAppealWithdrawnNotification() {
        addEventTypeToCase(caseData, APPEAL_WITHDRAWN);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, APPEAL_WITHDRAWN.getId(), idamTokens);

        assertNull(updatedCaseDetails.getCallbackResponseStatus());
    }

    @Test
    public void shouldSendHearingBookedNotification() {
        addEventTypeToCase(caseData, HEARING_BOOKED);
        addHearing(caseData);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, HEARING_BOOKED.getId(), idamTokens);

        assertNull(updatedCaseDetails.getCallbackResponseStatus());
    }
}
