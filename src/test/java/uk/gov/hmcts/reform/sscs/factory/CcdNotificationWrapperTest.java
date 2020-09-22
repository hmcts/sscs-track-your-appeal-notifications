package uk.gov.hmcts.reform.sscs.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

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

@RunWith(JUnitParamsRunner.class)
public class CcdNotificationWrapperTest {

    private CcdNotificationWrapper ccdNotificationWrapper;

    @Test
    @Parameters({"paper, PAPER", "oral, ORAL", "cor, ONLINE"})
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
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, null, Representative.builder().hasRepresentative("Yes").build());
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventType(NotificationEventType notificationEventType) {
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, null, null);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventType(NotificationEventType notificationEventType, Appointee appointee, Representative representative) {
        Appellant appellant = Appellant.builder().build();
        Subscription appointeeSubscription = null;
        if (null != appointee) {
            appellant.setAppointee(appointee);
            appointeeSubscription = Subscription.builder()
                .email("appointee@test.com")
                .subscribeEmail("Yes")
                .build();
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
        }

        return new CcdNotificationWrapper(
            SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                    .appeal(Appeal.builder()
                        .appellant(appellant)
                        .hearingType("cor")
                        .rep(representative)
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder().build())
                        .representativeSubscription(repSubscription)
                        .appointeeSubscription(appointeeSubscription)
                        .build())
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
                        .jointParty("yes")
                        .jointPartyAddressSameAsAppellant("yes")
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

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithoutAppointee(NotificationEventType notificationEventType) {
        return new CcdNotificationWrapper(
            SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                    .appeal(Appeal.builder()
                        .hearingType("cor")
                        .appellant(Appellant.builder().appointee(Appointee.builder().name(Name.builder().build()).build()).build())
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder().build())
                        .build())
                    .build())
                .notificationEventType(notificationEventType)
                .build()
        );
    }

    @Test
    @Parameters({"APPEAL_LAPSED_NOTIFICATION","HMCTS_APPEAL_LAPSED_NOTIFICATION","DWP_APPEAL_LAPSED_NOTIFICATION","APPEAL_WITHDRAWN_NOTIFICATION","EVIDENCE_RECEIVED_NOTIFICATION",
            "POSTPONEMENT_NOTIFICATION","HEARING_BOOKED_NOTIFICATION","SYA_APPEAL_CREATED_NOTIFICATION","VALID_APPEAL_CREATED",
            "RESEND_APPEAL_CREATED_NOTIFICATION", "APPEAL_RECEIVED_NOTIFICATION", "ADJOURNED_NOTIFICATION",
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
            "VALID_APPEAL_CREATED, cor", "RESEND_APPEAL_CREATED_NOTIFICATION, cor",
             "STRUCK_OUT, paper"})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointee(NotificationEventType notificationEventType, String hearingType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters({"APPEAL_LAPSED_NOTIFICATION, paper", "APPEAL_LAPSED_NOTIFICATION, oral", "EVIDENCE_REMINDER_NOTIFICATION, oral", "EVIDENCE_REMINDER_NOTIFICATION, paper",
            "APPEAL_DORMANT_NOTIFICATION, paper", "APPEAL_DORMANT_NOTIFICATION, oral", "ADJOURNED_NOTIFICATION, paper", "ADJOURNED_NOTIFICATION, oral", "POSTPONEMENT_NOTIFICATION, paper", "POSTPONEMENT_NOTIFICATION, oral",
            "EVIDENCE_RECEIVED_NOTIFICATION, paper", "EVIDENCE_RECEIVED_NOTIFICATION, oral", "APPEAL_WITHDRAWN_NOTIFICATION, paper",
            "HEARING_BOOKED_NOTIFICATION, oral", "HEARING_BOOKED_NOTIFICATION, paper",  "HEARING_REMINDER_NOTIFICATION, oral", "HEARING_REMINDER_NOTIFICATION, paper"})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointeeAndJointParty(NotificationEventType notificationEventType, String hearingType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2, subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    @Parameters({"SYA_APPEAL_CREATED_NOTIFICATION", "VALID_APPEAL_CREATED", "RESEND_APPEAL_CREATED_NOTIFICATION"})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithoutAppointee(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithoutAppointee(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters(method = "getEventTypeFilteredOnReps")
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithoutReps(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters({"DIRECTION_ISSUED, paper, PROVIDE_INFORMATION", "DIRECTION_ISSUED, oral, PROVIDE_INFORMATION",
            "DIRECTION_ISSUED, paper, APPEAL_TO_PROCEED", "DIRECTION_ISSUED, oral, APPEAL_TO_PROCEED",
            "DIRECTION_ISSUED, paper, GRANT_EXTENSION", "DIRECTION_ISSUED, oral, GRANT_EXTENSION",
            "DIRECTION_ISSUED, paper, REFUSE_EXTENSION", "DIRECTION_ISSUED, oral, REFUSE_EXTENSION",})
    public void testJointPartyDirections(NotificationEventType notificationEventType, String hearingType, DirectionType directionType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType, directionType);
        if (DirectionType.PROVIDE_INFORMATION.equals(directionType)) {
            Assert.assertTrue(ccdNotificationWrapper.directionForJointParty());
        } else {
            Assert.assertFalse(ccdNotificationWrapper.directionForJointParty());
        }
    }

    @Test
    @Parameters({"DIRECTION_ISSUED, paper, PROVIDE_INFORMATION", "DIRECTION_ISSUED, oral, PROVIDE_INFORMATION",
            "DIRECTION_ISSUED, paper, APPEAL_TO_PROCEED", "DIRECTION_ISSUED, oral, APPEAL_TO_PROCEED",
            "DIRECTION_ISSUED, paper, GRANT_EXTENSION", "DIRECTION_ISSUED, oral, GRANT_EXTENSION",
            "DIRECTION_ISSUED, paper, REFUSE_EXTENSION", "DIRECTION_ISSUED, oral, REFUSE_EXTENSION",})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointeeAndJointPartyDirection(NotificationEventType notificationEventType, String hearingType, DirectionType directionType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointeeAndJointParty(notificationEventType, hearingType, directionType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();

        if (DirectionType.PROVIDE_INFORMATION.equals(directionType)) {
            Assert.assertEquals(2, subsWithTypeList.size());
            Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
            Assert.assertEquals(SubscriptionType.JOINT_PARTY, subsWithTypeList.get(1).getSubscriptionType());
        } else {
            Assert.assertEquals(1, subsWithTypeList.size());
            Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        }
    }

    @SuppressWarnings({"unused"})
    private Object[] getEventTypeFilteredOnReps() {
        return Arrays.stream(values())
            .filter(type -> !(type.equals(APPEAL_LAPSED_NOTIFICATION)
                || type.equals(HMCTS_APPEAL_LAPSED_NOTIFICATION)
                || type.equals(DWP_APPEAL_LAPSED_NOTIFICATION)
                || type.equals(APPEAL_WITHDRAWN_NOTIFICATION)
                || type.equals(EVIDENCE_RECEIVED_NOTIFICATION)
                || type.equals(SYA_APPEAL_CREATED_NOTIFICATION)
                || type.equals(CASE_UPDATED)
                || type.equals(RESEND_APPEAL_CREATED_NOTIFICATION)
                || type.equals(APPEAL_DORMANT_NOTIFICATION)
                || type.equals(ADJOURNED_NOTIFICATION)
                || type.equals(APPEAL_RECEIVED_NOTIFICATION)
                || type.equals(POSTPONEMENT_NOTIFICATION)
                || type.equals(SUBSCRIPTION_UPDATED_NOTIFICATION)
                || type.equals(HEARING_BOOKED_NOTIFICATION)
                || type.equals(STRUCK_OUT)
                || type.equals(VALID_APPEAL_CREATED)
                || type.equals(REQUEST_INFO_INCOMPLETE)
                || type.equals(NON_COMPLIANT_NOTIFICATION)
            )).toArray();
    }

}
