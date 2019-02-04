package uk.gov.hmcts.reform.sscs.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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
    @Parameters({"paper, PAPER", "null, REGULAR", "oral, ORAL", "cor, ONLINE"})
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
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, false, true);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithAppointee(NotificationEventType notificationEventType) {
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, true, false);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventType(NotificationEventType notificationEventType, Boolean addAppointee, Boolean addRep) {
        Appointee appointee = null;
        if (addAppointee) {
            appointee = Appointee.builder()
                .name(Name.builder().firstName("Ap").lastName("Pointee").build())
                .address(Address.builder().line1("Appointee Line 1").town("Appointee Town").county("Appointee County").postcode("AP9 0IN").build())
                .build();
        }

        Representative rep = null;
        if (addRep) {
            rep = Representative.builder()
                .hasRepresentative("Yes")
                .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                .build();
        }

        return new CcdNotificationWrapper(
            SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                    .appeal(Appeal.builder()
                        .hearingType("cor")
                        .appellant(Appellant.builder().appointee(appointee).build())
                        .rep(rep)
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder().build())
                        .representativeSubscription(Subscription.builder().build())
                        .build())
                    .build())
                .notificationEventType(notificationEventType)
                .build()

        );
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithAppointee(NotificationEventType notificationEventType, String hearingType) {
        Appointee appointee = Appointee.builder()
            .name(Name.builder().firstName("Ap").lastName("Pointee").build())
            .address(Address.builder().line1("Appointee Line 1").town("Appointee Town").county("Appointee County").postcode("AP9 0IN").build())
            .build();

        return new CcdNotificationWrapper(
            SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                    .appeal(Appeal.builder()
                        .hearingType(hearingType)
                        .appellant(Appellant.builder().appointee(Appointee.builder().name(Name.builder().firstName("TEST")
                            .lastName("TEST").build()).build()).build())
                        .build())
                    .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder().build())
                        .appointeeSubscription(Subscription.builder().build())
                        .build())
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
                        .appointeeSubscription(Subscription.builder().build())
                        .build())
                    .build())
                .notificationEventType(notificationEventType)
                .build()
        );
    }

    @Test
    @Parameters({"APPEAL_LAPSED_NOTIFICATION","APPEAL_WITHDRAWN_NOTIFICATION","EVIDENCE_RECEIVED_NOTIFICATION","POSTPONEMENT_NOTIFICATION","HEARING_BOOKED_NOTIFICATION","SYA_APPEAL_CREATED_NOTIFICATION", "RESEND_APPEAL_CREATED_NOTIFICATION", "APPEAL_RECEIVED_NOTIFICATION", "ADJOURNED_NOTIFICATION", "APPEAL_DORMANT_NOTIFICATION"})
    public void givenSubscriptions_shouldGetAppellantAndRepSubscriptionTypeList(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    @Parameters({"INTERLOC_VALID_APPEAL"})
    public void givenSubscriptions_shouldGetAppointeeAndRepSubscriptionTypeList(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(notificationEventType, true, true);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    @Parameters({"SYA_APPEAL_CREATED_NOTIFICATION, cor", "DWP_RESPONSE_RECEIVED_NOTIFICATION, oral"})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointee(NotificationEventType notificationEventType, String hearingType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointee(notificationEventType, hearingType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters({"SYA_APPEAL_CREATED_NOTIFICATION"})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithoutAppointee(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithoutAppointee(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters(method = "getEventTypeFilteredOnReps")
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithoutReps(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithRep(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    @SuppressWarnings({"unused"})
    private Object[] getEventTypeFilteredOnReps() {
        return Arrays.stream(values())
            .filter(type -> !(type.equals(APPEAL_LAPSED_NOTIFICATION)
                || type.equals(APPEAL_WITHDRAWN_NOTIFICATION)
                || type.equals(EVIDENCE_RECEIVED_NOTIFICATION)
                || type.equals(SYA_APPEAL_CREATED_NOTIFICATION)
                || type.equals(INTERLOC_VALID_APPEAL)
                || type.equals(RESEND_APPEAL_CREATED_NOTIFICATION)
                || type.equals(APPEAL_DORMANT_NOTIFICATION)
                || type.equals(ADJOURNED_NOTIFICATION)
                || type.equals(APPEAL_RECEIVED_NOTIFICATION)
                || type.equals(POSTPONEMENT_NOTIFICATION)
                || type.equals(HEARING_BOOKED_NOTIFICATION)
            )).toArray();
    }

}