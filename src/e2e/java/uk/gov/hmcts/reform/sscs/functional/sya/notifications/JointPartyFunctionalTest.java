package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.apache.commons.lang.WordUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_UPLOAD_RESPONSE_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_REMINDER_NOTIFICATION;

import java.lang.reflect.Field;
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
    @Value("${notification.english.oral.evidenceReminder.joint_party.emailId}")
    private String oralEvidenceReminderJointPartyEmailId;
    @Value("${notification.english.oral.evidenceReminder.joint_party.smsId}")
    private String oralEvidenceReminderJointPartySmsId;
    @Value("${notification.english.oral.dwpUploadResponse.joint_party.emailId}")
    private String oralResponseReceivedJointPartyEmailId;
    @Value("${notification.english.oral.dwpUploadResponse.joint_party.smsId}")
    private String oralUploadResponseJointPartySmsId;

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

        List<Notification> notifications = tryFetchNotificationsForTestCase(jointPartyEmailId, jointPartySmsId);

        assertEquals(2, notifications.size());

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
            new Object[]{EVIDENCE_REMINDER_NOTIFICATION, ORAL, expectedNumberOfLettersIsZero},
            new Object[]{DWP_UPLOAD_RESPONSE_NOTIFICATION, ORAL, expectedNumberOfLettersIsTwo}
        };
    }
}