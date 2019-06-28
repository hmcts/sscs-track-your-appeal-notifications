package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import java.io.IOException;
import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.hmcts.reform.sscs.service.NotificationSender;


@RunWith(JUnitParamsRunner.class)
public class AppealReceivedLetterFunctionalTest extends AbstractFunctionalTest {

    public AppealReceivedLetterFunctionalTest() {
        super(30);
    }

    @Autowired
    private NotificationSender notificationSender;

    @Test
    public void sendsAppealReceivedLetterToAppellant() throws IOException {

        NotificationEventType notificationEventType = NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;

        simulateCcdCallback(notificationEventType,
                "appellant-" + notificationEventType.getId() + "Callback.json");

    }

}
