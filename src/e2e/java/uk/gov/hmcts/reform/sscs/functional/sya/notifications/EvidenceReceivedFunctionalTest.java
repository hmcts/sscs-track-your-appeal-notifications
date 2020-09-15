package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.ORAL;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.PAPER;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_RECEIVED_NOTIFICATION;

import java.lang.reflect.Field;
import java.util.List;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;

public class EvidenceReceivedFunctionalTest extends AbstractFunctionalTest {

    @Value("${notification.english.oral.evidenceReceived.appellant.emailId}")
    private String oralEvidenceReceivedAppellantEmailId;
    @Value("${notification.english.oral.evidenceReceived.appellant.smsId}")
    private String oralEvidenceReceivedAppellantSmsId;
    @Value("${notification.english.paper.evidenceReceived.appellant.emailId}")
    private String paperEvidenceReceivedAppellantEmailId;
    @Value("${notification.english.paper.evidenceReceived.appellant.smsId}")
    private String paperEvidenceReceivedAppellantSmsId;
    @Value("${notification.english.oral.evidenceReceived.representative.emailId}")
    private String oralEvidenceReceivedRepsEmailId;
    @Value("${notification.english.oral.evidenceReceived.representative.smsId}")
    private String oralEvidenceReceivedRepsSmsId;
    @Value("${notification.english.paper.evidenceReceived.representative.emailId}")
    private String paperEvidenceReceivedRepsEmailId;
    @Value("${notification.english.paper.evidenceReceived.representative.smsId}")
    private String paperEvidenceReceivedRepsSmsId;

    public EvidenceReceivedFunctionalTest() {
        super(30);
    }

    @Test
    @Parameters({"ORAL", "PAPER"})
    public void givenEvidenceReceivedWithRepsSubscription_shouldSendNotificationToReps(AppealHearingType appealHearingType)
            throws Exception {
        //Given
        final String appellantEmailId = getFieldValue(appealHearingType, "AppellantEmailId");
        final String appellantSmsId = getFieldValue(appealHearingType, "AppellantSmsId");
        final String repsEmailId = getFieldValue(appealHearingType, "RepsEmailId");
        final String repsSmsId = getFieldValue(appealHearingType, "RepsSmsId");

        simulateCcdCallback(EVIDENCE_RECEIVED_NOTIFICATION,
                "representative/"
                        + (appealHearingType != null ? (appealHearingType.name().toLowerCase() + "-") : "")
                        + EVIDENCE_RECEIVED_NOTIFICATION.getId() + "Callback.json");

        final List<Notification> notifications = (StringUtils.isBlank(appellantSmsId))
                ? tryFetchNotificationsForTestCase(appellantEmailId, repsEmailId, repsSmsId)
                : tryFetchNotificationsForTestCase(appellantEmailId, appellantSmsId, repsEmailId, repsSmsId);

        assertNotificationBodyContains(notifications, appellantEmailId);
        if (!PAPER.equals(appealHearingType)) {
            assertNotificationBodyContains(notifications, appellantSmsId);
        }

        String representativeName = "Harry Potter";
        assertNotificationBodyContains(notifications, repsEmailId, representativeName);
        assertNotificationBodyContains(notifications, repsSmsId);
    }

    private String getFieldValue(AppealHearingType appealHearingType, String fieldName) throws Exception {
        Field field = null;

        if (appealHearingType != null) {
            field = getField(appealHearingType, fieldName);
        }

        if (field == null) {
            field = this.getClass().getDeclaredField(EVIDENCE_RECEIVED_NOTIFICATION.getId() + fieldName);
        }

        field.setAccessible(true);
        return (String) field.get(this);
    }

    @SuppressWarnings("unused")
    private Field getField(AppealHearingType appealHearingType, String fieldName) throws NoSuchFieldException {
        try {
            return this.getClass().getDeclaredField(appealHearingType.name().toLowerCase() + StringUtils.capitalize(EVIDENCE_RECEIVED_NOTIFICATION.getId()) + fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] evidenceReceivedNotifications() {
        return new Object[]{
                new Object[]{ORAL},
                new Object[]{PAPER},
        };
    }
}
