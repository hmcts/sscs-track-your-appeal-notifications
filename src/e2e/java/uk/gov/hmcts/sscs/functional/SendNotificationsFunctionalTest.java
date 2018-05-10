package uk.gov.hmcts.sscs.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.sscs.domain.notify.EventType.APPEAL_RECEIVED;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.CcdResponseUtils;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;
import uk.gov.hmcts.sscs.service.ccd.CreateCcdService;
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

        caseData = CcdResponseUtils.buildCcdResponse("SC068/17/00022");

        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);

        assertNotNull(caseDetails);
        assertEquals("COMPLETED", caseDetails.getCallbackResponseStatus());
        caseId = caseDetails.getId();
    }

    @Test
    public void shouldSendAppealReceivedNotification() {
        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, APPEAL_RECEIVED.getId(), idamTokens);

        //FIXME: Bug in CCD tracked in ticket RDM-2189 which returns null for a success when it is an update. Should be COMPLETED
        assertNull(updatedCaseDetails.getCallbackResponseStatus());
    }
}
