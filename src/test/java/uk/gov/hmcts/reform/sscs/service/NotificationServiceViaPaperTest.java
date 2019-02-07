package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED_NOTIFICATION;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.Destination;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Reference;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class NotificationServiceViaPaperTest {

    private static final String LETTER = "Letter";
    private static final String EMAIL = "Email";
    private static final String SMS = "SMS";
    private static final String VIA_PAPER = "Paper";
    private static final String VIA_ONLINE = "Online";

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private NotificationFactory factory;

    @Mock
    private ReminderService reminderService;

    @Mock
    private NotificationValidService notificationValidService;

    @Mock
    private NotificationHandler notificationHandler;

    @Mock
    private OutOfHoursCalculator outOfHoursCalculator;

    @Mock
    private NotificationConfig notificationConfig;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private SscsGeneratePdfService sscsGeneratePdfService;

    @Mock
    private IdamService idamService;

    @Before
    public void setup() {
        initMocks(this);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);

        String authHeader = "authHeader";
        String serviceAuthHeader = "serviceAuthHeader";
        IdamTokens idamTokens = IdamTokens.builder().idamOauth2Token(authHeader).serviceAuthorization(serviceAuthHeader).build();

        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void sendResponseReceivedLetterForAppellantPaperCase() {
        testLetter(appellantCase(VIA_PAPER), DWP_RESPONSE_RECEIVED_NOTIFICATION, 1);
    }

    @Test
    public void sendResponseReceivedLetterForAppointeePaperCase() {
        testLetter(appointeeCase(VIA_PAPER), DWP_RESPONSE_RECEIVED_NOTIFICATION, 1);
    }

    @Test
    public void sendResponseReceivedLetterForAppellantRepPaperCase() {
        testLetter(appellantRepCase(VIA_PAPER), DWP_RESPONSE_RECEIVED_NOTIFICATION, 2);
    }

    @Test
    public void sendResponseReceivedLetterForAppointeeRepPaperCase() {
        testLetter(appointeeRepCase(VIA_PAPER), DWP_RESPONSE_RECEIVED_NOTIFICATION, 2);
    }

    @Test
    public void sendResponseReceivedLetterForAppellantOnlineCase() {
        testLetter(appellantCase(VIA_ONLINE), DWP_RESPONSE_RECEIVED_NOTIFICATION, 0);
    }

    @Test
    public void sendResponseReceivedLetterForAppointeeOnlineCase() {
        testLetter(appointeeCase(VIA_ONLINE), DWP_RESPONSE_RECEIVED_NOTIFICATION, 0);
    }

    @Test
    public void sendResponseReceivedLetterForAppellantRepOnlineCase() {
        testLetter(appellantRepCase(VIA_ONLINE), DWP_RESPONSE_RECEIVED_NOTIFICATION, 0);
    }

    @Test
    public void sendResponseReceivedLetterForAppointeeRepOnlineCase() {
        testLetter(appointeeRepCase(VIA_ONLINE), DWP_RESPONSE_RECEIVED_NOTIFICATION, 0);
    }

    private SscsCaseData appellantRepCase(String receivedVia) {
        SscsCaseData caseData = appellantCase(receivedVia);
        caseData.getAppeal().setRep(rep());
        caseData.setSubscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder().build())
                .representativeSubscription(Subscription.builder().build())
                .build());
        return caseData;
    }

    private SscsCaseData appointeeRepCase(String receivedVia) {
        SscsCaseData caseData = appointeeCase(receivedVia);
        caseData.getAppeal().setRep(rep());
        caseData.setSubscriptions(Subscriptions.builder()
                .appointeeSubscription(Subscription.builder().build())
                .representativeSubscription(Subscription.builder().build())
                .build());
        return caseData;
    }

    private SscsCaseData appointeeCase(String receivedVia) {
        SscsCaseData caseData = appellantCase(receivedVia);
        caseData.getAppeal().getAppellant().setAppointee(appointee());
        caseData.setSubscriptions(Subscriptions.builder()
                .appointeeSubscription(Subscription.builder().build())
                .build());
        return caseData;
    }

    private Representative rep() {
        return Representative.builder().hasRepresentative("Yes").build();
    }

    private SscsCaseData appellantCase(String receivedVia) {
        return SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .appellant(appellant())
                        .hearingType(AppealHearingType.ORAL.name())
                        .receivedVia(receivedVia)
                        .build())
                .subscriptions(Subscriptions.builder().appellantSubscription(Subscription.builder().build()).build())
                .build();
    }

    private Appellant appellant() {
        return Appellant.builder()
                .name(Name.builder()
                        .firstName("Appellant")
                        .lastName("One")
                        .build())
                .build();
    }

    private Appointee appointee() {
        return Appointee.builder()
                .name(Name.builder()
                        .firstName("Appointee")
                        .lastName("One")
                        .build())
                .build();
    }

    private void testLetter(SscsCaseData caseData, NotificationEventType eventType, int letterCount) {
        SscsCaseDataWrapper wrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .notificationEventType(eventType)
                .build();
        CcdNotificationWrapper ccdNotificationWrapper = new CcdNotificationWrapper(wrapper);

        String letterTemplateId = "letter-1";
        Notification notification = new Notification(Template.builder().letterTemplateId(letterTemplateId).build(), Destination.builder().build(), null, new Reference(), null);
        when((notificationValidService).isNotificationStillValidToSend(any(), any())).thenReturn(true);
        when((notificationValidService).isHearingTypeValidToSendNotification(any(), any())).thenReturn(true);
        when(factory.create(ccdNotificationWrapper, APPELLANT)).thenReturn(notification);
        when(factory.create(ccdNotificationWrapper, APPOINTEE)).thenReturn(notification);
        when(factory.create(ccdNotificationWrapper, REPRESENTATIVE)).thenReturn(notification);

        getNotificationService(false, true).manageNotificationAndSubscription(ccdNotificationWrapper);

        verify(notificationHandler, times(letterCount)).sendNotification(eq(ccdNotificationWrapper), eq(letterTemplateId), eq(LETTER), any(NotificationHandler.SendNotification.class));
        verify(notificationHandler, never()).sendNotification(any(), any(), eq(EMAIL), any());
        verify(notificationHandler, never()).sendNotification(any(), any(), eq(SMS), any());
    }


    private NotificationService getNotificationService(Boolean bundledLettersOn, Boolean lettersOn) {

        SendNotificationService sendNotificationService = getSendNotificationService();
        ReflectionTestUtils.setField(sendNotificationService, "noncompliantcaseletterTemplate", "/templates/non_compliant_case_letter_template.html");
        ReflectionTestUtils.setField(sendNotificationService, "bundledLettersOn", bundledLettersOn);
        ReflectionTestUtils.setField(sendNotificationService, "lettersOn", lettersOn);

        return new NotificationService(factory, reminderService, notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService);
    }

    private SendNotificationService getSendNotificationService() {
        return new SendNotificationService(notificationSender, evidenceManagementService, sscsGeneratePdfService, notificationHandler);
    }

}
