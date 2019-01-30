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
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, null, Representative.builder().hasRepresentative("Yes").build());
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventTypeWithAppointee(NotificationEventType notificationEventType) {
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, Appointee.builder().name(Name.builder().firstName("John").lastName("Doe").build()).build(), null);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventType(NotificationEventType notificationEventType) {
        return buildCcdNotificationWrapperBasedOnEventType(notificationEventType, null, null);
    }

    private CcdNotificationWrapper buildCcdNotificationWrapperBasedOnEventType(NotificationEventType notificationEventType, Appointee appointee, Representative representative) {
        Appellant appellant = Appellant.builder().build();
        Subscription appointeeSubscription = null;
        if (null != appointee) {
            appellant.setAppointee(appointee);
            appointeeSubscription = Subscription.builder().build();
        }

        Subscription repSubscription = null;
        if (null != representative) {
            representative = Representative.builder()
                .hasRepresentative("Yes")
                .name(Name.builder().firstName("Joe").lastName("Bloggs").build())
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                .build();
            repSubscription = Subscription.builder().build();
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

    @Test
    @Parameters({"APPEAL_LAPSED_NOTIFICATION","APPEAL_WITHDRAWN_NOTIFICATION","EVIDENCE_RECEIVED_NOTIFICATION","POSTPONEMENT_NOTIFICATION","HEARING_BOOKED_NOTIFICATION","SYA_APPEAL_CREATED_NOTIFICATION", "RESEND_APPEAL_CREATED_NOTIFICATION", "APPEAL_RECEIVED_NOTIFICATION", "ADJOURNED_NOTIFICATION", "APPEAL_DORMANT_NOTIFICATION", "INTERLOC_VALID_APPEAL"})
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
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(notificationEventType, Appointee.builder().name(Name.builder().firstName("John").lastName("Doe").build()).build(), Representative.builder().build());
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(2,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
        Assert.assertEquals(SubscriptionType.REPRESENTATIVE, subsWithTypeList.get(1).getSubscriptionType());
    }

    @Test
    @Parameters({"SYA_APPEAL_CREATED_NOTIFICATION","APPEAL_RECEIVED_NOTIFICATION"})
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithAppointee(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventTypeWithAppointee(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPOINTEE, subsWithTypeList.get(0).getSubscriptionType());
    }

    @Test
    @Parameters(method = "getEventTypeFilteredOnReps")
    public void givenSubscriptions_shouldGetSubscriptionTypeListWithoutReps(NotificationEventType notificationEventType) {
        ccdNotificationWrapper = buildCcdNotificationWrapperBasedOnEventType(notificationEventType);
        List<SubscriptionWithType> subsWithTypeList = ccdNotificationWrapper.getSubscriptionsBasedOnNotificationType();
        Assert.assertEquals(1,subsWithTypeList.size());
        Assert.assertEquals(SubscriptionType.APPELLANT, subsWithTypeList.get(0).getSubscriptionType());
    }

    private Object[] getEventTypeFilteredOnReps() {
        return Arrays.stream(NotificationEventType.values())
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