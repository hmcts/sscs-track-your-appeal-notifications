package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public class DocmosisWithGovNotifyLetterFunctionalTest extends AbstractFunctionalTest {

    public DocmosisWithGovNotifyLetterFunctionalTest() {
        super(30);
    }

    @Test
    public void sendsAppealReceivedLetterToAppellantAndRepresentative() throws IOException, NotificationClientException {

        NotificationEventType notificationEventType = NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;

        simulateCcdCallback(notificationEventType,
                 notificationEventType.getId() + "Callback.json");

        List<Notification> notifications = fetchLetters();

        assertEquals(2, notifications.size());
        assertEquals("Pre-compiled PDF", notifications.get(0).getSubject().orElse("Unknown Subject"));
        assertEquals("Pre-compiled PDF", notifications.get(1).getSubject().orElse("Unknown Subject"));
    }


    @Test
    public void sendsDirectionIssuedLetterToAppellantAndRepresentative() throws IOException, NotificationClientException {

        NotificationEventType notificationEventType = NotificationEventType.DIRECTION_ISSUED;

        simulateCcdCallback(notificationEventType,
                notificationEventType.getId() + "Callback.json");

        List<Notification> notifications = fetchLetters();

        assertEquals(2, notifications.size());
        assertEquals("Pre-compiled PDF", notifications.get(0).getSubject().orElse("Unknown Subject"));
        assertEquals("Pre-compiled PDF", notifications.get(1).getSubject().orElse("Unknown Subject"));
    }

    @Test
    public void sendsDecisionIssuedLetterToAppellantAndRepresentative() throws IOException, NotificationClientException {

        NotificationEventType notificationEventType = NotificationEventType.DECISION_ISSUED;

        simulateCcdCallback(notificationEventType,
                notificationEventType.getId() + "Callback.json");

        List<Notification> notifications = fetchLetters();

        assertEquals(2, notifications.size());
        assertEquals("Pre-compiled PDF", notifications.get(0).getSubject().orElse("Unknown Subject"));
        assertEquals("Pre-compiled PDF", notifications.get(1).getSubject().orElse("Unknown Subject"));
    }



}
