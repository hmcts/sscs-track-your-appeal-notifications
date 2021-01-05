package uk.gov.hmcts.reform.sscs.functional.sya.notifications;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_LAPSED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.NON_COMPLIANT_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REQUEST_INFO_INCOMPLETE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.REVIEW_CONFIDENTIALITY_REQUEST;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.STRUCK_OUT;

import java.io.IOException;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
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
    @Parameters(method = "eventTypes")
    public void shouldSendDocmosisLetters(NotificationEventType notificationEventType, int expectedNumberOfLetters) throws IOException, NotificationClientException {

        simulateCcdCallback(notificationEventType,
                notificationEventType.getId() + "Callback.json");

        List<Notification> notifications = fetchLetters();
        assertEquals(expectedNumberOfLetters, notifications.size());
        notifications.forEach(n -> assertEquals("Pre-compiled PDF", n.getSubject().orElse("Unknown Subject")));
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] eventTypes() {
        int expectedNumberOfLettersIsOne = 1;
        int expectedNumberOfLettersIsTwo = 2;
        int expectedNumberOfLettersIsThree = 3;
        return new Object[]{
           new Object[]{REQUEST_INFO_INCOMPLETE, expectedNumberOfLettersIsThree},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE, expectedNumberOfLettersIsTwo},
            new Object[]{STRUCK_OUT, expectedNumberOfLettersIsTwo},
            new Object[]{ISSUE_FINAL_DECISION, expectedNumberOfLettersIsTwo},
            new Object[]{DECISION_ISSUED, expectedNumberOfLettersIsTwo},
            new Object[]{DIRECTION_ISSUED, expectedNumberOfLettersIsTwo},
            new Object[]{APPEAL_RECEIVED_NOTIFICATION, expectedNumberOfLettersIsTwo},
            new Object[]{REVIEW_CONFIDENTIALITY_REQUEST, expectedNumberOfLettersIsOne},
            new Object[]{NON_COMPLIANT_NOTIFICATION, expectedNumberOfLettersIsTwo},
        };
    }
}
