package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.apache.commons.lang.WordUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;

public class JointPartyFunctionalTest extends AbstractFunctionalTest {
    private static final String NO_HEARING_TYPE = null;
    private static final String ORAL = "oral";
    @Value("${notification.english.appealLapsed.joint_party.emailId}")
    private String appealLapsedJointPartyEmailId;
    @Value("${notification.english.appealLapsed.joint_party.smsId}")
    private String appealLapsedJointPartySmsId;
    @Value("${notification.english.hearingAdjourned.joint_party.emailId}")
    private String hearingAdjournedJointPartyEmailId;
    @Value("${notification.english.hearingAdjourned.joint_party.smsId}")
    private String hearingAdjournedJointPartySmsId;
    @Value("${notification.english.hearingPostponed.joint_party.emailId}")
    private String hearingPostponedJointPartyEmailId;
    @Value("${notification.english.oral.evidenceReminder.joint_party.emailId}")
    private String oralEvidenceReminderJointPartyEmailId;
    @Value("${notification.english.oral.evidenceReminder.joint_party.smsId}")
    private String oralEvidenceReminderJointPartySmsId;
    @Value("${notification.english.oral.evidenceReceived.joint_party.emailId}")
    private String oralEvidenceReceivedJointPartyEmailId;
    @Value("${notification.english.oral.evidenceReceived.joint_party.smsId}")
    private String oralEvidenceReceivedJointPartySmsId;
    @Value("${notification.english.appealWithdrawn.joint_party.emailId}")
    private String appealWithdrawnJointPartyEmailId;
    @Value("${notification.english.appealWithdrawn.joint_party.smsId}")
    private String appealWithdrawnJointPartySmsId;


    public JointPartyFunctionalTest() {
        super(30);
    }

    @Test
    @Parameters(method = "eventTypeAndSubscriptions")
    public void givenEventAndJointPartySubscription_shouldSendNotificationToJointParty(
            NotificationEventType notificationEventType, @Nullable String hearingType, int expectedNumberOfLetters)
            throws Exception {
        //Given
        final String jointPartyEmailId = getFieldValue(hearingType, notificationEventType, "JointPartyEmailId");
        final String jointPartySmsId = getFieldValue(hearingType, notificationEventType, "JointPartySmsId");

        simulateCcdCallback(notificationEventType,
            "jointParty/" + ((hearingType == null) ? EMPTY : (hearingType + "-")) + notificationEventType.getId() + "Callback.json");

        List<String> expectedIds = new ArrayList<>();

        if (jointPartyEmailId != null) {
            expectedIds.add(jointPartyEmailId);
        }

        if (jointPartySmsId != null) {
            expectedIds.add(jointPartySmsId);
        }

        List<Notification> notifications = tryFetchNotificationsForTestCase(expectedIds.toArray(new String[expectedIds.size()]));

        assertEquals(expectedIds.size(), notifications.size());

        String jointPartyName = "Joint Party";
        assertNotificationBodyContains(notifications, jointPartyEmailId, jointPartyName);
        assertNotificationBodyContains(notifications, jointPartySmsId);

        if (expectedNumberOfLetters > 0) {
            List<Notification> notificationLetters = fetchLetters();
            assertEquals(expectedNumberOfLetters, notificationLetters.size());
            Optional<Notification> notificationOptional =
                    notificationLetters.stream().filter(notification ->
                            notification.getLine1().map(f -> f.contains(jointPartyName)).orElse(false)).findFirst();
            assertTrue(notificationOptional.isPresent());
            assertTrue(notificationOptional.get().getBody().contains("Dear " + jointPartyName));
        }
    }

    private String getFieldValue(String hearingType, NotificationEventType notificationEventType, String fieldName) throws Exception {
        String fieldValue;
        try {
            Field field = this.getClass().getDeclaredField(
                    defaultIfBlank(hearingType, EMPTY)
                            + ((hearingType == null) ? notificationEventType.getId() : capitalize(notificationEventType.getId()))
                            + fieldName);
            field.setAccessible(true);
            fieldValue = (String) field.get(this);
        } catch (NoSuchFieldException e) {
            fieldValue = null;
        }
        return fieldValue;
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] eventTypeAndSubscriptions() {
        final int expectedNumberOfLettersIsTwo = 2;
        final int expectedNumberOfLettersIsZero = 0;
        return new Object[]{
            new Object[]{APPEAL_LAPSED_NOTIFICATION, NO_HEARING_TYPE, expectedNumberOfLettersIsTwo},
            new Object[]{ADJOURNED_NOTIFICATION, NO_HEARING_TYPE, expectedNumberOfLettersIsZero},
            new Object[]{POSTPONEMENT_NOTIFICATION, NO_HEARING_TYPE, expectedNumberOfLettersIsZero},
            new Object[]{EVIDENCE_REMINDER_NOTIFICATION, ORAL, expectedNumberOfLettersIsZero},
            new Object[]{EVIDENCE_RECEIVED_NOTIFICATION, ORAL, expectedNumberOfLettersIsZero},
            new Object[]{APPEAL_WITHDRAWN_NOTIFICATION, ORAL, expectedNumberOfLettersIsTwo}
        };
    }
}