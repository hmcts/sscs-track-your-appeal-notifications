package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import uk.gov.hmcts.reform.sscs.SscsCaseDataUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;


public class NotificationValidServiceTest {

    private NotificationValidService notificationValidService;
    private SscsCaseData sscsCaseData;

    @Before
    public void setup() {
        notificationValidService = new NotificationValidService();
        sscsCaseData = SscsCaseData.builder().build();
    }

    @Test
    public void givenHearingIsInFutureAndEventIsHearingBooked_thenNotificationIsStillValidToSend() {
        assertTrue(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(sscsCaseData, 1), HEARING_BOOKED)
        );
    }

    @Test
    public void givenHearingIsInFutureAndEventIsHearingReminder_thenNotificationIsStillValidToSend() {
        assertTrue(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(sscsCaseData, 1), HEARING_REMINDER)
        );
    }

    @Test
    public void givenHearingIsInPastAndEventIsHearingBooked_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(sscsCaseData, -1), HEARING_BOOKED)
        );
    }

    @Test
    public void givenHearingIsInPastAndEventIsHearingReminder_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(sscsCaseData, -1), HEARING_REMINDER)
        );
    }

    @Test
    public void givenHearingIsInFutureAdjournedAndEventIsHearingBooked_thenNotificationIsNotValidToSend() {
        assertFalse(
                notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(sscsCaseData, 1, "Yes"), HEARING_BOOKED)
        );
    }

    @Test
    public void givenHearingIsInFutureAdjournedAndEventIsHearingReminder_thenNotificationIsNotValidToSend() {
        assertFalse(
                notificationValidService.isNotificationStillValidToSend(SscsCaseDataUtils.addHearing(sscsCaseData, 1, "Yes"), HEARING_REMINDER)
        );
    }

    @Test
    public void givenCaseDoesNotContainHearingAndEventIsHearingBooked_thenNotificationIsNotValidToSend() {
        assertFalse(
            notificationValidService.isNotificationStillValidToSend(null, HEARING_BOOKED)
        );
    }

    @Test
    public void givenCaseIsOralCaseAndNotificationTypeIsSentForOral_thenReturnTrue() {
        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("Yes").build()).build()).build();

        assertTrue(
            notificationValidService.isHearingTypeValidToSendNotification(caseData, APPEAL_RECEIVED)
        );
    }

    @Test
    public void givenCaseIsOralCaseAndNotificationTypeIsNotSentForOral_thenReturnFalse() {
        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("Yes").build()).build()).build();

        assertFalse(
            notificationValidService.isHearingTypeValidToSendNotification(caseData, DO_NOT_SEND)
        );
    }

    @Test
    public void givenCaseIsPaperCaseAndNotificationTypeIsSentForPaper_thenReturnTrue() {
        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("No").build()).build()).build();

        assertTrue(
            notificationValidService.isHearingTypeValidToSendNotification(caseData, APPEAL_RECEIVED)
        );
    }

    @Test
    public void givenCaseIsPaperCaseAndNotificationTypeIsNotSentForPaper_thenReturnFalse() {
        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("No").build()).build()).build();

        assertFalse(
            notificationValidService.isHearingTypeValidToSendNotification(caseData, HEARING_BOOKED)
        );
    }

    @Test
    public void givenCaseIsACohCaseAndNotificationTypeIsNotSentForCoh_thenReturnFalse() {
        SscsCaseData caseData = SscsCaseData.builder().appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("No").build()).build()).build();

        assertFalse(
                notificationValidService.isHearingTypeValidToSendNotification(caseData, DO_NOT_SEND)
        );
    }

    @DisplayName("When event notification type is other than ACTION_FURTHER_EVIDENCE then return true")
    @Test
    public void testIsNotificationValidForActionFurtherEvidence() {
        DynamicList sender = new DynamicList(new DynamicListItem("appellant", "Appellant"), new ArrayList<>());
        NotificationWrapper notificationWrapper = new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
            .notificationEventType(ACTION_HEARING_RECORDING_REQUEST)
            .newSscsCaseData(setupTestData(sender))
            .build());

        assertTrue(notificationValidService.isNotificationValidForActionFurtherEvidence(notificationWrapper, SubscriptionWithType.builder().partyId("12").build()));
    }

    @DisplayName("When event notification type is ACTION_FURTHER_EVIDENCE and send is Appellant return false")
    @Test
    public void testIsNotificationValidForActionFurtherEvidence1() {
        DynamicList sender = new DynamicList(new DynamicListItem("appellant", "Appellant"), new ArrayList<>());
        SscsCaseData caseData = setupTestData(sender);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(caseData.getSubscriptions().getAppointeeSubscription(), SubscriptionType.APPELLANT,
                caseData.getAppeal().getAppellant(), caseData.getAppeal().getAppellant().getAppointee());
        NotificationWrapper notificationWrapper = new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
            .notificationEventType(ACTION_FURTHER_EVIDENCE)
            .newSscsCaseData(setupTestData(sender))
            .build());

        assertFalse(notificationValidService.isNotificationValidForActionFurtherEvidence(notificationWrapper, subscriptionWithType));
    }

    @DisplayName("When event notification type is ACTION_FURTHER_EVIDENCE and send is Joint party return false")
    @Test
    public void testIsNotificationValidForActionFurtherEvidence2() {
        DynamicList sender = new DynamicList(new DynamicListItem("jointParty", "jointParty"), new ArrayList<>());
        SscsCaseData caseData = setupTestData(sender);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(caseData.getSubscriptions().getAppointeeSubscription(), SubscriptionType.JOINT_PARTY,
                caseData.getJointParty(), caseData.getAppeal().getAppellant().getAppointee());
        NotificationWrapper notificationWrapper = new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
            .notificationEventType(ACTION_FURTHER_EVIDENCE)
            .newSscsCaseData(setupTestData(sender))
            .build());

        assertFalse(notificationValidService.isNotificationValidForActionFurtherEvidence(notificationWrapper, subscriptionWithType));
    }

    @DisplayName("When event notification type is ACTION_FURTHER_EVIDENCE and sender is Other party representative return false")
    @Test
    public void testIsNotificationValidForActionFurtherEvidence3() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyRepb9186faf", "otherPartyRep"), new ArrayList<>());
        SscsCaseData caseData = setupTestData(sender);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(caseData.getSubscriptions().getAppointeeSubscription(), SubscriptionType.OTHER_PARTY,
                caseData.getOtherParties().get(0).getValue(), caseData.getAppeal().getAppellant().getAppointee());
        NotificationWrapper notificationWrapper = new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
            .notificationEventType(ACTION_FURTHER_EVIDENCE)
            .newSscsCaseData(setupTestData(sender))
            .build());

        assertFalse(notificationValidService.isNotificationValidForActionFurtherEvidence(notificationWrapper, subscriptionWithType));
    }

    private SscsCaseData setupTestData(DynamicList sender) {
        return SscsCaseData.builder()
                .originalSender(sender)
                .jointParty(JointParty.builder()
                        .id("JP12345")
                        .name(Name.builder()
                                .title("Mr.")
                                .firstName("Joint")
                                .lastName("Party")
                                .build())
                        .build())
                .otherParties(buildOtherPartyData())
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .id("AP12345")
                                .name(Name.builder()
                                        .title("Mr.")
                                        .firstName("Appellant")
                                        .lastName("Case")
                                        .build())
                                .build())
                        .rep(Representative.builder()
                                .name(Name.builder()
                                        .title("Mr.")
                                        .firstName("Representative")
                                        .lastName("Appellant")
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private List<CcdValue<OtherParty>> buildOtherPartyData() {
        return List.of(CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id("dedf975f")
                        .name(Name.builder()
                                .firstName("Other")
                                .lastName("Party")
                                .build())
                        .otherPartySubscription(Subscription.builder().email("other@party").subscribeEmail("Yes").build())
                        .rep(Representative.builder()
                                .id("b9186faf")
                                .name(Name.builder()
                                        .firstName("OtherParty")
                                        .lastName("Representative")
                                        .build())
                                .hasRepresentative(YesNo.YES.getValue())
                                .build())
                        .build())
                .build());
    }
}
