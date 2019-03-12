package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.lang.reflect.Field;
import java.util.List;
import junitparams.Parameters;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;

public class WithRepresentativePersonalisationTest extends AbstractFunctionalTest {
    @Value("${notification.appealLapsed.appellant.emailId}")
    private String appealLapsedAppellantEmailId;
    @Value("${notification.appealLapsed.appellant.smsId}")
    private String appealLapsedAppellantSmsId;
    @Value("${notification.appealLapsed.representative.emailId}")
    private String appealLapsedRepsEmailId;
    @Value("${notification.appealLapsed.representative.smsId}")
    private String appealLapsedRepsSmsId;
    @Value("${notification.appealWithdrawn.appellant.emailId}")
    private String appealWithdrawnAppellantEmailId;
    @Value("${notification.appealWithdrawn.appellant.smsId}")
    private String appealWithdrawnAppellantSmsId;
    @Value("${notification.appealWithdrawn.representative.emailId}")
    private String appealWithdrawnRepsEmailId;
    @Value("${notification.appealWithdrawn.representative.smsId}")
    private String appealWithdrawnRepsSmsId;
    @Value("${notification.hearingBooked.appellant.emailId}")
    private String hearingBookedAppellantEmailId;
    @Value("${notification.hearingBooked.appellant.smsId}")
    private String hearingBookedAppellantSmsId;
    @Value("${notification.hearingBooked.representative.emailId}")
    private String hearingBookedRepsEmailId;
    @Value("${notification.hearingBooked.representative.smsId}")
    private String hearingBookedRepsSmsId;
    @Value("${notification.appealCreated.appellant.emailId}")
    private String appealCreatedAppellantEmailId;
    @Value("${notification.appealCreated.appellant.smsId}")
    private String appealCreatedAppellantSmsId;
    @Value("${notification.appealCreated.representative.emailId}")
    private String appealCreatedRepsEmailId;
    @Value("${notification.appealCreated.representative.smsId}")
    private String appealCreatedRepsSmsId;
    @Value("${notification.paper.appealDormant.appellant.emailId}")
    private String appealDormantAppellantEmailId;
    @Value("${notification.paper.appealDormant.appellant.smsId}")
    private String appealDormantAppellantSmsId;
    @Value("${notification.paper.appealDormant.representative.emailId}")
    private String appealDormantRepsEmailId;
    @Value("${notification.paper.appealDormant.representative.smsId}")
    private String appealDormantRepsSmsId;

    @Value("${notification.hearingAdjourned.appellant.emailId}")
    private String hearingAdjournedAppellantEmailId;
    @Value("${notification.hearingAdjourned.appellant.smsId}")
    private String hearingAdjournedAppellantSmsId;
    @Value("${notification.hearingAdjourned.representative.emailId}")
    private String hearingAdjournedRepsEmailId;
    @Value("${notification.hearingAdjourned.representative.smsId}")
    private String hearingAdjournedRepsSmsId;
    @Value("${notification.appealReceived.appellant.emailId}")
    private String appealReceivedAppellantEmailId;
    @Value("${notification.appealReceived.appellant.smsId}")
    private String appealReceivedAppellantSmsId;
    @Value("${notification.appealReceived.representative.emailId}")
    private String appealReceivedRepsEmailId;
    @Value("${notification.appealReceived.representative.smsId}")
    private String appealReceivedRepsSmsId;

    @Value("${notification.hearingPostponed.appellant.emailId}")
    private String hearingPostponedAppellantEmailId;
    @Value("${notification.hearingPostponed.representative.emailId}")
    private String hearingPostponedRepsEmailId;

    public WithRepresentativePersonalisationTest() {
        super(30);
    }

    @Test
    @Parameters(method = "eventTypeAndSubscriptions")
    public void givenEventAndRepsSubscription_shouldSendNotificationToReps(NotificationEventType notificationEventType)
            throws Exception {
        //Given
        final String appellantEmailId = getFieldValue(notificationEventType, "AppellantEmailId");
        final String appellantSmsId = getFieldValue(notificationEventType, "AppellantSmsId");
        final String repsEmailId = getFieldValue(notificationEventType, "RepsEmailId");
        final String repsSmsId = getFieldValue(notificationEventType, "RepsSmsId");

        simulateCcdCallback(notificationEventType,
            "representative/" + notificationEventType.getId() + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                appellantEmailId, appellantSmsId,
                repsEmailId, repsSmsId);

        String representativeName = "Harry Potter";
        assertNotificationBodyContains(notifications, repsEmailId, representativeName);
        assertNotificationBodyContains(notifications, repsSmsId);
    }

    public void givenHearingPostponedEventAndRepsSubscription_shouldSendEmailOnlyNotificationToReps()
            throws Exception {

        final String appellantEmailId = getFieldValue(POSTPONEMENT_NOTIFICATION, "AppellantEmailId");
        final String repsEmailId = getFieldValue(POSTPONEMENT_NOTIFICATION, "RepsEmailId");

        simulateCcdCallback(POSTPONEMENT_NOTIFICATION,
                "representative/" + POSTPONEMENT_NOTIFICATION.getId()
                        + "Callback.json");

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                appellantEmailId, repsEmailId);

        assertNotificationBodyContains(notifications, appellantEmailId);

        String representativeName = "Harry Potter";
        assertNotificationBodyContains(notifications, repsEmailId, representativeName);
    }

    private String getFieldValue(NotificationEventType notificationEventType, String fieldName) throws Exception {
        String fieldValue;
        try {
            Field field = this.getClass().getDeclaredField(notificationEventType.getId() + fieldName);
            field.setAccessible(true);
            fieldValue = (String) field.get(this);
        } catch (NoSuchFieldException e) {
            fieldValue = null;
        }
        return fieldValue;
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] eventTypeAndSubscriptions() {
        return new Object[]{
            new Object[]{APPEAL_LAPSED_NOTIFICATION},
            new Object[]{APPEAL_WITHDRAWN_NOTIFICATION},
            new Object[]{SYA_APPEAL_CREATED_NOTIFICATION},
            new Object[]{APPEAL_DORMANT_NOTIFICATION},
            new Object[]{ADJOURNED_NOTIFICATION},
            new Object[]{APPEAL_RECEIVED_NOTIFICATION},
            new Object[]{HEARING_BOOKED_NOTIFICATION}
        };
    }
}