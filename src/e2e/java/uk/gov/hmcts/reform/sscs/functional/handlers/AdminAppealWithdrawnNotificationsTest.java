package uk.gov.hmcts.reform.sscs.functional.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.CASE_UPDATED;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.hmcts.reform.sscs.functional.Retry;
import uk.gov.service.notify.Notification;


public class AdminAppealWithdrawnNotificationsTest extends AbstractFunctionalTest {

    public AdminAppealWithdrawnNotificationsTest() {
        super(3);
    }

    @Rule
    public Retry retry = new Retry(0);

    @Before
    public void setUp() {
        delayInSeconds(10); //Allow some time to finish potential callbacks as part of the AppealCreated event
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

        delayInSeconds(60); //Allow some time to ensure callback and notifications have finished
        List<Notification> notifications = getClient()
            .getNotifications("", "", getCaseReference(), "").getNotifications();

        assertEquals(expectedNumEmailNotifications, getNumberOfNotificationsForGivenEmailOrSmsTemplateId(notifications, emailId));
        assertEquals(expectedNumSmsNotifications, getNumberOfNotificationsForGivenEmailOrSmsTemplateId(notifications, smsId));
        assertEquals(expectedNumLetters, getNumberOfLetterCorrespondence());
    }

    @Test
    public void givenCallbackWithNoSubscription_shouldSendLetterNotifications() throws Exception {
        simulateCcdCallback(ADMIN_APPEAL_WITHDRAWN, "handlers/" + ADMIN_APPEAL_WITHDRAWN.getId()
            + "NoSubscriptions" + "Callback.json");

        delayInSeconds(60); //Allow some time to ensure callback and notifications have finished
        List<Notification> notifications = getClient()
            .getNotifications("", "", getCaseReference(), "").getNotifications();

        assertTrue(notifications.isEmpty());
        assertEquals(1, getNumberOfLetterCorrespondence());
    }

    private void initialiseCcdCase() {
        getCaseData().setCorrespondence(null);
        getCaseData().setSubscriptions(null);
        getCcdService().updateCase(getCaseData(), getCaseId(), CASE_UPDATED.getId(), "create by Test",
            "Notification Service updated case", getIdamTokens());
    }

    private long getNumberOfNotificationsForGivenEmailOrSmsTemplateId(List<Notification> notifications, String emailId) {
        return notifications.stream()
            .filter(notification -> notification.getTemplateId().equals(UUID.fromString(emailId)))
            .count();
    }

    private void delayInSeconds(int timeout) {
        try {
            TimeUnit.SECONDS.sleep(timeout);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private long getNumberOfLetterCorrespondence() {
        return getCcdService().getByCaseId(getCaseId(), getIdamTokens()).getData().getCorrespondence().stream()
            .filter(c -> c.getValue().getCorrespondenceType() == CorrespondenceType.Letter)
            .count();
    }
}
