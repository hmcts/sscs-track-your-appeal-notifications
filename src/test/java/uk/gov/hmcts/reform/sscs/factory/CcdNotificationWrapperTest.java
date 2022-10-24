package uk.gov.hmcts.reform.sscs.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
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

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndRep(NotificationEventType notificationEventType) {
        Appointee appointee = Appointee.builder()
                .name(Name.builder().firstName("Ap").lastName("Pointee").build())
                .address(Address.builder().line1("Appointee Line 1").town("Appointee Town").county("Appointee County").postcode("AP9 0IN").build())
                .build();
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, appointee, Representative.builder().hasRepresentative("Yes").build(), true);
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
        YesNo jointPartyYesNo = NO;
        Name jointPartyName = null;
        if (jointParty) {
            jointPartyYesNo = YES;
            jointPartyName = Name.builder().title("Madam").firstName("Jon").lastName("Party").build();
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
                    .jointParty(JointParty.builder()
                            .hasJointParty(jointPartyYesNo)
                            .name(jointPartyName)
                            .build())
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
                    .jointParty(JointParty.builder()
                            .hasJointParty(YES)
                            .jointPartyAddressSameAsAppellant(YES)
                            .name(Name.builder().title("Madam").firstName("Jon").lastName("Party").build())
                            .build())
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

    private CcdNotificationWrapper buildNotificationWrapperWithOtherParty(NotificationEventType notificationEventType, List<CcdValue<OtherParty>> otherParties) {
        Appointee appointee = Appointee.builder()
                .name(Name.builder().firstName("Ap").lastName("Pointee").build())
                .address(Address.builder().line1("Appointee Line 1").town("Appointee Town").county("Appointee County").postcode("AP9 0IN").build())
                .build();
        return new CcdNotificationWrapper(
                SscsCaseDataWrapper.builder()
                        .newSscsCaseData(SscsCaseData.builder().otherParties(otherParties)
                                .appeal(Appeal.builder()
                                        .hearingType("Oral")
                                        .appellant(Appellant.builder().appointee(appointee).build())
                                        .build())
                                .subscriptions(Subscriptions.builder()
                                        .appellantSubscription(
                                                Subscription.builder()
                                                        .email("appellant@test.com")
                                                        .subscribeEmail("Yes")
                                                        .build()
                                        )
                                        .build())
                                .build())
                        .notificationEventType(notificationEventType)
                        .build());

    }

    private List<CcdValue<OtherParty>> buildOtherPartyData(boolean sendNewOtherPartyNotification, boolean hasAppointee, boolean hasRep) {
        return List.of(CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id("1")
                        .otherPartySubscription(Subscription.builder().email("other@party").subscribeEmail("Yes").build())
                        .sendNewOtherPartyNotification(sendNewOtherPartyNotification ? YesNo.YES : null)
                        .isAppointee(hasAppointee ? YesNo.YES.getValue() : YesNo.NO.getValue())
                        .appointee(Appointee.builder()
                                .id("2")
                                .name(Name.builder()
                                        .firstName("First")
                                        .lastName("Last")
                                        .build())
                                .build())
                        .rep(Representative.builder()
                                .id("3")
                                .hasRepresentative(hasRep ? YesNo.YES.getValue() : YesNo.NO.getValue())
                                .name(Name.builder()
                                        .firstName("First")
                                        .lastName("Last")
                                        .build())
                                .build())
                        .build())
                .build());
    }

    @Test
    @Parameters({"APPEAL_LAPSED","HMCTS_APPEAL_LAPSED","DWP_APPEAL_LAPSED","APPEAL_WITHDRAWN","EVIDENCE_RECEIVED",
        "POSTPONEMENT","HEARING_BOOKED","SYA_APPEAL_CREATED","VALID_APPEAL_CREATED",
        "RESEND_APPEAL_CREATED", "APPEAL_RECEIVED", "ADJOURNED", "ISSUE_FINAL_DECISION_WELSH",
        "PROCESS_AUDIO_VIDEO", "PROCESS_AUDIO_VIDEO_WELSH", "ACTION_POSTPONEMENT_REQUEST", "ACTION_POSTPONEMENT_REQUEST_WELSH",
        "APPEAL_DORMANT", "DWP_RESPONSE_RECEIVED", "STRUCK_OUT", "DECISION_ISSUED", "DECISION_ISSUED_WELSH", "DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH"})
    public void givenSubscriptions_shouldGetAppellantAndRepSubscriptionTypeList(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    @Parameters({"DEATH_OF_APPELLANT","PROVIDE_APPOINTEE_DETAILS"})
    public void givenSubscriptions_shouldGetAppointeeAndRepSubscriptionTypeList(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndRep(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    @Parameters({"SYA_APPEAL_CREATED, cor", "DWP_RESPONSE_RECEIVED, oral",
        "DWP_RESPONSE_RECEIVED, paper", "HMCTS_APPEAL_LAPSED, paper", "HMCTS_APPEAL_LAPSED, oral",
        "DWP_APPEAL_LAPSED, paper", "DWP_APPEAL_LAPSED, oral", "SUBSCRIPTION_UPDATED, paper",
        "VALID_APPEAL_CREATED, cor", "RESEND_APPEAL_CREATED, cor"})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointee(NotificationEventType notificationEventType, String hearingType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters({"APPEAL_LAPSED, paper", "APPEAL_LAPSED, oral", "EVIDENCE_REMINDER, oral", "EVIDENCE_REMINDER, paper",
        "APPEAL_DORMANT, paper", "APPEAL_DORMANT, oral", "ADJOURNED, paper", "ADJOURNED, oral", "POSTPONEMENT, paper", "POSTPONEMENT, oral",
        "EVIDENCE_RECEIVED, paper", "EVIDENCE_RECEIVED, oral", "APPEAL_WITHDRAWN, paper", "STRUCK_OUT, oral", "STRUCK_OUT, paper", "DIRECTION_ISSUED, oral", "DIRECTION_ISSUED, paper",
        "DIRECTION_ISSUED_WELSH, oral", "DIRECTION_ISSUED_WELSH, paper", "DWP_UPLOAD_RESPONSE, paper",
        "PROCESS_AUDIO_VIDEO, oral", "PROCESS_AUDIO_VIDEO_WELSH, paper", "ACTION_POSTPONEMENT_REQUEST, paper", "ACTION_POSTPONEMENT_REQUEST_WELSH, paper",
        "DWP_UPLOAD_RESPONSE, oral", "HEARING_BOOKED, oral", "HEARING_BOOKED, paper",  "HEARING_REMINDER, oral", "HEARING_REMINDER, paper",
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

    @Test
    public void givenNoOtherPartyInTheCase_thenReturnEmptySubscription() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, null);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions();
        Assert.assertTrue(subsWithTypeList.isEmpty());
    }

    @Test
    public void givenUpdateOtherPartyDataEventAndSendNotificationFlagIsNotSetInOtherParty_thenReturnEmptySubscription() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, buildOtherPartyData(false, true, true));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions();
        Assert.assertTrue(subsWithTypeList.isEmpty());
    }

    @Test
    public void givenUpdateOtherPartyDataEventAndSendNotificationFlagIsSetInOtherPartyWithAppointee_thenReturnAllOtherPartySubscription() {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, buildOtherPartyData(true, true, true));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions();
        Assert.assertEquals(2, subsWithTypeList.size());
        Assert.assertEquals(2, subsWithTypeList.get(0).getPartyId());
        Assert.assertEquals(3, subsWithTypeList.get(1).getPartyId());
    }

    @Test
    public void givenUpdateOtherPartyDataEventAndSendNotificationFlagIsSetInOtherPartyWithNoAppointee_thenReturnAllOtherPartySubscription() {

        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(UPDATE_OTHER_PARTY_DATA, buildOtherPartyData(true, false, true));
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getOtherPartySubscriptions();
        Assert.assertEquals(2, subsWithTypeList.size());
        Assert.assertEquals(1, subsWithTypeList.get(0).getPartyId());
        Assert.assertEquals(3, subsWithTypeList.get(1).getPartyId());
    }

    @Test
    @Parameters({
        "ADJOURNED",
        "ADJOURNED",
        "ADMIN_APPEAL_WITHDRAWN",
        "APPEAL_DORMANT",
        "APPEAL_LAPSED",
        "APPEAL_WITHDRAWN",
        "DECISION_ISSUED",
        "DECISION_ISSUED_WELSH",
        "DIRECTION_ISSUED",
        "DIRECTION_ISSUED_WELSH",
        "DWP_APPEAL_LAPSED",
        "DWP_RESPONSE_RECEIVED",
        "DWP_UPLOAD_RESPONSE",
        "EVIDENCE_RECEIVED",
        "EVIDENCE_REMINDER",
        "HEARING_BOOKED",
        "HEARING_REMINDER",
        "HMCTS_APPEAL_LAPSED",
        "ISSUE_ADJOURNMENT_NOTICE",
        "ISSUE_ADJOURNMENT_NOTICE_WELSH",
        "ISSUE_FINAL_DECISION",
        "ISSUE_FINAL_DECISION_WELSH",
        "NON_COMPLIANT",
        "POSTPONEMENT",
        "PROCESS_AUDIO_VIDEO",
        "PROCESS_AUDIO_VIDEO_WELSH",
        "REQUEST_INFO_INCOMPLETE",
        "STRUCK_OUT",
        "SUBSCRIPTION_CREATED",
        "SUBSCRIPTION_OLD",
        "SUBSCRIPTION_UPDATED"})
    public void givenNotificationForOtherParty_thenReturnAllOtherPartySubscription(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildNotificationWrapperWithOtherParty(notificationEventType, buildOtherPartyData(false, false, false));

        ccdNotificationWrapper.getNewSscsCaseData().setInformationFromPartySelected(new DynamicList(new DynamicListItem(PartyItemList.APPELLANT.getCode(), PartyItemList.APPELLANT.getLabel()), new ArrayList<>()));

        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2, subsWithTypeList.size());
        Assert.assertEquals(1, subsWithTypeList.get(1).getPartyId());
    }

    @SuppressWarnings({"unused"})
    private Object[] getEventTypeFilteredWithAppellant() {
        return Arrays.stream(values())
            .filter(type -> !(type.equals(APPEAL_LAPSED)
                || type.equals(HMCTS_APPEAL_LAPSED)
                || type.equals(DWP_APPEAL_LAPSED)
                || type.equals(APPEAL_WITHDRAWN)
                || type.equals(EVIDENCE_RECEIVED)
                || type.equals(CASE_UPDATED)
                || type.equals(APPEAL_DORMANT)
                || type.equals(ADJOURNED)
                || type.equals(APPEAL_RECEIVED)
                || type.equals(POSTPONEMENT)
                || type.equals(SUBSCRIPTION_UPDATED)
                || type.equals(HEARING_BOOKED)
                || type.equals(STRUCK_OUT)
                || type.equals(REQUEST_INFO_INCOMPLETE)
                || type.equals(NON_COMPLIANT)
                || type.equals(REVIEW_CONFIDENTIALITY_REQUEST)
                || type.equals(ACTION_HEARING_RECORDING_REQUEST)
                || type.equals(UPDATE_OTHER_PARTY_DATA)
            )).toArray();
    }

}
