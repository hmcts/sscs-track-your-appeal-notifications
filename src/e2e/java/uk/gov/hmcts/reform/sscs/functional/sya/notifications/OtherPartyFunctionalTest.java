package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;
import uk.gov.service.notify.Notification;

public class OtherPartyFunctionalTest extends AbstractFunctionalTest {

    private SscsCaseData sscsCaseData;
    @Value("${notification.english.oral.dwpUploadResponse.other_party.smsId}")
    private String oralDwpUploadResponseJointPartySmsId;
    @Value("${notification.english.oral.dwpUploadResponse.other_party.emailId}")
    private String oralDwpUploadResponseOtherPartyEmailId;

    public OtherPartyFunctionalTest() {
        super(30);
    }


    @Test
    @Parameters({"oral-,DWP_UPLOAD_RESPONSE_NOTIFICATION, oralDwpUploadResponseJointPartySmsId, oralDwpUploadResponseOtherPartyEmailId"})
    public void willSendDwpUploadResponse(@Nullable String prefix, NotificationEventType notificationEventType, String... fieldNames) throws Exception {

        simulateCcdCallback(notificationEventType,
                "otherparty/" + prefix + notificationEventType.getId() + "Callback.json");

        List<Notification> notifications = fetchLetters();
        assertEquals(2, notifications.size());
        notifications.forEach(n -> assertEquals("Pre-compiled PDF", n.getSubject().orElse("Unknown Subject")));
        tryFetchNotificationsForTestCase(getFieldValue(fieldNames));

    }

    private String[] getFieldValue(String... fieldNames) {
        return Arrays.stream(fieldNames)
                .map(this::getValue)
                .toArray(String[]::new);
    }

    private String getValue(String fieldName)  {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(this);
        } catch (Exception e) {
            return null;
        }
    }
}
