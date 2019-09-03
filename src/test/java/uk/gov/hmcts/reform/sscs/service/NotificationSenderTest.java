package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.NotificationBlacklist;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.service.notify.*;

public class NotificationSenderTest {
    public static final String CCD_CASE_ID = "78980909090099";
    public static final SscsCaseData SSCS_CASE_DATA = SscsCaseData.builder().build();
    public static final String SMS_SENDER = "sms-sender";
    private NotificationClient notificationClient;
    private NotificationClient testNotificationClient;
    private NotificationBlacklist blacklist;
    private NotificationSender notificationSender;
    private String templateId;
    private Map<String, String> personalisation;
    private String reference;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Mock
    private SendSmsResponse sendSmsResponse;

    @Mock
    private LetterResponse letterResponse;

    @Mock
    private CcdPdfService ccdPdfService;

    @Mock
    private SendLetterResponse sendLetterResponse;


    private Boolean saveCorrespondence = false;

    @Mock
    private MarkdownTransformationService markdownTransformationService;

    @Before
    public void setUp() {
        initMocks(this);

        notificationClient = mock(NotificationClient.class);
        testNotificationClient = mock(NotificationClient.class);
        blacklist = mock(NotificationBlacklist.class);
        templateId = "templateId";
        personalisation = Collections.emptyMap();
        reference = "reference";

        notificationSender = new NotificationSender(notificationClient, testNotificationClient, blacklist, ccdPdfService, markdownTransformationService, saveCorrespondence);
    }

    @Test
    public void sendEmailToTestSenderIfMatchesPattern() throws NotificationClientException {
        String emailAddress = "test123@hmcts.net";
        when(testNotificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, SSCS_CASE_DATA);

        verifyZeroInteractions(notificationClient);
        verify(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendEmailToNormalSender() throws NotificationClientException {
        String emailAddress = "random@example.com";
        when(notificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, SSCS_CASE_DATA);

        verifyZeroInteractions(testNotificationClient);
        verify(notificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendEmailToTestSenderIfOnBlacklist() throws NotificationClientException {
        String emailAddress = "random@example.com";
        when(blacklist.getTestRecipients()).thenReturn(singletonList(emailAddress));
        when(testNotificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, SSCS_CASE_DATA);

        verifyZeroInteractions(notificationClient);
        verify(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendSmsToNormalSender() throws NotificationClientException {
        String phoneNumber = "07777777777";
        when(notificationClient.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER))
                .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER, CCD_CASE_ID);

        verifyZeroInteractions(testNotificationClient);
        verify(notificationClient).sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER);
    }

    @Test
    public void sendSmsToTestSenderIfOnBlacklist() throws NotificationClientException {
        String phoneNumber = "07777777777";
        when(blacklist.getTestRecipients()).thenReturn(singletonList(phoneNumber));
        when(testNotificationClient.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER))
                .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER, CCD_CASE_ID);

        verifyZeroInteractions(notificationClient);
        verify(testNotificationClient).sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER);
    }

    @Test
    public void sendBundledLetterToNormalSender() throws IOException, NotificationClientException {
        String postcode = "LN8 4DX";

        when(notificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));
        notificationSender.sendBundledLetter(postcode, sampleDirectionCoversheet, CCD_CASE_ID);

        verifyZeroInteractions(testNotificationClient);
        verify(notificationClient).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    public void sendBundledLetterToSenderIfOnBlacklist() throws IOException, NotificationClientException {
        String postcode = "TS1 1ST";

        when(blacklist.getTestRecipients()).thenReturn(singletonList(postcode));
        when(testNotificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));
        notificationSender.sendBundledLetter(postcode, sampleDirectionCoversheet, CCD_CASE_ID);

        verifyZeroInteractions(notificationClient);
        verify(testNotificationClient).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    public void sendLetterToNormalSender() throws IOException, NotificationClientException {
        String postcode = "LN8 4DX";

        when(notificationClient.sendLetter(any(), any(), any())).thenReturn(sendLetterResponse);
        when(sendLetterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        Address address = Address.builder().line1("1 Appellant Ave").town("Sometown").county("Somecounty").postcode(postcode).build();
        notificationSender.sendLetter(templateId, address, personalisation, CCD_CASE_ID);

        verifyZeroInteractions(testNotificationClient);
        verify(notificationClient).sendLetter(any(), any(), any());
    }

    @Test
    public void sendLetterToSenderIfOnBlacklist() throws IOException, NotificationClientException {
        String postcode = "TS1 1ST";

        when(blacklist.getTestRecipients()).thenReturn(singletonList(postcode));
        when(testNotificationClient.sendLetter(any(), any(), any())).thenReturn(sendLetterResponse);
        when(sendLetterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        Address address = Address.builder().line1("1 Appellant Ave").town("Sometown").county("Somecounty").postcode(postcode).build();
        notificationSender.sendLetter(templateId, address, personalisation, CCD_CASE_ID);

        verifyZeroInteractions(notificationClient);
        verify(testNotificationClient).sendLetter(any(), any(), any());
    }

    @Test
    public void whenAnEmailIsSentWillSaveEmailTheEmailNotificationInCcd() throws NotificationClientException {

        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);

        String emailAddress = "random@example.com";
        when(notificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());
        when(markdownTransformationService.toHtml(anyString())).thenReturn("the body");
        when(ccdPdfService.mergeCorrespondenceIntoCcd(any(), any())).thenReturn(SscsCaseData.builder().build());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, SSCS_CASE_DATA);

        verifyZeroInteractions(testNotificationClient);
        verify(notificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
        verify(markdownTransformationService).toHtml(eq(null));

        Correspondence expectedCorrespondence = Correspondence.builder().value(CorrespondenceDetails.builder()
                .to(emailAddress)
                .from("")
                .correspondenceType(CorrespondenceType.Email)
                .eventType(NotificationEventType.APPEAL_RECEIVED_NOTIFICATION.getId())
                .sentOn("this field is ignored")
                .build()).build();
        verify(ccdPdfService).mergeCorrespondenceIntoCcd(eq(SscsCaseData.builder().build()),
               argThat(((Correspondence arg) -> EqualsBuilder.reflectionEquals(arg.getValue(), expectedCorrespondence.getValue(), "sentOn"))));
    }
}