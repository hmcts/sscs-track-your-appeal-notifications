package uk.gov.hmcts.sscs.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.CcdResponseUtils;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.models.idam.IdamTokens;
import uk.gov.hmcts.sscs.service.ccd.CreateCcdService;
import uk.gov.hmcts.sscs.service.ccd.UpdateCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("development")
public class SaveAndUpdateSimpleCaseInCcd {

    @Autowired
    private CreateCcdService createCcdService;
    @Autowired
    private UpdateCcdService updateCcdService;
    @Autowired
    private IdamService idamService;

    @Test
    public void shouldBeSavedAndThenUpdatedIntoCcdGivenACase() {
        CcdResponse caseData = CcdResponseUtils.buildCcdResponse("SC068/17/00013");
        IdamTokens idamTokens = IdamTokens.builder()
            .authenticationService(idamService.generateServiceAuthorization())
            .idamOauth2Token(idamService.getIdamOauth2Token())
            .build();
        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);
        assertNotNull(caseDetails);
        CcdResponse updatedCaseData = CcdResponseUtils.buildCcdResponse("SC123/12/78765");
        CaseDetails updatedCaseDetails = updateCcdService.update(updatedCaseData, caseDetails.getId(),
            "appealReceived", idamTokens);
        assertEquals("SC123/12/78765", updatedCaseDetails.getData().get("caseReference"));
    }

}
