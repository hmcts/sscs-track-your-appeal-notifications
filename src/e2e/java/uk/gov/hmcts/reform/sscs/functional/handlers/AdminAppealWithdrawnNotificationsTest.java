package uk.gov.hmcts.reform.sscs.functional.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.CASE_UPDATED;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.hmcts.reform.sscs.functional.Retry;
import uk.gov.service.notify.Notification;


public class AdminAppealWithdrawnNotificationsTest extends AbstractFunctionalTest {

    public AdminAppealWithdrawnNotificationsTest() {
        super(30);
    }

    @Rule
    public Retry retry = new Retry(0);

    //Test method runs three times and in worst case it needs 90 seconds waiting time.
    @Rule
    public Timeout globalTimeout = Timeout.seconds(100);

    @Before
    public void setUp() {
        initialiseCcdCase();
    }

    @Test
    @Parameters({
        "Appellant, 8620e023-f663-477e-a771-9cfad50ee30f, 446c7b23-7342-42e1-adff-b4c367e951cb, 1, 1, 1",
        "Appointee, 8620e023-f663-477e-a771-9cfad50ee30f, 446c7b23-7342-42e1-adff-b4c367e951cb, 1, 1, 1",
        "Reps, e29a2275-553f-4e70-97f4-2994c095f281, f59440ee-19ca-4d47-a702-13e9cecaccbd, 1, 1, 2"
    })
    public void givenCallbackWithSubscriptions_shouldSendEmailSmsAndLetterNotifications(
        String subscription,
        String emailId,
        String smsId,
        int expectedNumEmailNotifications,
        int expectedNumSmsNotifications,
        int expectedNumLetters) throws Exception {

        simulateCcdCallback(ADMIN_APPEAL_WITHDRAWN, "handlers/" + ADMIN_APPEAL_WITHDRAWN.getId() + subscription
            + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(emailId, smsId);

        assertEquals(expectedNumEmailNotifications, getNumberOfNotificationsForGivenEmailOrSmsTemplateId(notifications, emailId));
        assertEquals(expectedNumSmsNotifications, getNumberOfNotificationsForGivenEmailOrSmsTemplateId(notifications, smsId));
        assertTrue(fetchLetters(expectedNumLetters, subscription));
    }

    private boolean fetchLetters(int expectedNumLetters, String subscription) {
        do {
            if (getNumberOfLetterCorrespondence(subscription) == expectedNumLetters) {
                return true;
            }
            delayInSeconds(5);
        } while (true);
    }

    private void initialiseCcdCase() {
        caseData.setCorrespondence(null);
        caseData.setSubscriptions(null);
        ccdService.updateCase(caseData, caseId, CASE_UPDATED.getId(), "create by Test",
            "Notification Service updated case", idamTokens);
    }

    private long getNumberOfNotificationsForGivenEmailOrSmsTemplateId(List<Notification> notifications, String emailId) {
        return notifications.stream()
            .filter(notification -> notification.getTemplateId().equals(UUID.fromString(emailId)))
            .count();
    }

    private long getNumberOfLetterCorrespondence(String subscription) {
        List<Correspondence> correspondence = ccdService
            .getByCaseId(caseId, idamTokens).getData().getCorrespondence();
        if (correspondence == null) {
            return 0;
        }
        return correspondence.stream()
            .filter(c -> c.getValue().getCorrespondenceType() == CorrespondenceType.Letter)
            .filter(c -> c.getValue().getDocumentLink().getDocumentFilename().contains(ADMIN_APPEAL_WITHDRAWN.getId()))
            .filter(c -> isAMatch(subscription, c.getValue().getTo()))
            .count();
    }

    private boolean isAMatch(String subscription, String text) {
        Pattern pattern = Pattern.compile(setRegExpBasedOnSubscription(subscription), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    private String setRegExpBasedOnSubscription(String subscription) {
        String regexp = subscription;
        if ("Reps".equals(subscription)) {
            regexp = "(\\bAppellant\\b|\\bReps\\b)";
        }
        return regexp;
    }

}
