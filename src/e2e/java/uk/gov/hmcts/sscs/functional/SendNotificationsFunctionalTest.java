package uk.gov.hmcts.sscs.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.sscs.CcdResponseUtils.*;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import helper.EnvironmentProfileValueSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;
import uk.gov.hmcts.sscs.service.ccd.CreateCcdService;
import uk.gov.hmcts.sscs.service.ccd.UpdateCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@ProfileValueSourceConfiguration(EnvironmentProfileValueSource.class)
@IfProfileValue(name = "environment.shared-ccd", value = "false")
public class SendNotificationsFunctionalTest {

    // These tests need to fixed to work with the relevant CCD environments

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
        String oauth2Token = idamService.getIdamOauth2Token();
        idamTokens = IdamTokens.builder()
            .idamOauth2Token(oauth2Token)
            .serviceAuthorization(idamService.generateServiceAuthorization())
            .userId(idamService.getUserId(oauth2Token))
            .build();

        caseData = buildCcdResponse("SC068/17/00022", "Yes", "Yes");

        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);

        assertNotNull(caseDetails);
        caseId = caseDetails.getId();
    }

    @Test
    public void shouldSendAppealReceivedNotification() {
        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, APPEAL_RECEIVED.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
    }

    @Test
    public void shouldSendEvidenceReceivedNotification() {
        addEvidence(caseData);
        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, EVIDENCE_RECEIVED.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
    }

    @Test
    public void shouldSendHearingAdjournedNotification() {
        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, ADJOURNED.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
    }

    @Test
    public void shouldSendHearingPostponedNotification() {
        addEventTypeToCase(caseData, POSTPONEMENT);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, POSTPONEMENT.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
    }

    @Test
    public void shouldSendAppealLapsedNotification() {
        addEventTypeToCase(caseData, APPEAL_LAPSED);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, APPEAL_LAPSED.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
    }

    @Test
    public void shouldSendAppealWithdrawnNotification() {
        addEventTypeToCase(caseData, APPEAL_WITHDRAWN);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, APPEAL_WITHDRAWN.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
    }

    @Test
    public void shouldSendHearingBookedNotification() {
        addEventTypeToCase(caseData, HEARING_BOOKED);
        addHearing(caseData);

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, HEARING_BOOKED.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
    }
}
