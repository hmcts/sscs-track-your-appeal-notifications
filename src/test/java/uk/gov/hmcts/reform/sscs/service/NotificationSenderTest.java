package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.NotificationBlacklist;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.service.notify.*;

@RunWith(JUnitParamsRunner.class)
public class NotificationSenderTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    public static final String CCD_CASE_ID = "78980909090099";
    public static final SscsCaseData SSCS_CASE_DATA = SscsCaseData.builder().build();
    public static final String SMS_SENDER = "sms-sender";
    private NotificationSender notificationSender;
    private String templateId;
    private Map<String, String> personalisation;
    private String reference;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private NotificationClient testNotificationClient;

    @Mock
    private NotificationBlacklist blacklist;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Mock
    private SendSmsResponse sendSmsResponse;

    @Mock
    private LetterResponse letterResponse;

    @Mock
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    @Mock
    private SendLetterResponse sendLetterResponse;

    @Mock
    private MarkdownTransformationService markdownTransformationService;

    @Mock
    private SaveLetterCorrespondenceAsyncService saveLetterCorrespondenceAsyncService;

    @Before
    public void setUp() {
        templateId = "templateId";
        personalisation = Collections.emptyMap();
        reference = "reference";

        final Boolean saveCorrespondence = false;
        notificationSender = new NotificationSender(notificationClient, testNotificationClient, blacklist, ccdNotificationsPdfService, markdownTransformationService, saveLetterCorrespondenceAsyncService, saveCorrespondence);
    }

    @Test
    public void sendEmailToTestSenderIfMatchesPattern() throws NotificationClientException {
        String emailAddress = "test123@hmcts.net";
        when(testNotificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, SSCS_CASE_DATA);

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendEmailToNormalSender() throws NotificationClientException {
        String emailAddress = "random@example.com";
        when(notificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, SSCS_CASE_DATA);

        verifyNoInteractions(testNotificationClient);
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

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
    }

    @Test
    public void sendSmsToNormalSender() throws NotificationClientException {
        String phoneNumber = "07777777777";
        when(notificationClient.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER))
                .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, SSCS_CASE_DATA);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER);
    }

    @Test
    public void sendSmsToTestSenderIfOnBlacklist() throws NotificationClientException {
        String phoneNumber = "07777777777";
        when(blacklist.getTestRecipients()).thenReturn(singletonList(phoneNumber));
        when(testNotificationClient.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER))
                .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        notificationSender.sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, SSCS_CASE_DATA);

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendSms(templateId, phoneNumber, personalisation, reference, SMS_SENDER);
    }

    @Test
    public void sendBundledLetterToNormalSender() throws IOException, NotificationClientException {
        String postcode = "LN8 4DX";

        when(notificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));
        notificationSender.sendBundledLetter(postcode, sampleDirectionCoversheet, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, "Bob Squires", CCD_CASE_ID);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    public void sendBundledLetterToSenderIfOnBlacklist() throws IOException, NotificationClientException {
        String postcode = "TS1 1ST";

        when(blacklist.getTestRecipients()).thenReturn(singletonList(postcode));
        when(testNotificationClient.sendPrecompiledLetterWithInputStream(any(), any())).thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));
        notificationSender.sendBundledLetter(postcode, sampleDirectionCoversheet, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, "Bob Squires", CCD_CASE_ID);

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    public void sendLetterToNormalSender() throws IOException, NotificationClientException {
        String postcode = "LN8 4DX";

        when(notificationClient.sendLetter(any(), any(), any())).thenReturn(sendLetterResponse);
        when(sendLetterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        Address address = Address.builder().line1("1 Appellant Ave").town("Sometown").county("Somecounty").postcode(postcode).build();
        notificationSender.sendLetter(templateId, address, personalisation, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, "Bob Squires", CCD_CASE_ID);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendLetter(any(), any(), any());
    }

    @Test
    public void sendLetterToSenderIfOnBlacklist() throws IOException, NotificationClientException {
        String postcode = "TS1 1ST";

        when(blacklist.getTestRecipients()).thenReturn(singletonList(postcode));
        when(testNotificationClient.sendLetter(any(), any(), any())).thenReturn(sendLetterResponse);
        when(sendLetterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        Address address = Address.builder().line1("1 Appellant Ave").town("Sometown").county("Somecounty").postcode(postcode).build();
        notificationSender.sendLetter(templateId, address, personalisation, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, "Bob Squires", CCD_CASE_ID);

        verifyNoInteractions(notificationClient);
        verify(testNotificationClient).sendLetter(any(), any(), any());
    }

    @Test
    public void whenAnEmailIsSentWillSaveEmailNotificationInCcd() throws NotificationClientException {

        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);

        String emailAddress = "random@example.com";
        when(notificationClient.sendEmail(templateId, emailAddress, personalisation, reference))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());
        when(ccdNotificationsPdfService.mergeCorrespondenceIntoCcd(any(), any())).thenReturn(SscsCaseData.builder().build());

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, SSCS_CASE_DATA);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendEmail(templateId, emailAddress, personalisation, reference);
        verify(markdownTransformationService).toHtml(eq(null));

        Correspondence expectedCorrespondence = Correspondence.builder().value(CorrespondenceDetails.builder()
                .to(emailAddress)
                .from("")
                .correspondenceType(CorrespondenceType.Email)
                .eventType(NotificationEventType.APPEAL_RECEIVED_NOTIFICATION.getId())
                .sentOn("this field is ignored")
                .build()).build();
        verify(ccdNotificationsPdfService).mergeCorrespondenceIntoCcd(eq(SscsCaseData.builder().build()),
               argThat(((Correspondence arg) -> EqualsBuilder.reflectionEquals(arg.getValue(), expectedCorrespondence.getValue(), "sentOn"))));
    }

    @Test
    public void whenAnSmsIsSentWillSaveSmsNotificationInCcd() throws NotificationClientException {

        ReflectionTestUtils.setField(notificationSender, "saveCorrespondence", true);

        String smsNumber = "07999999000";
        when(notificationClient.sendSms(templateId, smsNumber, personalisation, reference, "Sender"))
                .thenReturn(sendSmsResponse);
        when(ccdNotificationsPdfService.mergeCorrespondenceIntoCcd(any(), any())).thenReturn(SscsCaseData.builder().build());

        notificationSender.sendSms(templateId, smsNumber, personalisation, reference, "Sender", NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, SSCS_CASE_DATA);

        verifyNoInteractions(testNotificationClient);
        verify(notificationClient).sendSms(templateId, smsNumber, personalisation, reference, "Sender");
        verify(markdownTransformationService).toHtml(eq(null));

        Correspondence expectedCorrespondence = Correspondence.builder().value(CorrespondenceDetails.builder()
                .to(smsNumber)
                .from("")
                .subject("SMS correspondence")
                .correspondenceType(CorrespondenceType.Sms)
                .eventType(NotificationEventType.APPEAL_RECEIVED_NOTIFICATION.getId())
                .sentOn("this field is ignored")
                .build()).build();
        verify(ccdNotificationsPdfService).mergeCorrespondenceIntoCcd(eq(SscsCaseData.builder().build()),
                argThat(((Correspondence arg) -> EqualsBuilder.reflectionEquals(arg.getValue(), expectedCorrespondence.getValue(), "sentOn"))));
    }

    @Test(expected = NotificationClientException.class)
    @Parameters({"null", "NotificationClientException"})
    public void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendEmail(String error) throws NotificationClientException {
        String emailAddress = "test123@hmcts.net";
        Exception exception = (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
        doThrow(exception).when(testNotificationClient).sendEmail(templateId, emailAddress, personalisation, reference);

        notificationSender.sendEmail(templateId, emailAddress, personalisation, reference, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, SSCS_CASE_DATA);
    }

    @Test(expected = NotificationClientException.class)
    @Parameters({"null", "NotificationClientException"})
    public void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendSms(String error) throws NotificationClientException {
        String smsNumber = "07999999000";
        Exception exception = (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
        doThrow(exception).when(notificationClient).sendSms(templateId, smsNumber, personalisation, reference, "Sender");

        notificationSender.sendSms(templateId, smsNumber, personalisation, reference, "Sender", NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, SSCS_CASE_DATA);
    }

    @Test(expected = NotificationClientException.class)
    @Parameters({"null", "NotificationClientException"})
    public void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendLetter(String error) throws NotificationClientException {
        String postcode = "TS1 1ST";
        Address address = Address.builder().line1("1 Appellant Ave").town("Sometown").county("Somecounty").postcode(postcode).build();
        Exception exception = (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
        doThrow(exception).when(notificationClient).sendLetter(any(), any(), any());

        notificationSender.sendLetter(templateId, address, personalisation, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, "Bob Squires", CCD_CASE_ID);
    }

    @Test(expected = NotificationClientException.class)
    @Parameters({"null", "NotificationClientException"})
    public void shouldCatchAndThrowAnyExceptionFromGovNotifyOnSendBundledLetter(String error) throws NotificationClientException, IOException {
        Exception exception = (error.equals("null")) ? new NullPointerException(error) : new NotificationClientException(error);
        doThrow(exception).when(notificationClient).sendPrecompiledLetterWithInputStream(any(), any());

        String postcode = "LN8 4DX";
        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));
        notificationSender.sendBundledLetter(postcode, sampleDirectionCoversheet, NotificationEventType.APPEAL_RECEIVED_NOTIFICATION, "Bob Squires", CCD_CASE_ID);
    }
}