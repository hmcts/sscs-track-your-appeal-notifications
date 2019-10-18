package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ADMIN_APPEAL_WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.SUBSCRIPTION_UPDATED_NOTIFICATION;

import java.util.ArrayList;
import java.util.List;
import junitparams.Parameters;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasons;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.service.notify.NotificationClientException;

public class NotificationServiceForSubscriptionUpdatedTest extends NotificationServiceBase {

    @Value("${notification.appealReceived.appellant.emailId}")
    private String appealReceivedAppellantEmailId;

    @Value("${notification.appealReceived.appellant.smsId}")
    private String appealReceivedAppellantSmsId;

    @Value("${notification.appealReceived.representative.emailId}")
    private String appealReceivedRepresentativeEmailId;

    @Value("${notification.appealReceived.representative.smsId}")
    private String appealReceivedRepresentativeSmsId;

    @Value("${notification.appealReceived.appointee.emailId}")
    private String appealReceivedAppointeeEmailId;

    @Value("${notification.appealReceived.appointee.smsId}")
    private String appealReceivedAppointeeSmsId;

    @Value("${notification.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailId;

    @Value("${notification.subscriptionUpdated.smsId}")
    private String subscriptionUpdatedSmsId;

    @Value("${notification.subscriptionCreated.appellant.smsId}")
    private String subscriptionCreatedAppellantSmsId;

    @Value("${notification.subscriptionCreated.appointee.smsId}")
    private String subscriptionCreatedAppointeeSmsId;

    @Value("${notification.subscriptionCreated.representative.smsId}")
    private String subscriptionCreatedRepresentativeSmsId;

    @Value("${notification.subscriptionOld.emailId}")
    private String subscriptionOldEmailId;

    @Value("${notification.subscriptionOld.smsId}")
    private String subscriptionOldSmsId;

    @Test
    @Ignore
    public void adminAppealWithdrawalWhenNoSubscription_shouldSendMandatoryLetter() throws Exception {
        SscsCaseData newSscsCaseData = getSscsCaseData(null);
        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, null, ADMIN_APPEAL_WITHDRAWN);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(getNotificationSender(), times(0)).sendEmail(any(), any(), any(), any(), any(), any());
        verify(getNotificationSender(), times(0))
            .sendSms(any(), any(), any(), any(), any(), any(), any());


    }

    @Test
    @Parameters({"appellant", "representative", "appointee"})
    public void unsubscribeFromSmsAndEmail_doesNotSendAnyEmailsOrSms(String who) {
        Subscription newSubscription = getSubscription().toBuilder()
            .subscribeEmail(NotificationServiceBase.NO)
            .subscribeSms(NotificationServiceBase.NO)
            .build();
        doUnsubscribeWithAssertions(newSubscription, who);
    }

    @Test
    @Parameters({"appellant", "representative", "appointee"})
    public void unsubscribeFromEmail_doesNotSendAnyEmailsOrSms(String who) {
        Subscription newSubscription = getSubscription().toBuilder().subscribeEmail(NotificationServiceBase.NO).subscribeSms(NotificationServiceBase.YES).build();
        doUnsubscribeWithAssertions(newSubscription, who);
    }

    @Test
    @Parameters({"appellant", "representative", "appointee"})
    public void unsubscribeFromSms_doesNotSendAnyEmailsOrSms(String who) {
        Subscription newSubscription = getSubscription().toBuilder().subscribeEmail(NotificationServiceBase.YES).subscribeSms(NotificationServiceBase.NO).build();
        doUnsubscribeWithAssertions(newSubscription, who);
    }

    @Test
    @Parameters({"appellant", "representative", "appointee"})
    public void subscribeEmail_willSendSubscriptionEmail_and_ResendLastEvent(String who) throws NotificationClientException {
        Subscription newSubscription = getSubscription().toBuilder().email(NotificationServiceBase.EMAIL_TEST_2).subscribeEmail(NotificationServiceBase.YES).subscribeSms(NotificationServiceBase.NO).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationServiceBase.NO).subscribeSms(NotificationServiceBase.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, who);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, who);
        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(getNotificationSender()).sendEmail(eq(subscriptionUpdatedEmailId), eq(newSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendEmail(eq(getAppealReceivedEmailId(who)), eq(newSubscription.getEmail()), any(), any(), any(), any());
        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    public void subscribeMobileForAppellant_willSendSubscriptionSms_and_ResendLastEvent() throws NotificationClientException {
        String appellant = "appellant";
        Subscription newSubscription = getSubscription().toBuilder().mobile(NotificationServiceBase.MOBILE_NUMBER_2).subscribeEmail(NotificationServiceBase.NO).subscribeSms(NotificationServiceBase.YES).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationServiceBase.NO).subscribeSms(NotificationServiceBase.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, appellant);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appellant);
        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(getNotificationSender()).sendSms(eq(subscriptionUpdatedSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(getAppealReceivedSmsId(appellant)), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    public void subscribeMobileForAppointee_willSendSubscriptionSms_and_ResendLastEvent() throws NotificationClientException {
        String appointee = "appointee";
        Subscription newSubscription = getSubscription().toBuilder().mobile(NotificationServiceBase.MOBILE_NUMBER_2).subscribeEmail(NotificationServiceBase.NO).subscribeSms(NotificationServiceBase.YES).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationServiceBase.NO).subscribeSms(NotificationServiceBase.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, appointee);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appointee);
        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(getNotificationSender()).sendSms(eq(subscriptionUpdatedSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(getAppealReceivedSmsId(appointee)), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    public void subscribeMobileForRepresentative_willSendSubscriptionSms_and_ResendLastEvent() throws NotificationClientException {
        String representative = "representative";
        Subscription newSubscription = getSubscription().toBuilder().mobile(NotificationServiceBase.MOBILE_NUMBER_2).subscribeEmail(NotificationServiceBase.NO).subscribeSms(NotificationServiceBase.YES).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationServiceBase.NO).subscribeSms(NotificationServiceBase.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, representative);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, representative);
        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(getNotificationSender()).sendSms(eq(subscriptionCreatedRepresentativeSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(getAppealReceivedSmsId(representative)), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    public void subscribeMobileAndEmailForAppellant_willSendSubscriptionEmailAndSms_and_ResendLastEvent() throws NotificationClientException {
        String appellant = "appellant";
        Subscription newSubscription = getSubscription().toBuilder().email(NotificationServiceBase.EMAIL_TEST_2).mobile(NotificationServiceBase.MOBILE_NUMBER_2).subscribeEmail(NotificationServiceBase.YES).subscribeSms(NotificationServiceBase.YES).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationServiceBase.NO).subscribeSms(NotificationServiceBase.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, appellant);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appellant);
        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(getNotificationSender()).sendEmail(eq(subscriptionUpdatedEmailId), eq(newSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendEmail(eq(getAppealReceivedEmailId(appellant)), eq(newSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(subscriptionCreatedAppellantSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(getAppealReceivedSmsId(appellant)), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    public void subscribeMobileAndEmailForAppointee_willSendSubscriptionEmailAndSms_and_ResendLastEvent() throws NotificationClientException {
        String appointee = "appointee";
        Subscription newSubscription = getSubscription().toBuilder().email(NotificationServiceBase.EMAIL_TEST_2).mobile(NotificationServiceBase.MOBILE_NUMBER_2).subscribeEmail(NotificationServiceBase.YES).subscribeSms(NotificationServiceBase.YES).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationServiceBase.NO).subscribeSms(NotificationServiceBase.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, appointee);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appointee);
        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(getNotificationSender()).sendEmail(eq(subscriptionUpdatedEmailId), eq(newSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendEmail(eq(getAppealReceivedEmailId(appointee)), eq(newSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(subscriptionCreatedAppointeeSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(getAppealReceivedSmsId(appointee)), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    public void subscribeMobileAndEmailForRepresentative_willSendSubscriptionEmailAndSms_and_ResendLastEvent() throws NotificationClientException {
        String representative = "representative";
        Subscription newSubscription = getSubscription().toBuilder().email(NotificationServiceBase.EMAIL_TEST_2).mobile(NotificationServiceBase.MOBILE_NUMBER_2).subscribeEmail(NotificationServiceBase.YES).subscribeSms(NotificationServiceBase.YES).build();
        Subscription oldSubscription = getSubscription().toBuilder().subscribeEmail(NotificationServiceBase.NO).subscribeSms(NotificationServiceBase.NO).build();
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, representative);
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, representative);
        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));
        verify(getNotificationSender()).sendEmail(eq(subscriptionUpdatedEmailId), eq(newSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendEmail(eq(getAppealReceivedEmailId(representative)), eq(newSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(subscriptionCreatedRepresentativeSmsId), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(getAppealReceivedSmsId(representative)), eq(newSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    @Parameters({"appellant", "representative", "appointee"})
    public void changeEmail_willSendChangeEmailToOldAndNewEmail(String who) throws NotificationClientException {
        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), who);

        Subscription oldSubscription = getSubscription().toBuilder().email(NotificationServiceBase.EMAIL_TEST_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, who);

        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(getNotificationSender()).sendEmail(eq(subscriptionUpdatedEmailId), eq(getSubscription().getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendEmail(eq(subscriptionOldEmailId), eq(oldSubscription.getEmail()), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    public void changeMobileForAppellant_willSendChangeSmsToOldAndNewMobile() throws NotificationClientException {
        String appellant = "appellant";
        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), appellant);

        Subscription oldSubscription = getSubscription().toBuilder().mobile(NotificationServiceBase.MOBILE_NUMBER_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appellant);

        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(getNotificationSender()).sendSms(eq(subscriptionCreatedAppellantSmsId), eq(getSubscription().getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    public void changeMobileForAppointee_willSendChangeSmsToOldAndNewMobile() throws NotificationClientException {
        String appointee = "appointee";
        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), appointee);

        Subscription oldSubscription = getSubscription().toBuilder().mobile(NotificationServiceBase.MOBILE_NUMBER_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appointee);

        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(getNotificationSender()).sendSms(eq(subscriptionCreatedAppointeeSmsId), eq(getSubscription().getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    public void changeMobileForRepresentative_willSendChangeSmsToOldAndNewMobile() throws NotificationClientException {
        String representative = "representative";

        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), representative);

        Subscription oldSubscription = getSubscription().toBuilder().mobile(NotificationServiceBase.MOBILE_NUMBER_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, representative);

        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(getNotificationSender()).sendSms(eq(subscriptionCreatedRepresentativeSmsId), eq(getSubscription().getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    public void changeMobileAndEmailForAppellant_willSendChangeSmsToOldAndNewMobileAndEmail() throws NotificationClientException {
        String appellant = "appellant";
        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), appellant);

        Subscription oldSubscription = getSubscription().toBuilder().mobile(NotificationServiceBase.MOBILE_NUMBER_2).email(NotificationServiceBase.EMAIL_TEST_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appellant);

        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(getNotificationSender()).sendEmail(eq(subscriptionUpdatedEmailId), eq(getSubscription().getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendEmail(eq(subscriptionOldEmailId), eq(oldSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(subscriptionCreatedAppellantSmsId), eq(getSubscription().getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    public void changeMobileAndEmailForAppointee_willSendChangeSmsToOldAndNewMobileAndEmail() throws NotificationClientException {
        String appointee = "appointee";
        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), appointee);

        Subscription oldSubscription = getSubscription().toBuilder().mobile(NotificationServiceBase.MOBILE_NUMBER_2).email(NotificationServiceBase.EMAIL_TEST_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, appointee);

        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(getNotificationSender()).sendEmail(eq(subscriptionUpdatedEmailId), eq(getSubscription().getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendEmail(eq(subscriptionOldEmailId), eq(oldSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(subscriptionCreatedAppointeeSmsId), eq(getSubscription().getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationSender());
    }

    @Test
    public void changeMobileAndEmailForRepresentative_willSendChangeSmsToOldAndNewMobileAndEmail() throws NotificationClientException {
        String representative = "representative";
        SscsCaseData newSscsCaseData = getSscsCaseData(getSubscription(), representative);

        Subscription oldSubscription = getSubscription().toBuilder().mobile(NotificationServiceBase.MOBILE_NUMBER_2).email(NotificationServiceBase.EMAIL_TEST_2).build();
        SscsCaseData oldSscsCaseData = getSscsCaseData(oldSubscription, representative);

        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verify(getNotificationSender()).sendEmail(eq(subscriptionUpdatedEmailId), eq(getSubscription().getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendEmail(eq(subscriptionOldEmailId), eq(oldSubscription.getEmail()), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(subscriptionCreatedRepresentativeSmsId), eq(getSubscription().getMobile()), any(), any(), any(), any(), any());
        verify(getNotificationSender()).sendSms(eq(subscriptionOldSmsId), eq(oldSubscription.getMobile()), any(), any(), any(), any(), any());

        verifyNoMoreInteractions(getNotificationSender());
    }

    private void doUnsubscribeWithAssertions(Subscription newSubscription, String who) {
        SscsCaseData newSscsCaseData = getSscsCaseData(newSubscription, who);
        SscsCaseData oldSscsCaseData = getSscsCaseData(getSubscription(), who);

        SscsCaseDataWrapper wrapper = getSscsCaseDataWrapper(newSscsCaseData, oldSscsCaseData, SUBSCRIPTION_UPDATED_NOTIFICATION);

        getNotificationService().manageNotificationAndSubscription(new CcdNotificationWrapper(wrapper));

        verifyNoMoreInteractions(getNotificationSender());
    }

    private SscsCaseDataWrapper getSscsCaseDataWrapper(SscsCaseData newSscsCaseData, SscsCaseData oldSscsCaseData,
                                                       NotificationEventType subscriptionUpdatedNotification) {
        return SscsCaseDataWrapper.builder()
            .newSscsCaseData(newSscsCaseData)
            .oldSscsCaseData(oldSscsCaseData)
            .notificationEventType(subscriptionUpdatedNotification).build();
    }

    private String getAppealReceivedEmailId(String who) {
        if (who.equals("appellant")) {
            return appealReceivedAppellantEmailId;
        } else if (who.equals("representative")) {
            return appealReceivedRepresentativeEmailId;
        }
        return appealReceivedAppointeeEmailId;
    }

    private String getAppealReceivedSmsId(String who) {
        if (who.equals("appellant")) {
            return appealReceivedAppellantSmsId;
        } else if (who.equals("representative")) {
            return appealReceivedRepresentativeSmsId;
        }
        return appealReceivedAppointeeSmsId;
    }

    private SscsCaseData getSscsCaseData(Subscription subscription, String who) {
        if (who.equals("appellant")) {
            return getSscsCaseData(subscription);
        } else if (who.equals("representative")) {
            return getSscsCaseDataForRep(subscription);
        }
        return getSscsCaseDataForAppointee(subscription);
    }

    private SscsCaseData getSscsCaseData(Subscription subscription) {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(NotificationServiceBase.DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        return SscsCaseData.builder().ccdCaseId(NotificationServiceBase.CASE_ID).events(events)
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(NotificationServiceBase.DATE).dwpIssuingOffice("office").build())
                .appealReasons(AppealReasons.builder().build())
                .rep(Representative.builder()
                    .hasRepresentative(NotificationServiceBase.YES)
                    .name(Name.builder().firstName("Rep").lastName("lastName").build())
                    .contact(Contact.builder().email(NotificationServiceBase.EMAIL_TEST_2).phone(NotificationServiceBase.MOBILE_NUMBER_2).build())
                    .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                    .build())
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("firstName").lastName("lastName").build())
                    .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .contact(Contact.builder().email(NotificationServiceBase.EMAIL_TEST_1).phone(NotificationServiceBase.MOBILE_NUMBER_1).build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build()).build())
                .hearingType(AppealHearingType.ORAL.name())
                .benefitType(BenefitType.builder().code(Benefit.PIP.name()).build())
                .hearingOptions(HearingOptions.builder()
                    .wantsToAttend(NotificationServiceBase.YES)
                    .build())
                .build())
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(subscription)
                .representativeSubscription(getSubscription().toBuilder().tya("REP_TYA").build())
                .build())
            .caseReference(NotificationServiceBase.CASE_REFERENCE).build();
    }

    private SscsCaseData getSscsCaseDataForRep(Subscription subscription) {
        Subscription appellantSubscription = getSubscription().toBuilder().tya("APPELLANT_TYA").build();
        SscsCaseData sscsCaseData = getSscsCaseData(appellantSubscription);
        return sscsCaseData.toBuilder()
            .subscriptions(sscsCaseData.getSubscriptions().toBuilder().representativeSubscription(subscription).build())
            .build();
    }

    private SscsCaseData getSscsCaseDataForAppointee(Subscription subscription) {
        SscsCaseData sscsCaseData = getSscsCaseData(subscription);
        return sscsCaseData.toBuilder()
            .appeal(sscsCaseData.getAppeal().toBuilder().appellant(sscsCaseData.getAppeal().getAppellant()

                .toBuilder()
                .appointee(Appointee.builder()
                    .name(Name.builder().firstName("Appoin").lastName("Tee").build())
                    .address(sscsCaseData.getAppeal().getAppellant().getAddress())
                    .contact(sscsCaseData.getAppeal().getAppellant().getContact())
                    .identity(sscsCaseData.getAppeal().getAppellant().getIdentity())
                    .build())
                .build()).build())
            .subscriptions(sscsCaseData.getSubscriptions().toBuilder()
                .appellantSubscription(null)
                .appointeeSubscription(subscription).build())
            .build();
    }
}
