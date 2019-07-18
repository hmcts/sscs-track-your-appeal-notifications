package uk.gov.hmcts.reform.sscs.domain.notify;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class NotificationEventTypeTest {
    @Test
    public void checkEventsWeDontHandle() {
        String event = "answers_submitted";
        assertFalse(NotificationEventType.checkEvent(event));
    }

    @Test
    public void checkEventWeDoHandle() {
        String event = "question_round_issued";
        assertTrue(NotificationEventType.checkEvent(event));

        event = "question_deadline_elapsed";
        assertTrue(NotificationEventType.checkEvent(event));

        event = "question_deadline_reminder";
        assertTrue(NotificationEventType.checkEvent(event));

        event = "continuous_online_hearing_relisted";
        assertTrue(NotificationEventType.checkEvent(event));

        event = "decision_issued";
        assertTrue(NotificationEventType.checkEvent(event));

        event = "corDecision";
        assertTrue(NotificationEventType.checkEvent(event));
    }

    @Test
    @Parameters({
            "appealCreated", "appealLapsed", "appealReceived", "appealWithdrawn", "evidenceReceived",
            "hearingAdjourned", "hearingBooked", "hearingPostponed", "responseReceived", "subscriptionUpdated",
            "appealDormant", "assignToJudge", "caseUpdated", "requestInfoIncompleteApplication", "nonCompliant",
            "struckOut", "corDecision", "resendAppealCreated", "directionIssued", "tcwDirectionIssued", "assignToJudge",
            "judgeDirectionIssued", "validAppealCreated", "judgeDecisionAppealToProceed", "tcwDecisionAppealToProceed"
    })
    public void checkEventsWeNeedToHandle(String event) {
        assertTrue(NotificationEventType.checkEvent(event));
    }
}
