package uk.gov.hmcts.reform.sscs.functional.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;

import java.util.List;
import java.util.concurrent.TimeUnit;
import junitparams.Parameters;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;


public class AdminAppealWithdrawnNotificationsTest extends AbstractFunctionalTest {

    public AdminAppealWithdrawnNotificationsTest() {
        super(3);
    }

    @Test
    @Parameters({"Appellant", "Appointee"})
    public void givenCallbackWithAppellantSubscription_shouldSendEmailSmsAndLetterNotifications(String party) throws Exception {
        simulateCcdCallback(ADMIN_APPEAL_WITHDRAWN, "handlers/" + ADMIN_APPEAL_WITHDRAWN.getId() + party + "Callback.json");

        delayAssertionInSeconds();


        String emailId = "8620e023-f663-477e-a771-9cfad50ee30f";
        String smsId = "446c7b23-7342-42e1-adff-b4c367e951cb";
        List<Notification> notifications = tryFetchNotificationsForTestCase(emailId, smsId);

        assertNotificationBodyContains(notifications, emailId);
        assertNotificationBodyContains(notifications, smsId);
        assertEquals(1, getNumberOfLetterCorrespondence());
    }

    @Test
    public void givenCallbackWithNoSubscription_shouldSendLetterNotifications() throws Exception {
        simulateCcdCallback(ADMIN_APPEAL_WITHDRAWN, "handlers/" + ADMIN_APPEAL_WITHDRAWN.getId()
            + "NoSubscriptions" + "Callback.json");

        delayAssertionInSeconds();


        String emailId = "8620e023-f663-477e-a771-9cfad50ee30f";
        String smsId = "446c7b23-7342-42e1-adff-b4c367e951cb";
        List<Notification> notifications = tryFetchNotificationsForTestCaseWithFlag(true, emailId, smsId);

        assertTrue(notifications.isEmpty());
        assertEquals(1, getNumberOfLetterCorrespondence());
    }

    private void delayAssertionInSeconds() {
        try {
            TimeUnit.SECONDS.sleep(60);
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
