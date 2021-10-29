package uk.gov.hmcts.reform.sscs.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;

@RunWith(JUnitParamsRunner.class)
public class CcdNotificationWrapperTest {

    private CcdNotificationWrapper ccdNotificationWrapper;

    @Test
    @Parameters({"paper, PAPER", "oral, ORAL"})
    public void should_returnAccordingAppealHearingType_when_hearingTypeIsPresent(String hearingType,
                                                                                  AppealHearingType expected) {
        ccdNotificationWrapper = buildCcdNotificationWrapper(hearingType);

        assertThat(ccdNotificationWrapper.getHearingType(), is(expected));
    }

    private CcdNotificationWrapper buildCcdNotificationWrapper(String hearingType) {
        return new CcdNotificationWrapper(
                SscsCaseDataWrapper.builder()
                        .newSscsCaseData(SscsCaseData.builder()
                                .appeal(Appeal.builder()
                                        .hearingType(hearingType)
                                        .build())
                                .subscriptions(Subscriptions.builder()
                                        .appellantSubscription(Subscription.builder().build())
                                        .representativeSubscription(Subscription.builder().build())
                                        .build())
                                .build())
                        .build()
        );
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithRep(NotificationEventType notificationEventType) {
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, null, Representative.builder().hasRepresentative("Yes").build(), false);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(NotificationEventType notificationEventType, Representative rep, boolean jointParty) {
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, null, rep, jointParty);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventType(NotificationEventType notificationEventType) {
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, null, null, false);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventType(NotificationEventType notificationEventType, Appointee appointee, Representative representative, boolean jointParty) {
        Appellant appellant = Appellant.builder().build();
        List<HearingRecordingRequest> releasedHearings = new ArrayList<>();
        Subscription appointeeSubscription = null;
        if (null != appointee) {
            appellant.setAppointee(appointee);
            appointeeSubscription = Subscription.builder()
                .email("appointee@test.com")
                .subscribeEmail("Yes")
                .build();
            HearingRecordingRequest appointeeHearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty(PartyItemList.APPELLANT.getCode()).build()).build();
            releasedHearings.add(appointeeHearingRecordingRequest);
        }

        Subscription repSubscription = null;
        if (null != representative) {
            representative = Representative.builder()
                .hasRepresentative("Yes")
                .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                .build();
            repSubscription = Subscription.builder()
                .email("rep@test.com")
                .subscribeEmail("Yes")
                .build();
            HearingRecordingRequest repHearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty(PartyItemList.REPRESENTATIVE.getCode()).build()).build();
            releasedHearings.add(repHearingRecordingRequest);
        }

        Subscription jointPartySubscription = null;
        String jointPartyYesNo = "No";
        JointPartyName jointPartyName = null;
        if (jointParty) {
            jointPartyYesNo = "Yes";
            jointPartyName = JointPartyName.builder().title("Madam").firstName("Jon").lastName("Party").build();
            jointPartySubscription = Subscription.builder()
                    .email("joint@test.com")
                    .subscribeEmail("Yes")
                    .build();
            HearingRecordingRequest jointPartyHearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder().requestingParty(PartyItemList.JOINT_PARTY.getCode()).build()).build();
            releasedHearings.add(jointPartyHearingRecordingRequest);
        }

        return new CcdNotificationWrapper(
            SscsCaseDataWrapper.builder()
                .oldSscsCaseData(SscsCaseData.builder().build())
                .newSscsCaseData(SscsCaseData.builder()
                    .jointParty(jointPartyYesNo)
                    .jointPartyName(jointPartyName)
                    .appeal(Appeal.builder()
                        .appellant(appellant)
                        .hearingType("cor")
                        .rep(representative)
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder().build())
                        .representativeSubscription(repSubscription)
                        .appointeeSubscription(appointeeSubscription)
                        .jointPartySubscription(jointPartySubscription)
                        .build())
                     .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().citizenReleasedHearings(releasedHearings).build())
                    .build())
                .notificationEventType(notificationEventType)
                .build()

        );
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(
            NotificationEventType notificationEventType, String hearingType) {
        return buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType, null);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(
            NotificationEventType notificationEventType, String hearingType, DirectionType directionType) {
        Appointee appointee = Appointee.builder()
            .name(Name.builder().firstName("Ap").lastName("Pointee").build())
            .address(Address.builder().line1("Appointee Line 1").town("Appointee Town").county("Appointee County").postcode("AP9 0IN").build())
            .build();

        DynamicList dynamicList = null;
        if (directionType != null) {
            dynamicList = new DynamicList(directionType.toString());
        }

        return new CcdNotificationWrapper(
            SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                        .jointParty("Yes")
                        .jointPartyAddressSameAsAppellant("Yes")
                        .jointPartyName(JointPartyName.builder().title("Madam").firstName("Jon").lastName("Party").build())
                    .appeal(Appeal.builder()
                        .hearingType(hearingType)
                        .appellant(Appellant.builder().appointee(appointee).build())
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(
                            Subscription.builder()
                                .email("appellant@test.com")
                                .subscribeEmail("Yes")
                                .build()
                        )
                        .appointeeSubscription(
                            Subscription.builder()
                                .email("appointee@test.com")
                                .subscribeEmail("Yes")
                                .build()
                        )
                            .jointPartySubscription(
                                Subscription.builder()
                                    .email("jointParty@test.com")
                                    .subscribeEmail("Yes")
                                    .build()
                            )
                        .build())
                    .directionTypeDl(dynamicList)
                    .build())
                .notificationEventType(notificationEventType)
                .build()

        );
    }

    @Test
    @Parameters({"APPEAL_LAPSED_NOTIFICATION","HMCTS_APPEAL_LAPSED_NOTIFICATION","DWP_APPEAL_LAPSED_NOTIFICATION","APPEAL_WITHDRAWN_NOTIFICATION","EVIDENCE_RECEIVED_NOTIFICATION",
            "POSTPONEMENT_NOTIFICATION","HEARING_BOOKED_NOTIFICATION","SYA_APPEAL_CREATED_NOTIFICATION","VALID_APPEAL_CREATED",
            "RESEND_APPEAL_CREATED_NOTIFICATION", "APPEAL_RECEIVED_NOTIFICATION", "ADJOURNED_NOTIFICATION", "ISSUE_FINAL_DECISION_WELSH",
            "PROCESS_AUDIO_VIDEO", "PROCESS_AUDIO_VIDEO_WELSH", "ACTION_POSTPONEMENT_REQUEST", "ACTION_POSTPONEMENT_REQUEST_WELSH",
            "APPEAL_DORMANT_NOTIFICATION", "DWP_RESPONSE_RECEIVED_NOTIFICATION", "STRUCK_OUT", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH"})
    public void givenSubscriptions_shouldGetAppellantAndRepSubscriptionTypeList(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    @Parameters({"SYA_APPEAL_CREATED_NOTIFICATION, cor", "DWP_RESPONSE_RECEIVED_NOTIFICATION, oral",
            "DWP_RESPONSE_RECEIVED_NOTIFICATION, paper", "HMCTS_APPEAL_LAPSED_NOTIFICATION, paper", "HMCTS_APPEAL_LAPSED_NOTIFICATION, oral",
            "DWP_APPEAL_LAPSED_NOTIFICATION, paper", "DWP_APPEAL_LAPSED_NOTIFICATION, oral", "SUBSCRIPTION_UPDATED_NOTIFICATION, paper",
            "VALID_APPEAL_CREATED, cor", "RESEND_APPEAL_CREATED_NOTIFICATION, cor"})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointee(NotificationEventType notificationEventType, String hearingType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters({"APPEAL_LAPSED_NOTIFICATION, paper", "APPEAL_LAPSED_NOTIFICATION, oral", "EVIDENCE_REMINDER_NOTIFICATION, oral", "EVIDENCE_REMINDER_NOTIFICATION, paper",
            "APPEAL_DORMANT_NOTIFICATION, paper", "APPEAL_DORMANT_NOTIFICATION, oral", "ADJOURNED_NOTIFICATION, paper", "ADJOURNED_NOTIFICATION, oral", "POSTPONEMENT_NOTIFICATION, paper", "POSTPONEMENT_NOTIFICATION, oral",
            "EVIDENCE_RECEIVED_NOTIFICATION, paper", "EVIDENCE_RECEIVED_NOTIFICATION, oral", "APPEAL_WITHDRAWN_NOTIFICATION, paper", "STRUCK_OUT, oral", "STRUCK_OUT, paper", "DIRECTION_ISSUED, oral", "DIRECTION_ISSUED, paper",
            "DIRECTION_ISSUED_WELSH, oral", "DIRECTION_ISSUED_WELSH, paper", "DWP_UPLOAD_RESPONSE_NOTIFICATION, paper",
            "PROCESS_AUDIO_VIDEO, oral", "PROCESS_AUDIO_VIDEO_WELSH, paper", "ACTION_POSTPONEMENT_REQUEST, paper", "ACTION_POSTPONEMENT_REQUEST_WELSH, paper",
            "DWP_UPLOAD_RESPONSE_NOTIFICATION, oral", "HEARING_BOOKED_NOTIFICATION, oral", "HEARING_BOOKED_NOTIFICATION, paper",  "HEARING_REMINDER_NOTIFICATION, oral", "HEARING_REMINDER_NOTIFICATION, paper",
            "ISSUE_ADJOURNMENT_NOTICE, paper", "ISSUE_ADJOURNMENT_NOTICE_WELSH, oral"})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointeeAndJointParty(NotificationEventType notificationEventType, String hearingType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    public void givenSubscriptionForAppellantRepAndJointParty_shouldGetSubscriptionTypeListForAppellantAndJointPartyOnlyWhenBothGranted() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(REVIEW_CONFIDENTIALITY_REQUEST, Representative.builder().hasRepresentative("Yes").build(), true);
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    public void givenSubscriptionForAppellantRepAndJointParty_shouldGetSubscriptionTypeListForAppellantOnlyWhenOnlyAppellantGranted() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(REVIEW_CONFIDENTIALITY_REQUEST, Representative.builder().hasRepresentative("Yes").build(), true);
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeJointParty(null);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenSubscriptionForAppellantRepAndJointParty_shouldGetSubscriptionTypeListForJointPartyOnlyWhenOnlyJointPartyGranted() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(REVIEW_CONFIDENTIALITY_REQUEST, Representative.builder().hasRepresentative("Yes").build(), true);
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeAppellant(null);
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenSubscriptionForAppellantRepAndJointParty_shouldGetSubscriptionTypeListForJointPartyOnlyWhenOnlyJointPartyIsNewlyGranted() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(REVIEW_CONFIDENTIALITY_REQUEST, Representative.builder().hasRepresentative("Yes").build(), true);
        ccdNotificationWrapper.getOldSscsCaseData().setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        ccdNotificationWrapper.getOldSscsCaseData().setConfidentialityRequestOutcomeJointParty(null);
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        ccdNotificationWrapper.getNewSscsCaseData().setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters(method = "getEventTypeFilteredWithAppellant")
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppellant(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters({"DIRECTION_ISSUED, paper, PROVIDE_INFORMATION", "DIRECTION_ISSUED, oral, PROVIDE_INFORMATION",
            "DIRECTION_ISSUED, paper, APPEAL_TO_PROCEED", "DIRECTION_ISSUED, oral, APPEAL_TO_PROCEED",
            "DIRECTION_ISSUED, paper, GRANT_EXTENSION", "DIRECTION_ISSUED, oral, GRANT_EXTENSION",
            "DIRECTION_ISSUED, paper, REFUSE_EXTENSION", "DIRECTION_ISSUED, oral, REFUSE_EXTENSION",})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointeeAndJointPartyDirection(NotificationEventType notificationEventType, String hearingType, DirectionType directionType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType, directionType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    public void givenRequestInfoIncompleteForAppointeeWithSubscription_shouldSendRequestInfoNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(REQUEST_INFO_INCOMPLETE, "oral");
        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.APPELLANT.getCode(), PartyItemList.APPELLANT.getLabel()), new ArrayList<>()));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenRequestInfoIncompleteForAppellantWithSubscription_shouldSendRequestInfoNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(REQUEST_INFO_INCOMPLETE);
        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.APPELLANT.getCode(), PartyItemList.APPELLANT.getLabel()), new ArrayList<>()));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenRequestInfoIncompleteForRepWithSubscription_shouldSendRequestInfoNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(REQUEST_INFO_INCOMPLETE);
        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.REPRESENTATIVE.getCode(), PartyItemList.REPRESENTATIVE.getLabel()), new ArrayList<>()));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenRequestInfoIncompleteForJointPartyWithSubscription_shouldSendRequestInfoNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(REQUEST_INFO_INCOMPLETE, "paper");
        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.JOINT_PARTY.getCode(), PartyItemList.JOINT_PARTY.getLabel()), new ArrayList<>()));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenProcessHearingRequestForRepWithSubscription_shouldSendProcessHearingRequestNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(ACTION_HEARING_RECORDING_REQUEST);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenProcessHearingRequestForJointPartyWithSubscription_shouldSendProcessHearingRequestNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithJointParty(ACTION_HEARING_RECORDING_REQUEST, null, true);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    public void givenProcessHearingRequestForNoPartyWithSubscription_shouldNotSendProcessHearingRequestNotification() {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(ACTION_HEARING_RECORDING_REQUEST, null, null, false);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertTrue(subsWithTypeList.isEmpty());
    }

    @SuppressWarnings({"unused"})
    private Object[] getEventTypeFilteredWithAppellant() {
        return Arrays.stream(values())
            .filter(type -> !(type.equals(APPEAL_LAPSED_NOTIFICATION)
                || type.equals(HMCTS_APPEAL_LAPSED_NOTIFICATION)
                || type.equals(DWP_APPEAL_LAPSED_NOTIFICATION)
                || type.equals(APPEAL_WITHDRAWN_NOTIFICATION)
                || type.equals(EVIDENCE_RECEIVED_NOTIFICATION)
                || type.equals(CASE_UPDATED)
                || type.equals(APPEAL_DORMANT_NOTIFICATION)
                || type.equals(ADJOURNED_NOTIFICATION)
                || type.equals(APPEAL_RECEIVED_NOTIFICATION)
                || type.equals(POSTPONEMENT_NOTIFICATION)
                || type.equals(SUBSCRIPTION_UPDATED_NOTIFICATION)
                || type.equals(HEARING_BOOKED_NOTIFICATION)
                || type.equals(STRUCK_OUT)
                || type.equals(REQUEST_INFO_INCOMPLETE)
                || type.equals(NON_COMPLIANT_NOTIFICATION)
                || type.equals(REVIEW_CONFIDENTIALITY_REQUEST)
                || type.equals(ACTION_HEARING_RECORDING_REQUEST)
            )).toArray();
    }

}
