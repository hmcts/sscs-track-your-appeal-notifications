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
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.service.ccd.CreateCcdService;
import uk.gov.hmcts.sscs.service.ccd.UpdateCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamService;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReminderNotificationsFunctionalTest {

    @Autowired
    private CreateCcdService createCcdService;
    @Autowired
    private UpdateCcdService updateCcdService;

    @Autowired
    private IdamService idamService;

    private CcdResponse caseData;
    private IdamTokens idamTokens;
    private Long caseId;

    @Autowired
    private NotificationClient client;

    @Before
    public void setup() {
        idamTokens = IdamTokens.builder()
                .authenticationService(idamService.generateServiceAuthorization())
                .idamOauth2Token(idamService.getIdamOauth2Token())
                .build();

        caseData = buildCcdResponse("SC068/17/00022", "Yes", "Yes", EventType.DWP_RESPONSE_RECEIVED);

        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);

        assertNotNull(caseDetails);
        caseId = caseDetails.getId();
    }

    @Test
    public void shouldSendResponseReceivedNotification() throws NotificationClientException {
        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, DWP_RESPONSE_RECEIVED.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // noop
        }

        assertEquals(1, client.getReceivedTextMessages("").getReceivedTextMessages().size());
    }

}
