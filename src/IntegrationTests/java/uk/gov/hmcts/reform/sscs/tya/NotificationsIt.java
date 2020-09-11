package uk.gov.hmcts.reform.sscs.tya;

import static helper.IntegrationTestHelper.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import javax.servlet.http.HttpServletResponse;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.NotificationBlacklist;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.controller.NotificationController;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.*;
import uk.gov.hmcts.reform.sscs.service.docmosis.PdfLetterService;
import uk.gov.service.notify.*;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class NotificationsIt {
    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private MockMvc mockMvc;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Mock
    private SendSmsResponse sendSmsResponse;

    @Mock
    private SendLetterResponse sendLetterResponse;

    @Mock
    private ReminderService reminderService;

    @MockBean
    private AuthorisationService authorisationService;

    @Mock
    private NotificationBlacklist notificationBlacklist;

    @Autowired
    private NotificationValidService notificationValidService;

    @Autowired
    private NotificationFactory factory;

    @Autowired
    private CcdService ccdService;

    @Autowired
    private SscsCaseCallbackDeserializer deserializer;

    @MockBean
    private IdamService idamService;

    private String json;

    @Autowired
    private NotificationHandler notificationHandler;

    @MockBean
    private OutOfHoursCalculator outOfHoursCalculator;

    @Autowired
    private NotificationConfig notificationConfig;

    @Autowired
    private PdfLetterService pdfLetterService;

    @Autowired
    private DocmosisPdfService docmosisPdfService;

    @MockBean
    private DocmosisPdfGenerationService docmosisPdfGenerationService;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Value("${notification.english.subscriptionUpdated.emailId}")
    private String subscriptionUpdatedEmailId;

    @Mock
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    @Value("${notification.english.subscriptionCreated.appellant.smsId}")
    private String subscriptionCreatedSmsId;

    private final Boolean saveCorrespondence = false;

    @Mock
    private MarkdownTransformationService markdownTransformationService;

    @Mock
    private SaveLetterCorrespondenceAsyncService saveLetterCorrespondenceAsyncService;

    @Before
    public void setup() throws Exception {
        NotificationSender sender = new NotificationSender(notificationClient, null, notificationBlacklist, ccdNotificationsPdfService, markdownTransformationService, saveLetterCorrespondenceAsyncService, saveCorrespondence);

        SendNotificationService sendNotificationService = new SendNotificationService(sender, evidenceManagementService, notificationHandler, notificationValidService, pdfLetterService);

        NotificationService service = new NotificationService(factory, reminderService, notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService, false);
        NotificationController controller = new NotificationController(service, authorisationService, ccdService, deserializer, idamService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        when(notificationClient.sendEmail(any(), any(), any(), any()))
            .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        when(notificationClient.sendSms(any(), any(), any(), any(), any()))
            .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        when(notificationClient.sendLetter(any(), any(), any()))
            .thenReturn(sendLetterResponse);
        when(notificationClient.sendPrecompiledLetterWithInputStream(any(), any()))
            .thenReturn(sendLetterResponse);
        when(sendLetterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        outOfHoursCalculator = mock(OutOfHoursCalculator.class);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);

        byte[] pdfbytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));
        when(docmosisPdfGenerationService.generatePdf(any())).thenReturn(pdfbytes);
    }

    @Test
    public void shouldSendNotificationForAnAppealReceivedRequestForAnOralHearing() throws Exception {
        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(2)).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAnAppealReceivedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(2)).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAnAdjournedRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "hearingAdjourned");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(2)).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForAnAdjournedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "hearingAdjourned");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any());
        verify(notificationClient, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAnEvidenceReceivedRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "evidenceReceived");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(2)).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendEmailNotificationOnlyForAnEvidenceReceivedRequestToAnAppellantForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "evidenceReceived");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "representativeSubscription", "subscribeSms");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "representativeSubscription", "subscribeEmail");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, times(1)).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(1)).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAHearingPostponedRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "hearingPostponed");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForAHearingPostponedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "hearingPostponed");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any());
        verify(notificationClient, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    @Parameters(method = "generateRepsNotificationScenarios")
    public void shouldSendRepsNotificationsForAnEventForAnOralOrPaperHearingAndForEachSubscription(
        NotificationEventType notificationEventType, String hearingType, List<String> expectedEmailTemplateIds,
        List<String> expectedSmsTemplateIds, List<String> expectedLetterTemplateIds, String appellantEmailSubs, String appellantSmsSubs, String repsEmailSubs,
        String repsSmsSubs, int wantedNumberOfSendEmailInvocations, int wantedNumberOfSendSmsInvocations, int wantedNumberOfSendLetterInvocations) throws Exception {
        json = updateEmbeddedJson(json, hearingType, "case_details", "case_data", "appeal", "hearingType");
        json = updateEmbeddedJson(json, appellantEmailSubs, "case_details", "case_data", "subscriptions",
            "appellantSubscription", "subscribeEmail");
        json = updateEmbeddedJson(json, appellantSmsSubs, "case_details", "case_data", "subscriptions",
            "appellantSubscription", "subscribeSms");
        json = updateEmbeddedJson(json, repsEmailSubs, "case_details", "case_data", "subscriptions",
            "representativeSubscription", "subscribeEmail");
        json = updateEmbeddedJson(json, repsSmsSubs, "case_details", "case_data", "subscriptions",
            "representativeSubscription", "subscribeSms");
        json = updateEmbeddedJson(json, notificationEventType.getId(), "event_id");

        if (notificationEventType.equals(REQUEST_INFO_INCOMPLETE)) {
            json = updateEmbeddedJson(json, "Yes", "case_details", "case_data", "informationFromAppellant");
        }

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));
        assertHttpStatus(response, HttpStatus.OK);

        String expectedName = "Harry Potter";
        validateEmailNotifications(expectedEmailTemplateIds, wantedNumberOfSendEmailInvocations, expectedName);
        validateSmsNotifications(expectedSmsTemplateIds, wantedNumberOfSendSmsInvocations);
        validateLetterNotifications(expectedLetterTemplateIds, wantedNumberOfSendLetterInvocations, expectedName);
    }

    @Test
    @Parameters(method = "generateBundledLetterNotificationScenarios")
    public void shouldSendRepsBundledLetterNotificationsForAnEventForAnOralOrPaperHearingAndForEachSubscription(
        NotificationEventType notificationEventType, String hearingType, boolean hasRep, boolean hasAppointee, int wantedNumberOfSendLetterInvocations) throws Exception {

        byte[] sampleDirectionNotice = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-text.pdf"));
        when(evidenceManagementService.download(any(), any())).thenReturn(sampleDirectionNotice);

        String filename = "json/ccdResponse_"
            + notificationEventType.getId()
            + (hasRep ? "_withRep" : "")
            + (hasAppointee ? "_withAppointee" : "")
            + ".json";
        String path = getClass().getClassLoader().getResource(filename).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = updateEmbeddedJson(json, hearingType, "case_details", "case_data", "appeal", "hearingType");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));
        assertHttpStatus(response, HttpStatus.OK);

        verify(notificationClient, times(wantedNumberOfSendLetterInvocations)).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    @Parameters(method = "generateRepsNotificationScenariosWhenNoOldCaseRef")
    public void shouldSendRepsNotificationsForAnEventForAnOralOrPaperHearingAndForEachSubscriptionWhenNoOldCaseRef(
        NotificationEventType notificationEventType, String hearingType, List<String> expectedEmailTemplateIds,
        List<String> expectedSmsTemplateIds, List<String> expectedLetterTemplateIds, String appellantEmailSubs, String appellantSmsSubs, String repsEmailSubs,
        String repsSmsSubs, int wantedNumberOfSendEmailInvocations, int wantedNumberOfSendSmsInvocations, int wantedNumberOfSendLetterInvocations) throws Exception {
        String path = getClass().getClassLoader().getResource("json/ccdResponseWithNoOldCaseRef.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = updateEmbeddedJson(json, hearingType, "case_details", "case_data", "appeal", "hearingType");
        json = updateEmbeddedJson(json, appellantEmailSubs, "case_details", "case_data", "subscriptions",
            "appellantSubscription", "subscribeEmail");
        json = updateEmbeddedJson(json, appellantSmsSubs, "case_details", "case_data", "subscriptions",
            "appellantSubscription", "subscribeSms");
        json = updateEmbeddedJson(json, repsEmailSubs, "case_details", "case_data", "subscriptions",
            "representativeSubscription", "subscribeEmail");
        json = updateEmbeddedJson(json, repsSmsSubs, "case_details", "case_data", "subscriptions",
            "representativeSubscription", "subscribeSms");
        json = updateEmbeddedJson(json, notificationEventType.getId(), "event_id");

        if (notificationEventType.equals(REQUEST_INFO_INCOMPLETE)) {
            json = updateEmbeddedJson(json, "Yes", "case_details", "case_data", "informationFromAppellant");
        }

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));
        assertHttpStatus(response, HttpStatus.OK);

        String expectedName = "Harry Potter";
        validateEmailNotifications(expectedEmailTemplateIds, wantedNumberOfSendEmailInvocations, expectedName);
        validateSmsNotifications(expectedSmsTemplateIds, wantedNumberOfSendSmsInvocations);
        validateLetterNotifications(expectedLetterTemplateIds, wantedNumberOfSendLetterInvocations, expectedName);
    }

    @Test
    @Parameters(method = "generateAppointeeNotificationScenarios")
    @SuppressWarnings("unchecked")
    public void shouldSendAppointeeNotificationsForAnEventForAnOralOrPaperHearingAndForEachSubscription(
        NotificationEventType notificationEventType, String hearingType, List<String> expectedEmailTemplateIds,
        List<String> expectedSmsTemplateIds, List<String> expectedLetterTemplateIds, String appointeeEmailSubs,
        String appointeeSmsSubs, int wantedNumberOfSendEmailInvocations, int wantedNumberOfSendSmsInvocations,
        int wantedNumberOfSendLetterInvocations, String expectedName) throws Exception {

        String path = getClass().getClassLoader().getResource("json/ccdResponseWithAppointee.json").getFile();
        String jsonAppointee = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        jsonAppointee = updateEmbeddedJson(jsonAppointee, hearingType, "case_details", "case_data", "appeal", "hearingType");

        jsonAppointee = updateEmbeddedJson(jsonAppointee, appointeeEmailSubs, "case_details", "case_data", "subscriptions",
            "appointeeSubscription", "subscribeEmail");
        jsonAppointee = updateEmbeddedJson(jsonAppointee, appointeeSmsSubs, "case_details", "case_data", "subscriptions",
            "appointeeSubscription", "subscribeSms");

        if (notificationEventType.equals(HEARING_BOOKED_NOTIFICATION)) {
            jsonAppointee = jsonAppointee.replace("appealReceived", "hearingBooked");
            jsonAppointee = jsonAppointee.replace("2018-01-12", LocalDate.now().plusDays(2).toString());
        }

        if (notificationEventType.equals(REQUEST_INFO_INCOMPLETE)) {
            jsonAppointee = updateEmbeddedJson(jsonAppointee, "Yes", "case_details", "case_data", "informationFromAppellant");
        }

        jsonAppointee = updateEmbeddedJson(jsonAppointee, notificationEventType.getId(), "event_id");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(jsonAppointee));

        assertHttpStatus(response, HttpStatus.OK);

        validateEmailNotifications(expectedEmailTemplateIds, wantedNumberOfSendEmailInvocations, expectedName);
        validateSmsNotifications(expectedSmsTemplateIds, wantedNumberOfSendSmsInvocations);
        validateLetterNotifications(expectedLetterTemplateIds, wantedNumberOfSendLetterInvocations, expectedName);
    }

    @Test
    @Parameters(method = "generateAppointeeNotificationWhenNoOldCaseReferenceScenarios")
    @SuppressWarnings("unchecked")
    public void shouldSendAppointeeNotificationsForAnEventForAnOralOrPaperHearingAndForEachSubscriptionWhenNoOldCaseReference(
        NotificationEventType notificationEventType,
        String hearingType,
        List<String> expectedEmailTemplateIds,
        List<String> expectedSmsTemplateIds,
        List<String> expectedLetterTemplateIds,
        String appointeeEmailSubs,
        String appointeeSmsSubs,
        int wantedNumberOfSendEmailInvocations,
        int wantedNumberOfSendSmsInvocations,
        int wantedNumberOfSendLetterInvocations,
        String expectedName) throws Exception {

        String path = Objects.requireNonNull(getClass().getClassLoader()
            .getResource("json/ccdResponseWithAppointeeWithNoOldCaseRef.json")).getFile();
        String jsonAppointee = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        jsonAppointee = updateEmbeddedJson(jsonAppointee, hearingType, "case_details", "case_data", "appeal", "hearingType");

        jsonAppointee = updateEmbeddedJson(jsonAppointee, appointeeEmailSubs, "case_details", "case_data", "subscriptions",
            "appointeeSubscription", "subscribeEmail");
        jsonAppointee = updateEmbeddedJson(jsonAppointee, appointeeSmsSubs, "case_details", "case_data", "subscriptions",
            "appointeeSubscription", "subscribeSms");

        if (notificationEventType.equals(REQUEST_INFO_INCOMPLETE)) {
            jsonAppointee = updateEmbeddedJson(jsonAppointee, "Yes", "case_details", "case_data", "informationFromAppellant");
        }

        jsonAppointee = updateEmbeddedJson(jsonAppointee, notificationEventType.getId(), "event_id");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(jsonAppointee));

        assertHttpStatus(response, HttpStatus.OK);

        validateEmailNotifications(expectedEmailTemplateIds, wantedNumberOfSendEmailInvocations, expectedName);
        validateSmsNotifications(expectedSmsTemplateIds, wantedNumberOfSendSmsInvocations);
        validateLetterNotifications(expectedLetterTemplateIds, wantedNumberOfSendLetterInvocations, expectedName);
    }

    @Test
    @Parameters(method = "generateJointPartyNotificationScenarios")
    public void shouldSendJointPartyNotificationsForAnEventForAnOralOrPaperHearingAndForEachSubscription(
            NotificationEventType notificationEventType, String hearingType, List<String> expectedEmailTemplateIds,
            List<String> expectedSmsTemplateIds, List<String> expectedLetterTemplateIds, String jointPartyEmailSubs,
            String jointPartySmsSubs, int wantedNumberOfSendEmailInvocations, int wantedNumberOfSendSmsInvocations, int wantedNumberOfSendLetterInvocations) throws Exception {
        String path = getClass().getClassLoader().getResource("json/ccdResponseWithJointParty.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = updateEmbeddedJson(json, hearingType, "case_details", "case_data", "appeal", "hearingType");
        json = updateEmbeddedJson(json, "no", "case_details", "case_data", "subscriptions",
                "appellantSubscription", "subscribeEmail");
        json = updateEmbeddedJson(json, "no", "case_details", "case_data", "subscriptions",
                "appellantSubscription", "subscribeSms");
        json = updateEmbeddedJson(json, jointPartyEmailSubs, "case_details", "case_data", "subscriptions",
                "jointPartySubscription", "subscribeEmail");
        json = updateEmbeddedJson(json, jointPartySmsSubs, "case_details", "case_data", "subscriptions",
                "jointPartySubscription", "subscribeSms");
        json = updateEmbeddedJson(json, notificationEventType.getId(), "event_id");

        if (notificationEventType.equals(REQUEST_INFO_INCOMPLETE)) {
            json = updateEmbeddedJson(json, "Yes", "case_details", "case_data", "informationFromAppellant");
        }

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));
        assertHttpStatus(response, HttpStatus.OK);

        String expectedName = "Joint Party";
        validateEmailNotifications(expectedEmailTemplateIds, wantedNumberOfSendEmailInvocations, expectedName);
        validateSmsNotifications(expectedSmsTemplateIds, wantedNumberOfSendSmsInvocations);
        validateLetterNotifications(expectedLetterTemplateIds, wantedNumberOfSendLetterInvocations, expectedName);
    }

    private void validateEmailNotifications(List<String> expectedEmailTemplateIds,
                                            int wantedNumberOfSendEmailInvocations, String expectedName)
        throws NotificationClientException {

        ArgumentCaptor<String> emailTemplateIdCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> emailPersonalisationCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationClient, times(wantedNumberOfSendEmailInvocations))
            .sendEmail(emailTemplateIdCaptor.capture(), any(), emailPersonalisationCaptor.capture(), any());
        assertArrayEquals(expectedEmailTemplateIds.toArray(), emailTemplateIdCaptor.getAllValues().toArray());

        if (0 < wantedNumberOfSendEmailInvocations) {
            Map<String, ?> personalisation = emailPersonalisationCaptor.getValue();
            if (null != personalisation.get(REPRESENTATIVE_NAME)) {
                assertEquals(expectedName, personalisation.get(REPRESENTATIVE_NAME));
            } else {
                assertEquals(expectedName, personalisation.get(NAME));
            }
            assertEquals("Dexter Vasquez", personalisation.get(APPELLANT_NAME));
        }
    }

    private void validateSmsNotifications(List<String> expectedSmsTemplateIds, int wantedNumberOfSendSmsInvocations) throws NotificationClientException {
        ArgumentCaptor<String> smsTemplateIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationClient, times(wantedNumberOfSendSmsInvocations))
            .sendSms(smsTemplateIdCaptor.capture(), any(), any(), any(), any());
        assertArrayEquals(expectedSmsTemplateIds.toArray(), smsTemplateIdCaptor.getAllValues().toArray());
    }

    private void validateLetterNotifications(List<String> expectedLetterTemplateIds, int wantedNumberOfSendLetterInvocations, String expectedName) throws NotificationClientException {
        ArgumentCaptor<Map<String, String>> letterPersonalisationCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> letterTemplateIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationClient, atMost(wantedNumberOfSendLetterInvocations))
            .sendLetter(letterTemplateIdCaptor.capture(), letterPersonalisationCaptor.capture(), any());
        assertArrayEquals(expectedLetterTemplateIds.stream().filter(f -> !(f.endsWith(".doc") || f.endsWith(".docx"))).toArray(), letterTemplateIdCaptor.getAllValues().toArray());

        int expectedDocmosisLetters = expectedLetterTemplateIds.stream().filter(f -> f.endsWith(".doc") || f.endsWith(".docx")).toArray().length;
        if (expectedDocmosisLetters > 0) {
            verify(notificationClient, times(expectedDocmosisLetters)).sendPrecompiledLetterWithInputStream(any(), any());
        } else {
            verify(notificationClient, times(0)).sendPrecompiledLetterWithInputStream(any(), any());
        }

        if (0 < wantedNumberOfSendLetterInvocations) {
            Map<String, String> personalisation = letterPersonalisationCaptor.getValue();
            if (null != personalisation.get(REPRESENTATIVE_NAME)) {
                assertEquals(expectedName, personalisation.get(REPRESENTATIVE_NAME));
            } else {
                assertEquals(expectedName, personalisation.get(NAME));
            }
            assertEquals("Dexter Vasquez", personalisation.get(APPELLANT_NAME));
        }
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateJointPartyNotificationScenarios() {
        return new Object[]{
                new Object[]{
                        APPEAL_LAPSED_NOTIFICATION,
                        "paper",
                        Collections.singletonList("8ce8d794-75e8-49a0-b4d2-0c6cd2061c11"),
                        Collections.singletonList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"),
                        Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                        "yes",
                        "yes",
                        "1",
                        "1",
                        "2"
                },
                new Object[]{
                        APPEAL_LAPSED_NOTIFICATION,
                        "oral",
                        Collections.singletonList("8ce8d794-75e8-49a0-b4d2-0c6cd2061c11"),
                        Collections.singletonList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"),
                        Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                        "yes",
                        "yes",
                        "1",
                        "1",
                        "2"
                },
                new Object[]{
                        APPEAL_LAPSED_NOTIFICATION,
                        "paper",
                        Collections.emptyList(),
                        Collections.singletonList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"),
                        Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                        "no",
                        "yes",
                        "0",
                        "1",
                        "2"
                },
                new Object[]{
                        APPEAL_LAPSED_NOTIFICATION,
                        "paper",
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                        "no",
                        "no",
                        "0",
                        "0",
                        "2"
                },
                new Object[]{
                        EVIDENCE_REMINDER_NOTIFICATION,
                        "paper",
                        Collections.singletonList("c507a630-9e6a-43c9-8e39-dcabdcffaf53"),
                        Collections.singletonList("56a6c0c8-a251-482d-be83-95a7a1bf528c"),
                        Collections.emptyList(),
                        "yes",
                        "yes",
                        "1",
                        "1",
                        "0"
                }
        };
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateRepsNotificationScenarios() {
        return new Object[]{
            new Object[]{
                    STRUCK_OUT,
                    "paper",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Arrays.asList("TB-SCS-GNO-ENG-00466.docx", "TB-SCS-GNO-ENG-00466.docx"),
                    "no",
                    "no",
                    "no",
                    "no",
                    "0",
                    "0",
                    "0"
            },
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "paper",
                Arrays.asList("7af36950-fc63-45d1-907d-f472fac7af06"),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "yes",
                "no",
                "1",
                "0",
                "0"
            },
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "paper",
                Arrays.asList("cab48431-a4f0-41f5-b753-2cecf20ab5d4", "7af36950-fc63-45d1-907d-f472fac7af06"),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "yes",
                "no",
                "2",
                "0",
                "0"
            },
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "oral",
                Arrays.asList("30260c0b-5575-4f4e-bce4-73cf3f245c2d"),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "yes",
                "no",
                "1",
                "0",
                "0"
            },
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "oral",
                Arrays.asList("c5654134-2e13-4541-ac73-334a5b5cdbb6", "30260c0b-5575-4f4e-bce4-73cf3f245c2d"),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "yes",
                "no",
                "2",
                "0",
                "0"
            },
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                "paper",
                Arrays.asList("8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "e93dd744-84a1-4173-847a-6d023b55637f"),
                Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "ee58f7d0-8de7-4bee-acd4-252213db6b7b"),
                Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "2"
            },
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                "oral",
                Arrays.asList("8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "e93dd744-84a1-4173-847a-6d023b55637f"),
                Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "ee58f7d0-8de7-4bee-acd4-252213db6b7b"),
                Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "2"
            },
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                "paper",
                Collections.singletonList("e93dd744-84a1-4173-847a-6d023b55637f"),
                Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "ee58f7d0-8de7-4bee-acd4-252213db6b7b"),
                Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "1"
            },
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "1"
            },
            new Object[]{
                APPEAL_WITHDRAWN_NOTIFICATION,
                "paper",
                Arrays.asList("8620e023-f663-477e-a771-9cfad50ee30f", "e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
                Arrays.asList("d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", "8c719552-6b6b-4df3-9edb-9fe1635755b9"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "2"
            },
            new Object[]{
                APPEAL_WITHDRAWN_NOTIFICATION,
                "oral",
                Arrays.asList("8620e023-f663-477e-a771-9cfad50ee30f", "e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
                Arrays.asList("d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", "8c719552-6b6b-4df3-9edb-9fe1635755b9"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "2"
            },
            new Object[]{
                APPEAL_WITHDRAWN_NOTIFICATION,
                "paper",
                Collections.singletonList("e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
                Arrays.asList("d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", "8c719552-6b6b-4df3-9edb-9fe1635755b9"),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "2"
            },
            new Object[]{
                APPEAL_WITHDRAWN_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", "8c719552-6b6b-4df3-9edb-9fe1635755b9"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "2"
            },
            new Object[]{
                ADMIN_APPEAL_WITHDRAWN,
                "paper",
                Collections.singletonList("e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
                Arrays.asList("d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", "8c719552-6b6b-4df3-9edb-9fe1635755b9"),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "2"
            },
            new Object[]{
                ADJOURNED_NOTIFICATION,
                "paper",
                Arrays.asList("cff1be5f-20cf-4cfa-9a90-4a75d3341ba8", "ecf7db7d-a257-4496-a2bf-768e560c80e7"),
                Arrays.asList("f71772b1-ae1d-49d6-87c6-a41da97a4039", "259b8e81-b44a-4271-a57b-ba7f8bdfcb33"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                ADJOURNED_NOTIFICATION,
                "oral",
                Arrays.asList("cff1be5f-20cf-4cfa-9a90-4a75d3341ba8", "ecf7db7d-a257-4496-a2bf-768e560c80e7"),
                Arrays.asList("f71772b1-ae1d-49d6-87c6-a41da97a4039", "259b8e81-b44a-4271-a57b-ba7f8bdfcb33"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                ADJOURNED_NOTIFICATION,
                "paper",
                Collections.singletonList("ecf7db7d-a257-4496-a2bf-768e560c80e7"),
                Arrays.asList("f71772b1-ae1d-49d6-87c6-a41da97a4039", "259b8e81-b44a-4271-a57b-ba7f8bdfcb33"),
                Collections.emptyList(),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                ADJOURNED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Arrays.asList("b90df52f-c628-409c-8875-4b0b9663a053", "4b1ee55b-abd1-4e7e-b0ed-693d8df1e741"),
                Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b", "99bd4a56-256c-4de8-b187-d43a8dde466f"),
                Arrays.asList("TB-SCS-GNO-ENG-00060.doc", "TB-SCS-GNO-ENG-00079.doc"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "oral",
                Arrays.asList("b90df52f-c628-409c-8875-4b0b9663a053", "4b1ee55b-abd1-4e7e-b0ed-693d8df1e741"),
                Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b", "99bd4a56-256c-4de8-b187-d43a8dde466f"),
                Arrays.asList("TB-SCS-GNO-ENG-00060.doc", "TB-SCS-GNO-ENG-00079.doc"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("4b1ee55b-abd1-4e7e-b0ed-693d8df1e741"),
                Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b", "99bd4a56-256c-4de8-b187-d43a8dde466f"),
                Arrays.asList("TB-SCS-GNO-ENG-00060.doc", "TB-SCS-GNO-ENG-00079.doc"),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00060.doc", "TB-SCS-GNO-ENG-00079.doc"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                APPEAL_DORMANT_NOTIFICATION,
                "paper",
                Arrays.asList("976bdb6c-8a86-48cf-9e0f-7989acaec0c2", "b74ea5d4-dba2-4148-b822-d102cedbea12"),
                Arrays.asList("1aa60c8a-1b6f-4ee1-88ae-51c1cef0ea2b", "4562984e-2854-4191-81d9-cffbe5111015"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                APPEAL_DORMANT_NOTIFICATION,
                "paper",
                Collections.singletonList("b74ea5d4-dba2-4148-b822-d102cedbea12"),
                Arrays.asList("1aa60c8a-1b6f-4ee1-88ae-51c1cef0ea2b", "4562984e-2854-4191-81d9-cffbe5111015"),
                Collections.emptyList(),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                APPEAL_DORMANT_NOTIFICATION,
                "oral",
                Arrays.asList("fc9d0618-68c4-48ec-9481-a84b225a57a9", "e2ee8609-7d56-4857-b3f8-79028e8960aa"),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "yes",
                "no",
                "2",
                "0",
                "0"
            },
            new Object[]{
                APPEAL_DORMANT_NOTIFICATION,
                "oral",
                Collections.singletonList("e2ee8609-7d56-4857-b3f8-79028e8960aa"),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "yes",
                "no",
                "1",
                "0",
                "0"
            },
            new Object[]{
                APPEAL_DORMANT_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "paper",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "paper",
                Collections.singletonList("652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                POSTPONEMENT_NOTIFICATION,
                "paper",
                Arrays.asList("221095a2-aee8-466b-a7ab-beee516cc6cc", "e07b7dba-f383-49ca-a0ba-b5b61be27da6"),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "yes",
                "no",
                "2",
                "0",
                "0"
            },
            new Object[]{
                POSTPONEMENT_NOTIFICATION,
                "oral",
                Arrays.asList("221095a2-aee8-466b-a7ab-beee516cc6cc", "e07b7dba-f383-49ca-a0ba-b5b61be27da6"),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "Yes",
                "no",
                "2",
                "0",
                "0"
            },
            new Object[]{
                POSTPONEMENT_NOTIFICATION,
                "paper",
                Collections.singletonList("e07b7dba-f383-49ca-a0ba-b5b61be27da6"),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "yes",
                "no",
                "1",
                "0",
                "0"
            },
            new Object[]{
                POSTPONEMENT_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                CASE_UPDATED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "yes",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                CASE_UPDATED,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "Yes",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                CASE_UPDATED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "yes",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                CASE_UPDATED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                CASE_UPDATED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                EVIDENCE_REMINDER_NOTIFICATION,
                "oral",
                Arrays.asList("df0803aa-f804-49fe-a2ac-c27adc4bb585"),
                Arrays.asList("5f91012e-0d3f-465b-b301-ee3ee5a50100"),
                Collections.emptyList(),
                "no",
                "no",
                "yes",
                "yes",
                "1",
                "1",
                "0"
            },
            new Object[]{
                EVIDENCE_REMINDER_NOTIFICATION,
                "paper",
                Arrays.asList("81fa38cc-b7cc-469c-8109-67c801dc9c84"),
                Arrays.asList("f1076482-a76d-4389-b411-9865373cfc42"),
                Collections.emptyList(),
                "no",
                "no",
                "yes",
                "yes",
                "1",
                "1",
                "0"
            },
            new Object[]{
                REQUEST_INFO_INCOMPLETE,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00452.docx", "TB-SCS-GNO-ENG-00452.docx"),
                "yes",
                "yes",
                "yes",
                "yes",
                "0",
                "0",
                "0"
            },
            new Object[]{
                REQUEST_INFO_INCOMPLETE,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00452.docx", "TB-SCS-GNO-ENG-00452.docx"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "paper",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "oral",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "paper",
                Collections.singletonList("652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                RESEND_APPEAL_CREATED_NOTIFICATION,
                "paper",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                RESEND_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                RESEND_APPEAL_CREATED_NOTIFICATION,
                "paper",
                Collections.singletonList("652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                RESEND_APPEAL_CREATED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                NON_COMPLIANT_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("114cadaa-7760-4699-9add-d3a252f68909", "5745f77c-6512-4082-8c34-63851f24eab1"),
                "yes",
                "yes",
                "yes",
                "yes",
                "0",
                "0",
                "2"
            },
            new Object[]{
                NON_COMPLIANT_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("114cadaa-7760-4699-9add-d3a252f68909", "5745f77c-6512-4082-8c34-63851f24eab1"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "2"
            },
            new Object[]{
                DIRECTION_ISSUED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00067.docx", "TB-SCS-GNO-ENG-00089.docx"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                DECISION_ISSUED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00067.docx", "TB-SCS-GNO-ENG-00089.docx"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                ISSUE_FINAL_DECISION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00067.docx", "TB-SCS-GNO-ENG-00089.docx"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                ISSUE_ADJOURNMENT_NOTICE,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00067.docx", "TB-SCS-GNO-ENG-00089.docx"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
        };
    }


    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateBundledLetterNotificationScenarios() {
        return new Object[]{
            new Object[]{
                    STRUCK_OUT,
                    "paper",
                    false,
                    false,
                    "1"
            },
            new Object[]{
                    STRUCK_OUT,
                    "oral",
                    false,
                    false,
                    "1"
            },
            new Object[]{
                DIRECTION_ISSUED,
                "paper",
                false,
                false,
                "1"
            },
            new Object[]{
                DIRECTION_ISSUED,
                "oral",
                false,
                false,
                "1"
            },
            new Object[]{
                DIRECTION_ISSUED,
                "oral",
                false,
                true,
                "1"
            },
            new Object[]{
                DIRECTION_ISSUED,
                "paper",
                false,
                true,
                "1"
            },
            new Object[]{
                DIRECTION_ISSUED,
                "paper",
                true,
                false,
                "2"
            },
            new Object[]{
                DIRECTION_ISSUED,
                "oral",
                true,
                false,
                "2"
            },
            new Object[]{
                DIRECTION_ISSUED,
                "paper",
                true,
                true,
                "2"
            },
            new Object[]{
                DIRECTION_ISSUED,
                "oral",
                true,
                true,
                "2"
            },
            new Object[]{
                DECISION_ISSUED,
                "paper",
                false,
                false,
                "1"
            },
            new Object[]{
                DECISION_ISSUED,
                "oral",
                false,
                false,
                "1"
            },
            new Object[]{
                DECISION_ISSUED,
                "oral",
                false,
                true,
                "1"
            },
            new Object[]{
                DECISION_ISSUED,
                "paper",
                false,
                true,
                "1"
            },
            new Object[]{
                DECISION_ISSUED,
                "paper",
                true,
                false,
                "2"
            },
            new Object[]{
                DECISION_ISSUED,
                "oral",
                true,
                false,
                "2"
            },
            new Object[]{
                DECISION_ISSUED,
                "paper",
                true,
                true,
                "2"
            },
            new Object[]{
                DECISION_ISSUED,
                "oral",
                true,
                true,
                "2"
            },
            new Object[]{
                ISSUE_FINAL_DECISION,
                "paper",
                false,
                false,
                "1"
            },
            new Object[]{
                ISSUE_FINAL_DECISION,
                "oral",
                false,
                false,
                "1"
            },
            new Object[]{
                ISSUE_FINAL_DECISION,
                "oral",
                false,
                true,
                "1"
            },
            new Object[]{
                ISSUE_FINAL_DECISION,
                "paper",
                false,
                true,
                "1"
            },
            new Object[]{
                ISSUE_FINAL_DECISION,
                "paper",
                true,
                false,
                "2"
            },
            new Object[]{
                ISSUE_FINAL_DECISION,
                "oral",
                true,
                false,
                "2"
            },
            new Object[]{
                ISSUE_FINAL_DECISION,
                "paper",
                true,
                true,
                "2"
            },
            new Object[]{
                ISSUE_FINAL_DECISION,
                "oral",
                true,
                true,
                "2"
            },
            new Object[]{
                ISSUE_ADJOURNMENT_NOTICE,
                "paper",
                false,
                false,
                "1"
            },
            new Object[]{
                ISSUE_ADJOURNMENT_NOTICE,
                "oral",
                false,
                false,
                "1"
            },
            new Object[]{
                ISSUE_ADJOURNMENT_NOTICE,
                "oral",
                false,
                true,
                "1"
            },
            new Object[]{
                ISSUE_ADJOURNMENT_NOTICE,
                "paper",
                false,
                true,
                "1"
            },
            new Object[]{
                ISSUE_ADJOURNMENT_NOTICE,
                "paper",
                true,
                false,
                "2"
            },
            new Object[]{
                ISSUE_ADJOURNMENT_NOTICE,
                "oral",
                true,
                false,
                "2"
            },
            new Object[]{
                ISSUE_ADJOURNMENT_NOTICE,
                "paper",
                true,
                true,
                "2"
            },
            new Object[]{
                ISSUE_ADJOURNMENT_NOTICE,
                "oral",
                true,
                true,
                "2"
            }
        };
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateRepsNotificationScenariosWhenNoOldCaseRef() {
        return new Object[]{
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "paper",
                Arrays.asList("cab48431-a4f0-41f5-b753-2cecf20ab5d4", "7af36950-fc63-45d1-907d-f472fac7af06"),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "yes",
                "no",
                "2",
                "0",
                "0"
            },
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                "paper",
                Arrays.asList("8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "e93dd744-84a1-4173-847a-6d023b55637f"),
                Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "ee58f7d0-8de7-4bee-acd4-252213db6b7b"),
                Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "1"
            },
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                "oral",
                Arrays.asList("8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "e93dd744-84a1-4173-847a-6d023b55637f"),
                Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "ee58f7d0-8de7-4bee-acd4-252213db6b7b"),
                Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "1"
            },
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                "paper",
                Collections.singletonList("e93dd744-84a1-4173-847a-6d023b55637f"),
                Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "ee58f7d0-8de7-4bee-acd4-252213db6b7b"),
                Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "1"
            },
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "1"
            },
            new Object[]{
                APPEAL_WITHDRAWN_NOTIFICATION,
                "paper",
                Arrays.asList("8620e023-f663-477e-a771-9cfad50ee30f", "e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
                Arrays.asList("d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", "8c719552-6b6b-4df3-9edb-9fe1635755b9"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "2"
            },
            new Object[]{
                APPEAL_WITHDRAWN_NOTIFICATION,
                "oral",
                Arrays.asList("8620e023-f663-477e-a771-9cfad50ee30f", "e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
                Arrays.asList("d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", "8c719552-6b6b-4df3-9edb-9fe1635755b9"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "2"
            },
            new Object[]{
                APPEAL_WITHDRAWN_NOTIFICATION,
                "paper",
                Collections.singletonList("e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
                Arrays.asList("d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", "8c719552-6b6b-4df3-9edb-9fe1635755b9"),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "2"
            },
            new Object[]{
                APPEAL_WITHDRAWN_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", "8c719552-6b6b-4df3-9edb-9fe1635755b9"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "2"
            },
            new Object[]{
                ADJOURNED_NOTIFICATION,
                "paper",
                Arrays.asList("cff1be5f-20cf-4cfa-9a90-4a75d3341ba8", "ecf7db7d-a257-4496-a2bf-768e560c80e7"),
                Arrays.asList("f71772b1-ae1d-49d6-87c6-a41da97a4039", "259b8e81-b44a-4271-a57b-ba7f8bdfcb33"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                ADJOURNED_NOTIFICATION,
                "oral",
                Arrays.asList("cff1be5f-20cf-4cfa-9a90-4a75d3341ba8", "ecf7db7d-a257-4496-a2bf-768e560c80e7"),
                Arrays.asList("f71772b1-ae1d-49d6-87c6-a41da97a4039", "259b8e81-b44a-4271-a57b-ba7f8bdfcb33"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                ADJOURNED_NOTIFICATION,
                "paper",
                Collections.singletonList("ecf7db7d-a257-4496-a2bf-768e560c80e7"),
                Arrays.asList("f71772b1-ae1d-49d6-87c6-a41da97a4039", "259b8e81-b44a-4271-a57b-ba7f8bdfcb33"),
                Collections.emptyList(),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                ADJOURNED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Arrays.asList("b90df52f-c628-409c-8875-4b0b9663a053", "4b1ee55b-abd1-4e7e-b0ed-693d8df1e741"),
                Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b", "99bd4a56-256c-4de8-b187-d43a8dde466f"),
                Arrays.asList("TB-SCS-GNO-ENG-00060.doc", "TB-SCS-GNO-ENG-00079.doc"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "oral",
                Arrays.asList("b90df52f-c628-409c-8875-4b0b9663a053", "4b1ee55b-abd1-4e7e-b0ed-693d8df1e741"),
                Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b", "99bd4a56-256c-4de8-b187-d43a8dde466f"),
                Arrays.asList("TB-SCS-GNO-ENG-00060.doc", "TB-SCS-GNO-ENG-00079.doc"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("4b1ee55b-abd1-4e7e-b0ed-693d8df1e741"),
                Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b", "99bd4a56-256c-4de8-b187-d43a8dde466f"),
                Arrays.asList("TB-SCS-GNO-ENG-00060.doc", "TB-SCS-GNO-ENG-00079.doc"),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00060.doc", "TB-SCS-GNO-ENG-00079.doc"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                APPEAL_DORMANT_NOTIFICATION,
                "paper",
                Arrays.asList("976bdb6c-8a86-48cf-9e0f-7989acaec0c2", "b74ea5d4-dba2-4148-b822-d102cedbea12"),
                Arrays.asList("1aa60c8a-1b6f-4ee1-88ae-51c1cef0ea2b", "4562984e-2854-4191-81d9-cffbe5111015"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                APPEAL_DORMANT_NOTIFICATION,
                "paper",
                Collections.singletonList("b74ea5d4-dba2-4148-b822-d102cedbea12"),
                Arrays.asList("1aa60c8a-1b6f-4ee1-88ae-51c1cef0ea2b", "4562984e-2854-4191-81d9-cffbe5111015"),
                Collections.emptyList(),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                APPEAL_DORMANT_NOTIFICATION,
                "oral",
                Arrays.asList("fc9d0618-68c4-48ec-9481-a84b225a57a9", "e2ee8609-7d56-4857-b3f8-79028e8960aa"),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "yes",
                "no",
                "2",
                "0",
                "0"
            },
            new Object[]{
                APPEAL_DORMANT_NOTIFICATION,
                "oral",
                Collections.singletonList("e2ee8609-7d56-4857-b3f8-79028e8960aa"),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "yes",
                "no",
                "1",
                "0",
                "0"
            },
            new Object[]{
                APPEAL_DORMANT_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "paper",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "paper",
                Collections.singletonList("652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("91143b85-dd9d-430c-ba23-e42ec90f44f8", "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "2"
            },
            new Object[]{
                POSTPONEMENT_NOTIFICATION,
                "paper",
                Arrays.asList("221095a2-aee8-466b-a7ab-beee516cc6cc", "e07b7dba-f383-49ca-a0ba-b5b61be27da6"),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "yes",
                "no",
                "2",
                "0",
                "0"
            },
            new Object[]{
                POSTPONEMENT_NOTIFICATION,
                "oral",
                Arrays.asList("221095a2-aee8-466b-a7ab-beee516cc6cc", "e07b7dba-f383-49ca-a0ba-b5b61be27da6"),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "Yes",
                "no",
                "2",
                "0",
                "0"
            },
            new Object[]{
                POSTPONEMENT_NOTIFICATION,
                "paper",
                Collections.singletonList("e07b7dba-f383-49ca-a0ba-b5b61be27da6"),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "yes",
                "no",
                "1",
                "0",
                "0"
            },
            new Object[]{
                POSTPONEMENT_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                EVIDENCE_REMINDER_NOTIFICATION,
                "oral",
                Arrays.asList("df0803aa-f804-49fe-a2ac-c27adc4bb585"),
                Arrays.asList("5f91012e-0d3f-465b-b301-ee3ee5a50100"),
                Collections.emptyList(),
                "no",
                "no",
                "yes",
                "yes",
                "1",
                "1",
                "0"
            },
            new Object[]{
                EVIDENCE_REMINDER_NOTIFICATION,
                "paper",
                Arrays.asList("81fa38cc-b7cc-469c-8109-67c801dc9c84"),
                Arrays.asList("f1076482-a76d-4389-b411-9865373cfc42"),
                Collections.emptyList(),
                "no",
                "no",
                "yes",
                "yes",
                "1",
                "1",
                "0"
            },
            new Object[]{
                REQUEST_INFO_INCOMPLETE,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00452.docx", "TB-SCS-GNO-ENG-00452.docx"),
                "yes",
                "yes",
                "yes",
                "yes",
                "0",
                "0",
                "0"
            },
            new Object[]{
                REQUEST_INFO_INCOMPLETE,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00452.docx", "TB-SCS-GNO-ENG-00452.docx"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "paper",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "oral",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "paper",
                Collections.singletonList("652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                RESEND_APPEAL_CREATED_NOTIFICATION,
                "paper",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                RESEND_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                RESEND_APPEAL_CREATED_NOTIFICATION,
                "paper",
                Collections.singletonList("652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                NON_COMPLIANT_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("114cadaa-7760-4699-9add-d3a252f68909", "5745f77c-6512-4082-8c34-63851f24eab1"),
                "yes",
                "yes",
                "yes",
                "yes",
                "0",
                "0",
                "2"
            },
            new Object[]{
                NON_COMPLIANT_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("114cadaa-7760-4699-9add-d3a252f68909", "5745f77c-6512-4082-8c34-63851f24eab1"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "2"
            },
            new Object[]{
                DIRECTION_ISSUED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00067.docx", "TB-SCS-GNO-ENG-00089.docx"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                DECISION_ISSUED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00067.docx", "TB-SCS-GNO-ENG-00089.docx"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                ISSUE_FINAL_DECISION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00067.docx", "TB-SCS-GNO-ENG-00089.docx"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
                new Object[]{
                ISSUE_ADJOURNMENT_NOTICE,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00067.docx", "TB-SCS-GNO-ENG-00089.docx"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
        };
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateAppointeeNotificationScenarios() {
        return new Object[]{
            new Object[]{
                    STRUCK_OUT,
                    "oral",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.singletonList("TB-SCS-GNO-ENG-00466.docx"),
                    "no",
                    "no",
                    "0",
                    "0",
                    "0",
                    "Appointee Appointee"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "0",
                "0",
                "0",
                ""
            },
            new Object[]{
                DWP_RESPONSE_RECEIVED_NOTIFICATION,
                "oral",
                Collections.singletonList("2c5644db-1f7b-429b-b10a-8b23a80ed26a"),
                Collections.singletonList("f20ffcb1-c5f0-4bff-b2d1-a1094f8014e6"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                DWP_RESPONSE_RECEIVED_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                DWP_UPLOAD_RESPONSE_NOTIFICATION,
                "oral",
                Collections.singletonList("2c5644db-1f7b-429b-b10a-8b23a80ed26a"),
                Collections.singletonList("f20ffcb1-c5f0-4bff-b2d1-a1094f8014e6"),
                Collections.singletonList("TB-SCS-GNO-ENG-00261.doc"),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                DWP_UPLOAD_RESPONSE_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00261.doc"),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                EVIDENCE_REMINDER_NOTIFICATION,
                "oral",
                Arrays.asList("b9e47ec4-3b58-4b8d-9304-f77ac27fb7f2"),
                Arrays.asList("e3f71440-d1ac-43c8-a8cc-a088c4f3c959"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                EVIDENCE_REMINDER_NOTIFICATION,
                "paper",
                Arrays.asList("c507a630-9e6a-43c9-8e39-dcabdcffaf53"),
                Arrays.asList("56a6c0c8-a251-482d-be83-95a7a1bf528c"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("c5654134-2e13-4541-ac73-334a5b5cdbb6"),
                Collections.singletonList("74bda35f-040b-4355-bda3-faf0e4f5ae6e"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "oral",
                Arrays.asList("c5654134-2e13-4541-ac73-334a5b5cdbb6"),
                Arrays.asList("74bda35f-040b-4355-bda3-faf0e4f5ae6e"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                SUBSCRIPTION_UPDATED_NOTIFICATION,
                "oral",
                Arrays.asList("b8b2904f-629d-42cf-acea-1b74bde5b2ff", "03b957bf-e21d-4147-90c1-b6fefa8cf70d"),
                Arrays.asList("7397a76f-14cb-468c-b1a7-0570940ead91", "759c712a-6b55-485e-bcf7-1cf5c4896eb1"),
                Collections.emptyList(),
                "yes",
                "yes",
                "2",
                "2",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                SUBSCRIPTION_UPDATED_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Harry Potter"
            },
            new Object[]{
                DIRECTION_ISSUED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00067.docx"),
                "yes",
                "yes",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                DECISION_ISSUED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00067.docx"),
                "yes",
                "yes",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                ISSUE_FINAL_DECISION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00067.docx"),
                "yes",
                "yes",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                ISSUE_ADJOURNMENT_NOTICE,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00067.docx"),
                "yes",
                "yes",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("08365e91-9e07-4a5c-bf96-ef56fd0ada63"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                Collections.singletonList("TB-SCS-GNO-ENG-00060.doc"),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "oral",
                Collections.singletonList("08365e91-9e07-4a5c-bf96-ef56fd0ada63"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                Collections.singletonList("TB-SCS-GNO-ENG-00060.doc"),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00060.doc"),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                CASE_UPDATED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Harry Potter"
            },
            new Object[]{
                HEARING_REMINDER_NOTIFICATION,
                "oral",
                Collections.singletonList("774a5cba-fab6-4b8c-a9d9-03f913ed2dca"),
                Collections.singletonList("fcb93790-8308-4f4f-9674-65b777b7fe8c"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                HEARING_REMINDER_NOTIFICATION,
                "oral",
                Collections.singletonList("774a5cba-fab6-4b8c-a9d9-03f913ed2dca"),
                Collections.emptyList(),
                Collections.emptyList(),
                "yes",
                "no",
                "1",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                HEARING_REMINDER_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.singletonList("fcb93790-8308-4f4f-9674-65b777b7fe8c"),
                Collections.emptyList(),
                "no",
                "yes",
                "0",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                HEARING_REMINDER_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "0",
                "0",
                "0",
                ""
            },
            new Object[]{
                REQUEST_INFO_INCOMPLETE,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00452.docx"),
                "yes",
                "yes",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                REQUEST_INFO_INCOMPLETE,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00452.docx"),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                NON_COMPLIANT_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("114cadaa-7760-4699-9add-d3a252f68909"),
                "yes",
                "yes",
                "0",
                "0",
                "1",
                "Appointee Appointee"
            },
            new Object[]{
                NON_COMPLIANT_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("114cadaa-7760-4699-9add-d3a252f68909"),
                "no",
                "no",
                "0",
                "0",
                "1",
                "Appointee Appointee"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "0",
                "0",
                "0",
                ""
            },
            new Object[]{
                RESEND_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                RESEND_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                RESEND_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "0",
                "0",
                "0",
                ""
            }
        };
    }


    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateAppointeeNotificationWhenNoOldCaseReferenceScenarios() {
        return new Object[]{
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                EVIDENCE_RECEIVED_NOTIFICATION,
                "oral",
                Collections.singletonList("c5654134-2e13-4541-ac73-334a5b5cdbb6"),
                Collections.singletonList("74bda35f-040b-4355-bda3-faf0e4f5ae6e"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                SYA_APPEAL_CREATED_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("747d026e-1bec-4e96-8a34-28f36e30bba5"),
                "no",
                "no",
                "0",
                "0",
                "1",
                "Appointee Appointee"
            },
            new Object[]{
                DWP_RESPONSE_RECEIVED_NOTIFICATION,
                "oral",
                Collections.singletonList("2c5644db-1f7b-429b-b10a-8b23a80ed26a"),
                Collections.singletonList("f20ffcb1-c5f0-4bff-b2d1-a1094f8014e6"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                DWP_RESPONSE_RECEIVED_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("8b11f3f4-6452-4a35-93d8-a94996af6499"),
                "no",
                "no",
                "0",
                "0",
                "1",
                "Appointee Appointee"
            },
            new Object[]{
                DWP_UPLOAD_RESPONSE_NOTIFICATION,
                "oral",
                Collections.singletonList("2c5644db-1f7b-429b-b10a-8b23a80ed26a"),
                Collections.singletonList("f20ffcb1-c5f0-4bff-b2d1-a1094f8014e6"),
                Collections.singletonList("TB-SCS-GNO-ENG-00261.doc"),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                DWP_UPLOAD_RESPONSE_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00261.doc"),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                EVIDENCE_REMINDER_NOTIFICATION,
                "oral",
                Collections.singletonList("b9e47ec4-3b58-4b8d-9304-f77ac27fb7f2"),
                Collections.singletonList("e3f71440-d1ac-43c8-a8cc-a088c4f3c959"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                EVIDENCE_REMINDER_NOTIFICATION,
                "paper",
                Arrays.asList("c507a630-9e6a-43c9-8e39-dcabdcffaf53"),
                Arrays.asList("56a6c0c8-a251-482d-be83-95a7a1bf528c"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                "oral",
                Collections.singletonList("8ce8d794-75e8-49a0-b4d2-0c6cd2061c11"),
                Collections.singletonList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"),
                    Arrays.asList("85a81e3c-a59f-4ae9-9c99-266dfd5ca95f"),
                "yes",
                "yes",
                "1",
                "1",
                "1",
                "Appointee Appointee"
            },
            new Object[]{
                SUBSCRIPTION_UPDATED_NOTIFICATION,
                "oral",
                Arrays.asList("b8b2904f-629d-42cf-acea-1b74bde5b2ff", "03b957bf-e21d-4147-90c1-b6fefa8cf70d"),
                Arrays.asList("7397a76f-14cb-468c-b1a7-0570940ead91", "759c712a-6b55-485e-bcf7-1cf5c4896eb1"),
                Collections.emptyList(),
                "yes",
                "yes",
                "2",
                "2",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("08365e91-9e07-4a5c-bf96-ef56fd0ada63"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                Collections.singletonList("TB-SCS-GNO-ENG-00060.doc"),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                ADJOURNED_NOTIFICATION,
                "oral",
                Collections.singletonList("cff1be5f-20cf-4cfa-9a90-4a75d3341ba8"),
                Collections.singletonList("f71772b1-ae1d-49d6-87c6-a41da97a4039"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                DWP_RESPONSE_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("e1084d78-5e2d-45d2-a54f-84339da141c1"),
                Collections.singletonList("505be856-ceca-4bbc-ba70-29024585056f"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                DWP_UPLOAD_RESPONSE_NOTIFICATION,
                "paper",
                Collections.singletonList("e1084d78-5e2d-45d2-a54f-84339da141c1"),
                Collections.singletonList("505be856-ceca-4bbc-ba70-29024585056f"),
                Collections.singletonList("TB-SCS-GNO-ENG-00261.doc"),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                HEARING_BOOKED_NOTIFICATION,
                "oral",
                Collections.singletonList("fee16753-0bdb-43f1-9abb-b14b826e3b26"),
                Collections.singletonList("693c9bfb-151e-4add-a6f2-af1cbc94eef5"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "oral",
                Collections.singletonList("08365e91-9e07-4a5c-bf96-ef56fd0ada63"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                Collections.singletonList("TB-SCS-GNO-ENG-00060.doc"),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_WITHDRAWN_NOTIFICATION,
                "oral",
                Collections.singletonList("8620e023-f663-477e-a771-9cfad50ee30f"),
                Collections.singletonList("446c7b23-7342-42e1-adff-b4c367e951cb"),
                Collections.singletonList("d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d"),
                "yes",
                "yes",
                "1",
                "1",
                "1",
                "Appointee Appointee"
            },
            new Object[]{
                SUBSCRIPTION_UPDATED_NOTIFICATION,
                "oral",
                Arrays.asList("b8b2904f-629d-42cf-acea-1b74bde5b2ff", "03b957bf-e21d-4147-90c1-b6fefa8cf70d"),
                Arrays.asList("7397a76f-14cb-468c-b1a7-0570940ead91", "759c712a-6b55-485e-bcf7-1cf5c4896eb1"),
                Collections.emptyList(),
                "yes",
                "yes",
                "2",
                "2",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                SUBSCRIPTION_UPDATED_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Harry Potter"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("08365e91-9e07-4a5c-bf96-ef56fd0ada63"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                Collections.singletonList("TB-SCS-GNO-ENG-00060.doc"),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "oral",
                Collections.singletonList("08365e91-9e07-4a5c-bf96-ef56fd0ada63"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                Collections.singletonList("TB-SCS-GNO-ENG-00060.doc"),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00060.doc"),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                REQUEST_INFO_INCOMPLETE,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00452.docx"),
                "yes",
                "yes",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                REQUEST_INFO_INCOMPLETE,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00452.docx"),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                NON_COMPLIANT_NOTIFICATION,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("114cadaa-7760-4699-9add-d3a252f68909"),
                "yes",
                "yes",
                "0",
                "0",
                "1",
                "Appointee Appointee"
            },
            new Object[]{
                NON_COMPLIANT_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("114cadaa-7760-4699-9add-d3a252f68909"),
                "no",
                "no",
                "0",
                "0",
                "1",
                "Appointee Appointee"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            }
        };
    }

    @Test
    public void shouldSendNotificationForHearingBookedRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "hearingBooked");
        json = json.replace("2018-01-12", LocalDate.now().plusDays(2).toString());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(2)).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForHearingBookedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "hearingBooked");
        json = json.replace("2018-01-12", LocalDate.now().plusDays(2).toString());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any());
        verify(notificationClient, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForHearingBookedRequestForHearingInThePastForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "hearingBooked");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any());
        verify(notificationClient, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendAppellantNotificationForEvidenceReminderForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "evidenceReminder");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(eq("b9e47ec4-3b58-4b8d-9304-f77ac27fb7f2"), any(), any(), any());
        verify(notificationClient).sendSms(eq("e3f71440-d1ac-43c8-a8cc-a088c4f3c959"), any(), any(), any(), any());
    }

    @Test
    public void shouldSendAppellantNotificationForEvidenceReminderForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "evidenceReminder");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(eq("c507a630-9e6a-43c9-8e39-dcabdcffaf53"), any(), any(), any());
        verify(notificationClient).sendSms(eq("56a6c0c8-a251-482d-be83-95a7a1bf528c"), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForHearingReminderForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "hearingReminder");
        json = json.replace("2018-01-12", LocalDate.now().plusDays(2).toString());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(2)).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForHearingReminderForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "hearingReminder");
        json = json.replace("2018-01-12", LocalDate.now().plusDays(2).toString());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any());
        verify(notificationClient, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForSyaAppealCreatedRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "appealCreated");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, times(1)).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(2)).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForSyaAppealCreatedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "appealCreated");
        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, times(1)).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(2)).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendSubscriptionCreatedNotificationForSubscriptionUpdatedRequestWithNewSubscribeSmsRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "subscriptionUpdated");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeEmail");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any());
        verify(notificationClient).sendSms(eq("7397a76f-14cb-468c-b1a7-0570940ead91"), any(), any(), any(), any());
    }

    /*@Test
    public void shouldSendSubscriptionCreatedNotificationForSubscriptionUpdatedRequestWithNewSubscribeSmsRequestForAPaperHearingWithRepSubscribedToSms() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "subscriptionUpdated");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeEmail");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendSms(eq(subscriptionCreatedSmsId), any(), any(), any(), any());
        verify(notificationClient).sendSms(eq(paperResponseReceivedSmsId), any(), any(), any(), any());
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    public void shouldSendSubscriptionUpdatedNotificationForSubscriptionUpdatedRequestWithNewEmailAddressForAnOralHearingWhenAlreadySubscribedToSms() throws Exception {
        json = updateEmbeddedJson(json, "subscriptionUpdated", "event_id");
        json = updateEmbeddedJson(json, "oral", "case_details", "case_data", "appeal", "hearingType");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions",
                "appellantSubscription", "subscribeSms");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(eq(subscriptionUpdatedEmailId), any(), any(), any());
        verify(notificationClient).sendEmail(eq(oralResponseReceivedEmailId), any(), any(), any());
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    public void shouldSendSubscriptionUpdatedNotificationForSubscriptionUpdatedRequestWithNewEmailAddressForAPaperHearingWhenRepAlreadySubscriptedToSms() throws Exception {
        updateJsonForPaperHearing();
        json = updateEmbeddedJson(json, "subscriptionUpdated", "event_id");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions",
                "appellantSubscription", "subscribeSms");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(eq(subscriptionUpdatedEmailId), any(), any(), any());
        verify(notificationClient).sendEmail(eq(paperResponseReceivedEmailId), any(), any(), any());
        verifyNoMoreInteractions(notificationClient);
    }*/

    @Test
    public void shouldNotSendSubscriptionUpdatedNotificationForSubscriptionUpdatedRequestWithSameEmailAddress() throws Exception {
        json = json.replace("appealReceived", "subscriptionUpdated");
        json = json.replace("sscstest@greencroftconsulting.com", "tester@hmcts.net");

        json = updateEmbeddedJson(json, "Yes", "case_details_before", "case_data", "subscriptions", "appellantSubscription", "subscribeEmail");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeSms");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any());
        verify(notificationClient, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void givenAnUnknownRpcCase_thenDoNotProcessNotifications() throws Exception {
        String path = getClass().getClassLoader().getResource("json/ccdResponseWithNoOldCaseRef.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = json.replace("appealReceived", "appealCreated");
        json = json.replace("SC022", "SC948");

        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeEmail");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeSms");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "representativeSubscription", "subscribeEmail");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "representativeSubscription", "subscribeSms");
        json = updateEmbeddedJson(json, "No", "case_details_before", "case_data", "subscriptions", "representativeSubscription", "subscribeSms");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any());
        verify(notificationClient, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldReturn400WhenAuthHeaderIsMissing() throws Exception {
        HttpServletResponse response = getResponse(getRequestWithoutAuthHeader(json));

        assertHttpStatus(response, HttpStatus.BAD_REQUEST);
        verify(authorisationService, never()).authorise(anyString());
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any(), any());
    }

    @Test
    @Parameters({"subscriptionUpdated", "appealReceived", "directionIssued", "nonCompliant"})
    public void shouldNotSendNotificationWhenAppealDormantAndNotificationType(String notificationEventType) throws Exception {
        json = json.replace("appealCreated", State.DORMANT_APPEAL_STATE.toString());
        json = json.replace("appealReceived", notificationEventType);

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any());
        verify(notificationClient, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    @Parameters({"appealLapsed", "appealDormant"})
    public void shouldSendNotificationWhenAppealDormantAndNotificationType(String notificationEventType) throws Exception {
        json = json.replace("appealCreated", State.DORMANT_APPEAL_STATE.toString());
        json = json.replace("appealReceived", notificationEventType);

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, atMostOnce()).sendEmail(any(), any(), any(), any());
        verify(notificationClient, atMost(2)).sendSms(any(), any(), any(), any(), any());
        verify(notificationClient, atMostOnce()).sendPrecompiledLetterWithInputStream(any(), any());

        if (notificationEventType.equals(APPEAL_LAPSED_NOTIFICATION.getId())) {
            verify(notificationClient, atMostOnce()).sendLetter(any(), any(), any());
        }
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    @Parameters({"appealWithdrawn", "directionIssued"})
    public void shouldSendNotificationLetterWhenAppealDormantAndNotificationType(String notificationEventType) throws Exception {
        json = json.replace("appealCreated", State.DORMANT_APPEAL_STATE.toString());
        json = json.replace("appealReceived", notificationEventType);

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, atMostOnce()).sendEmail(any(), any(), any(), any());
        verify(notificationClient, atMost(2)).sendSms(any(), any(), any(), any(), any());
        verify(notificationClient, atMost(2)).sendLetter(any(), any(), any());
        if (notificationEventType.equals(DIRECTION_ISSUED.getId())) {
            verify(notificationClient, atMost(2)).sendPrecompiledLetterWithInputStream(any(), any());
        }
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    public void givenAStruckOutEvent_shouldStillSendStruckOutNotificationWhenAppealDormant() throws Exception {

        String filename = "json/ccdResponse_struckOut.json";
        String path = getClass().getClassLoader().getResource(filename).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = json.replace("appealCreated", State.DORMANT_APPEAL_STATE.toString());

        byte[] sampleDirectionNotice = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-text.pdf"));
        when(evidenceManagementService.download(any(), any())).thenReturn(sampleDirectionNotice);

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, atMostOnce()).sendEmail(any(), any(), any(), any());
        verify(notificationClient, atMost(2)).sendSms(any(), any(), any(), any(), any());
        verify(notificationClient, atMostOnce()).sendPrecompiledLetterWithInputStream(any(), any());
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    @Parameters({"adjournCase", "issueFinalDecision", "decisionIssued", "directionIssued"})
    public void givenAReissueEvent_shouldStillSendDirectionIssued(String furtherEvidenceType) throws Exception {

        String filename = "json/ccdResponse_reissueDocument.json";
        String path = getClass().getClassLoader().getResource(filename).getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = json.replace("appealCreated", State.DORMANT_APPEAL_STATE.toString());
        json = json.replace("REISSUE_DOCUMENT", furtherEvidenceType);

        byte[] sampleDirectionNotice = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-text.pdf"));
        when(evidenceManagementService.download(any(), any())).thenReturn(sampleDirectionNotice);

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, times(0)).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(0)).sendSms(any(), any(), any(), any(), any());
        verify(notificationClient, atMostOnce()).sendPrecompiledLetterWithInputStream(any(), any());
        verifyNoMoreInteractions(notificationClient);
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

    private void updateJsonForPaperHearing() throws IOException {
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "appeal", "hearingOptions", "wantsToAttend");
        json = updateEmbeddedJson(json, "paper", "case_details", "case_data", "appeal", "hearingType");
    }

}
