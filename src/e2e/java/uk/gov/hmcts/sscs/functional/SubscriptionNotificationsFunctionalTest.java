package uk.gov.hmcts.sscs.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.sscs.CcdResponseUtils.buildCcdResponse;
import static uk.gov.hmcts.sscs.domain.notify.EventType.SUBSCRIPTION_UPDATED;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Subscriptions;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;
import uk.gov.hmcts.sscs.service.ccd.CreateCcdService;
import uk.gov.hmcts.sscs.service.ccd.UpdateCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SubscriptionNotificationsFunctionalTest {

    @Autowired
    private CreateCcdService createCcdService;
    @Autowired
    private UpdateCcdService updateCcdService;

    @Autowired
    private IdamService idamService;

    private CcdResponse caseData;
    private IdamTokens idamTokens;
    private Long caseId;

    public void createCase(String caseRef, String subscribeEmail, String subscribeSms) {
        idamTokens = IdamTokens.builder()
                .authenticationService(idamService.generateServiceAuthorization())
                .idamOauth2Token(idamService.getIdamOauth2Token())
                .build();

        caseData = buildCcdResponse(caseRef, subscribeEmail, subscribeSms);

        CaseDetails caseDetails = createCcdService.create(caseData, idamTokens);

        assertNotNull(caseDetails);
        caseId = caseDetails.getId();
    }

    @Test
    public void shouldSendSubscriptionCreatedNotification() {
        createCase("SC068/17/00023", "No", "No");

        Subscriptions subscriptions = caseData.getSubscriptions();
        subscriptions.getAppellantSubscription().setSubscribeEmail("Yes");
        subscriptions.getAppellantSubscription().setSubscribeSms("Yes");

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, SUBSCRIPTION_UPDATED.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
    }

    @Test
    public void shouldSendSubscriptionUpdatedNotification() {
        createCase("SC068/17/00024", "Yes", "Yes");

        Subscriptions subscriptions = caseData.getSubscriptions();
        subscriptions.getAppellantSubscription().setEmail("sscstest+notify2@greencroftconsulting.com");

        CaseDetails updatedCaseDetails = updateCcdService.update(caseData, caseId, SUBSCRIPTION_UPDATED.getId(), idamTokens);

        assertEquals("COMPLETED", updatedCaseDetails.getCallbackResponseStatus());
    }
}
