package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;

import java.io.IOException;

public class AppealReceivedLetterFunctionalTest extends AbstractFunctionalTest {

    public AppealReceivedLetterFunctionalTest() {
        super(30);
    }

    @Test
    public void sendsAppealReceivedLetterToAppellant() throws IOException {

        NotificationEventType notificationEventType = NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;

        simulateCcdCallback(notificationEventType,
                "appellant-" + notificationEventType.getId() + "Callback.json");


    }

}
