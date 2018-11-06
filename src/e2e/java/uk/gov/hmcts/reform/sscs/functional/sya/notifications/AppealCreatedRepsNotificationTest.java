package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.functional.AbstractFunctionalTest;

public class AppealCreatedRepsNotificationTest extends AbstractFunctionalTest {
    public AppealCreatedRepsNotificationTest() {
        super(30);
    }

    @Test
    public void givenAppealCreatedEventAndRepsSubscription_shouldSendAppealCreatedNotificationToReps() {

    }
}
