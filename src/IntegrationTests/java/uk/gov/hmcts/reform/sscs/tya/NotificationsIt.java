package uk.gov.hmcts.reform.sscs.tya;

import static helper.IntegrationTestHelper.assertHttpStatus;
import static helper.IntegrationTestHelper.getRequestWithAuthHeader;
import static helper.IntegrationTestHelper.getRequestWithoutAuthHeader;
import static helper.IntegrationTestHelper.updateEmbeddedJson;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.APPELLANT_NAME;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.NAME;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.NotificationBlacklist;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.controller.NotificationController;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.*;
import uk.gov.service.notify.*;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.NotificationHandler;
import uk.gov.hmcts.reform.sscs.service.NotificationSender;
import uk.gov.hmcts.reform.sscs.service.NotificationService;
import uk.gov.hmcts.reform.sscs.service.NotificationValidService;
import uk.gov.hmcts.reform.sscs.service.OutOfHoursCalculator;
import uk.gov.hmcts.reform.sscs.service.ReminderService;
import uk.gov.hmcts.reform.sscs.service.SendNotificationService;
import uk.gov.hmcts.reform.sscs.service.SscsGeneratePdfService;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class NotificationsIt {
    private static final String TEMPLATE_PATH = "/templates/non_compliant_case_letter_template.html";

    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    MockMvc mockMvc;

    NotificationController controller;

    @Mock
    NotificationClient notificationClient;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Mock
    private SendSmsResponse sendSmsResponse;

    @Mock
    private SendLetterResponse sendLetterResponse;

    @Mock
    ReminderService reminderService;

    @Autowired
    NotificationValidService notificationValidService;

    @MockBean
    private AuthorisationService authorisationService;

    @Mock
    NotificationBlacklist notificationBlacklist;

    @Autowired
    NotificationFactory factory;

    @Autowired
    private CcdService ccdService;

    @Autowired
    private SscsCaseDataWrapperDeserializer deserializer;

    @MockBean
    private IdamService idamService;

    String json;

    @Autowired
    private NotificationHandler notificationHandler;

    @MockBean
    private OutOfHoursCalculator outOfHoursCalculator;

    @Autowired
    private NotificationConfig notificationConfig;

    @Autowired
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private SscsGeneratePdfService sscsGeneratePdfService;

    @Before
    public void setup() throws Exception {
        NotificationSender sender = new NotificationSender(notificationClient, null, notificationBlacklist);

        SendNotificationService sendNotificationService = new SendNotificationService(sender, evidenceManagementService, sscsGeneratePdfService, notificationHandler, notificationValidService);
        ReflectionTestUtils.setField(sendNotificationService, "bundledLettersOn", true);
        ReflectionTestUtils.setField(sendNotificationService, "lettersOn", true);

        NotificationService service = new NotificationService(factory, reminderService, notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService);
        controller = new NotificationController(service, authorisationService, ccdService, deserializer, idamService);
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
        when(sendLetterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        outOfHoursCalculator = mock(OutOfHoursCalculator.class);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);
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
    @Parameters({
            "oral, 01caec0c-191b-4a32-882a-6fded2546ce6, 317a121e-d08c-4890-b3b3-4652f741771f",
            "paper, a64bce9a-9162-47ca-b3e7-cf5f85ca7bdc, f5b61f94-0b2b-4e8e-9c25-56e9830df7d4"
    })
    public void shouldSendNotificationForAnResponseReceivedRequestForAnOralOrPaperHearing(
            String hearingType, String emailTemplateId, String smsTemplateId) throws Exception {
        json = updateEmbeddedJson(json, hearingType, "case_details", "case_data", "appeal", "hearingType");
        json = updateEmbeddedJson(json, "responseReceived", "event_id");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(eq(emailTemplateId), any(), any(), any());
        verify(notificationClient).sendSms(eq(smsTemplateId), any(), any(), any(), any());
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
    public void shouldSendNotificationForAnEvidenceReceivedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "evidenceReceived");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(2)).sendSms(any(), any(), any(), any(), any());
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

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));
        assertHttpStatus(response, HttpStatus.OK);

        String expectedName = "Harry Potter";
        validateEmailNotifications(expectedEmailTemplateIds, wantedNumberOfSendEmailInvocations, expectedName);
        validateSmsNotifications(expectedSmsTemplateIds, wantedNumberOfSendSmsInvocations);
        validateLetterNotifications(expectedLetterTemplateIds, wantedNumberOfSendLetterInvocations, expectedName);
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
        List<String> expectedSmsTemplateIds, List<String> expectedLetterTemplateIds, String appointeeEmailSubs, String appointeeSmsSubs,
        int wantedNumberOfSendEmailInvocations, int wantedNumberOfSendSmsInvocations, int wantedNumberOfSendLetterInvocations, String expectedName) throws Exception {
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
        NotificationEventType notificationEventType, String hearingType, List<String> expectedEmailTemplateIds,
        List<String> expectedSmsTemplateIds, List<String> expectedLetterTemplateIds, String appointeeEmailSubs, String appointeeSmsSubs,
        int wantedNumberOfSendEmailInvocations, int wantedNumberOfSendSmsInvocations, int wantedNumberOfSendLetterInvocations, String expectedName) throws Exception {
        String path = getClass().getClassLoader().getResource("json/ccdResponseWithAppointeeWithNoOldCaseRef.json").getFile();
        String jsonAppointee = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        jsonAppointee = updateEmbeddedJson(jsonAppointee, hearingType, "case_details", "case_data", "appeal", "hearingType");

        jsonAppointee = updateEmbeddedJson(jsonAppointee, appointeeEmailSubs, "case_details", "case_data", "subscriptions",
            "appointeeSubscription", "subscribeEmail");
        jsonAppointee = updateEmbeddedJson(jsonAppointee, appointeeSmsSubs, "case_details", "case_data", "subscriptions",
            "appointeeSubscription", "subscribeSms");

        jsonAppointee = updateEmbeddedJson(jsonAppointee, notificationEventType.getId(), "event_id");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(jsonAppointee));

        assertHttpStatus(response, HttpStatus.OK);

        validateEmailNotifications(expectedEmailTemplateIds, wantedNumberOfSendEmailInvocations, expectedName);
        validateSmsNotifications(expectedSmsTemplateIds, wantedNumberOfSendSmsInvocations);
        validateLetterNotifications(expectedLetterTemplateIds, wantedNumberOfSendLetterInvocations, expectedName);
    }

    private void validateEmailNotifications(List<String> expectedEmailTemplateIds, int wantedNumberOfSendEmailInvocations, String expectedName) throws NotificationClientException {
        ArgumentCaptor<String> emailTemplateIdCaptor = ArgumentCaptor.forClass(String.class);
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
        verify(notificationClient, times(wantedNumberOfSendLetterInvocations))
            .sendLetter(letterTemplateIdCaptor.capture(), letterPersonalisationCaptor.capture(), any());
        assertArrayEquals(expectedLetterTemplateIds.toArray(), letterTemplateIdCaptor.getAllValues().toArray());

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
    private Object[] generateRepsNotificationScenarios() {
        return new Object[]{
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                "paper",
                Arrays.asList("8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "e93dd744-84a1-4173-847a-6d023b55637f"),
                Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "ee58f7d0-8de7-4bee-acd4-252213db6b7b"),
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
                APPEAL_LAPSED_NOTIFICATION,
                "oral",
                Arrays.asList("8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "e93dd744-84a1-4173-847a-6d023b55637f"),
                Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "ee58f7d0-8de7-4bee-acd4-252213db6b7b"),
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
                APPEAL_LAPSED_NOTIFICATION,
                "paper",
                Collections.singletonList("e93dd744-84a1-4173-847a-6d023b55637f"),
                Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "ee58f7d0-8de7-4bee-acd4-252213db6b7b"),
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
                APPEAL_LAPSED_NOTIFICATION,
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
                APPEAL_WITHDRAWN_NOTIFICATION,
                "paper",
                Arrays.asList("8620e023-f663-477e-a771-9cfad50ee30f", "e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
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
                APPEAL_WITHDRAWN_NOTIFICATION,
                "oral",
                Arrays.asList("8620e023-f663-477e-a771-9cfad50ee30f", "e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
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
                APPEAL_WITHDRAWN_NOTIFICATION,
                "paper",
                Collections.singletonList("e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
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
                APPEAL_WITHDRAWN_NOTIFICATION,
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
                ADJOURNED_NOTIFICATION,
                "paper",
                Arrays.asList("bff02237-9bcb-49fa-bbf7-11725b97132a", "75357eb8-bba7-4bdf-b879-b535bc3fb50a"),
                Arrays.asList("46c6bf06-33dd-4e5a-9b6b-8bd6d0eb33b1", "a170d63e-b04e-4da5-ad89-d93644b6c1e9"),
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
                Arrays.asList("bff02237-9bcb-49fa-bbf7-11725b97132a", "75357eb8-bba7-4bdf-b879-b535bc3fb50a"),
                Arrays.asList("46c6bf06-33dd-4e5a-9b6b-8bd6d0eb33b1", "a170d63e-b04e-4da5-ad89-d93644b6c1e9"),
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
                Collections.singletonList("75357eb8-bba7-4bdf-b879-b535bc3fb50a"),
                Arrays.asList("46c6bf06-33dd-4e5a-9b6b-8bd6d0eb33b1", "a170d63e-b04e-4da5-ad89-d93644b6c1e9"),
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
                APPEAL_RECEIVED_NOTIFICATION,
                "oral",
                Arrays.asList("b90df52f-c628-409c-8875-4b0b9663a053", "4b1ee55b-abd1-4e7e-b0ed-693d8df1e741"),
                Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b", "99bd4a56-256c-4de8-b187-d43a8dde466f"),
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
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("4b1ee55b-abd1-4e7e-b0ed-693d8df1e741"),
                Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b", "99bd4a56-256c-4de8-b187-d43a8dde466f"),
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
                APPEAL_RECEIVED_NOTIFICATION,
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
                Arrays.asList("fc9d0618-68c4-48ec-9481-a84b225a57a9","e2ee8609-7d56-4857-b3f8-79028e8960aa"),
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
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "0e44927e-168b-4510-ac57-6932fda7aec1"),
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
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "0e44927e-168b-4510-ac57-6932fda7aec1"),
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
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "0e44927e-168b-4510-ac57-6932fda7aec1"),
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
                Arrays.asList("08959288-e09a-472d-80b8-af79bfcbb437", "0a48bd48-f79c-4863-b6e3-e8fa69019c34"),
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
                Arrays.asList("08959288-e09a-472d-80b8-af79bfcbb437", "0a48bd48-f79c-4863-b6e3-e8fa69019c34"),
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
                Collections.singletonList("0a48bd48-f79c-4863-b6e3-e8fa69019c34"),
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
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
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
                CASE_UPDATED,
                "oral",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
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
                CASE_UPDATED,
                "paper",
                Collections.singletonList("652753bf-59b4-46eb-9c24-bd762338a098"),
                Collections.emptyList(),
                Collections.singletonList("91143b85-dd9d-430c-ba23-e42ec90f44f8"),
                "no",
                "no",
                "yes",
                "no",
                "1",
                "0",
                "1"
            },
            new Object[]{
                CASE_UPDATED,
                "paper",
                Collections.singletonList("01293b93-b23e-40a3-ad78-2c6cd01cd21c"),
                Collections.emptyList(),
                Collections.singletonList("77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf"),
                "yes",
                "no",
                "no",
                "no",
                "1",
                "0",
                "1"
            },
            new Object[]{
                CASE_UPDATED,
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
            }
        };
    }


    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateRepsNotificationScenariosWhenNoOldCaseRef() {
        return new Object[]{
            new Object[]{
                APPEAL_LAPSED_NOTIFICATION,
                "paper",
                Arrays.asList("8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "e93dd744-84a1-4173-847a-6d023b55637f"),
                Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "ee58f7d0-8de7-4bee-acd4-252213db6b7b"),
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
                APPEAL_LAPSED_NOTIFICATION,
                "oral",
                Arrays.asList("8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "e93dd744-84a1-4173-847a-6d023b55637f"),
                Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "ee58f7d0-8de7-4bee-acd4-252213db6b7b"),
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
                APPEAL_LAPSED_NOTIFICATION,
                "paper",
                Collections.singletonList("e93dd744-84a1-4173-847a-6d023b55637f"),
                Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "ee58f7d0-8de7-4bee-acd4-252213db6b7b"),
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
                APPEAL_LAPSED_NOTIFICATION,
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
                APPEAL_WITHDRAWN_NOTIFICATION,
                "paper",
                Arrays.asList("8620e023-f663-477e-a771-9cfad50ee30f", "e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
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
                APPEAL_WITHDRAWN_NOTIFICATION,
                "oral",
                Arrays.asList("8620e023-f663-477e-a771-9cfad50ee30f", "e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
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
                APPEAL_WITHDRAWN_NOTIFICATION,
                "paper",
                Collections.singletonList("e29a2275-553f-4e70-97f4-2994c095f281"),
                Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb", "f59440ee-19ca-4d47-a702-13e9cecaccbd"),
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
                APPEAL_WITHDRAWN_NOTIFICATION,
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
                ADJOURNED_NOTIFICATION,
                "paper",
                Arrays.asList("bff02237-9bcb-49fa-bbf7-11725b97132a", "75357eb8-bba7-4bdf-b879-b535bc3fb50a"),
                Arrays.asList("46c6bf06-33dd-4e5a-9b6b-8bd6d0eb33b1", "a170d63e-b04e-4da5-ad89-d93644b6c1e9"),
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
                Arrays.asList("bff02237-9bcb-49fa-bbf7-11725b97132a", "75357eb8-bba7-4bdf-b879-b535bc3fb50a"),
                Arrays.asList("46c6bf06-33dd-4e5a-9b6b-8bd6d0eb33b1", "a170d63e-b04e-4da5-ad89-d93644b6c1e9"),
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
                Collections.singletonList("75357eb8-bba7-4bdf-b879-b535bc3fb50a"),
                Arrays.asList("46c6bf06-33dd-4e5a-9b6b-8bd6d0eb33b1", "a170d63e-b04e-4da5-ad89-d93644b6c1e9"),
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
                APPEAL_RECEIVED_NOTIFICATION,
                "oral",
                Arrays.asList("b90df52f-c628-409c-8875-4b0b9663a053", "4b1ee55b-abd1-4e7e-b0ed-693d8df1e741"),
                Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b", "99bd4a56-256c-4de8-b187-d43a8dde466f"),
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
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("4b1ee55b-abd1-4e7e-b0ed-693d8df1e741"),
                Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b", "99bd4a56-256c-4de8-b187-d43a8dde466f"),
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
                APPEAL_RECEIVED_NOTIFICATION,
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
                Arrays.asList("fc9d0618-68c4-48ec-9481-a84b225a57a9","e2ee8609-7d56-4857-b3f8-79028e8960aa"),
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
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "0e44927e-168b-4510-ac57-6932fda7aec1"),
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
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "0e44927e-168b-4510-ac57-6932fda7aec1"),
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
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "0e44927e-168b-4510-ac57-6932fda7aec1"),
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
                Arrays.asList("08959288-e09a-472d-80b8-af79bfcbb437", "0a48bd48-f79c-4863-b6e3-e8fa69019c34"),
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
                Arrays.asList("08959288-e09a-472d-80b8-af79bfcbb437", "0a48bd48-f79c-4863-b6e3-e8fa69019c34"),
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
                Collections.singletonList("0a48bd48-f79c-4863-b6e3-e8fa69019c34"),
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
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
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
                CASE_UPDATED,
                "oral",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
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
                CASE_UPDATED,
                "paper",
                Collections.singletonList("652753bf-59b4-46eb-9c24-bd762338a098"),
                Collections.emptyList(),
                Collections.singletonList("91143b85-dd9d-430c-ba23-e42ec90f44f8"),
                "no",
                "no",
                "yes",
                "no",
                "1",
                "0",
                "1"
            },
            new Object[]{
                CASE_UPDATED,
                "paper",
                Collections.singletonList("01293b93-b23e-40a3-ad78-2c6cd01cd21c"),
                Collections.emptyList(),
                Collections.singletonList("77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf"),
                "yes",
                "no",
                "no",
                "no",
                "1",
                "0",
                "1"
            },
            new Object[]{
                CASE_UPDATED,
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
            }
        };
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateAppointeeNotificationScenarios() {
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
                "Harry Potter"
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
                "Harry Potter"
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
                Collections.singletonList("01caec0c-191b-4a32-882a-6fded2546ce6"),
                Collections.singletonList("317a121e-d08c-4890-b3b3-4652f741771f"),
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
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("08365e91-9e07-4a5c-bf96-ef56fd0ada63"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Harry Potter"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "oral",
                Collections.singletonList("08365e91-9e07-4a5c-bf96-ef56fd0ada63"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Harry Potter"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
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
                CASE_UPDATED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("91143b85-dd9d-430c-ba23-e42ec90f44f8"),
                "no",
                "no",
                "0",
                "0",
                "1",
                "Harry Potter"
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
                "Harry Potter"
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
                "Harry Potter"
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
                "Harry Potter"
            },
            new Object[]{
                DWP_RESPONSE_RECEIVED_NOTIFICATION,
                "oral",
                Collections.singletonList("01caec0c-191b-4a32-882a-6fded2546ce6"),
                Collections.singletonList("317a121e-d08c-4890-b3b3-4652f741771f"),
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
                SUBSCRIPTION_UPDATED_NOTIFICATION,
                "oral",
                Arrays.asList("b8b2904f-629d-42cf-acea-1b74bde5b2ff", "03b957bf-e21d-4147-90c1-b6fefa8cf70d"),
                Arrays.asList("7397a76f-14cb-468c-b1a7-0570940ead91", "759c712a-6b55-485e-bcf7-1cf5c4896eb1"),
                "yes",
                "yes",
                "2",
                "2"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("08365e91-9e07-4a5c-bf96-ef56fd0ada63"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                "yes",
                "yes",
                "1",
                "1"
            },
            new Object[]{
                DWP_RESPONSE_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("a64bce9a-9162-47ca-b3e7-cf5f85ca7bdc"),
                Collections.singletonList("f5b61f94-0b2b-4e8e-9c25-56e9830df7d4"),
                "yes",
                "yes",
                "1",
                "1"
            },
            new Object[]{
                HEARING_BOOKED_NOTIFICATION,
                "oral",
                Collections.singletonList("fee16753-0bdb-43f1-9abb-b14b826e3b26"),
                Collections.singletonList("f900174a-a556-43b2-8042-bbf3e6090071"),
                "yes",
                "yes",
                "1",
                "1"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "oral",
                Collections.singletonList("08365e91-9e07-4a5c-bf96-ef56fd0ada63"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                "yes",
                "yes",
                "1",
                "1"
            },
            new Object[]{
                APPEAL_WITHDRAWN_NOTIFICATION,
                "oral",
                Collections.singletonList("8620e023-f663-477e-a771-9cfad50ee30f"),
                Collections.singletonList("446c7b23-7342-42e1-adff-b4c367e951cb"),
                "yes",
                "yes",
                "1",
                "1"
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
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("08365e91-9e07-4a5c-bf96-ef56fd0ada63"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Harry Potter"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "oral",
                Collections.singletonList("08365e91-9e07-4a5c-bf96-ef56fd0ada63"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Harry Potter"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
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
                CASE_UPDATED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("91143b85-dd9d-430c-ba23-e42ec90f44f8"),
                "no",
                "no",
                "0",
                "0",
                "1",
                "Harry Potter"
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
    public void shouldSendNotificationForEvidenceReminderForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "evidenceReminder");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForEvidenceReminderForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "evidenceReminder");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForDwpResponseLateReminderForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "dwpResponseLateReminder");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForDwpResponseLateReminderForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "dwpResponseLateReminder");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForHearingReminderForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "hearingReminder");
        json = json.replace("2018-01-12", LocalDate.now().plusDays(2).toString());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient).sendEmail(any(), any(), any(), any());
        verify(notificationClient).sendSms(any(), any(), any(), any(), any());
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
        verify(notificationClient).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendSubscriptionCreatedNotificationForSubscriptionUpdatedRequestWithNewSubscribeSmsRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "subscriptionUpdated");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeEmail");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any());
        verify(notificationClient).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendSubscriptionUpdatedNotificationForSubscriptionUpdatedRequestWithNewEmailAddressForAnOralHearing() throws Exception {
        json = updateEmbeddedJson(json, "subscriptionUpdated", "event_id");
        json = updateEmbeddedJson(json, "oral", "case_details", "case_data", "appeal", "hearingType");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions",
                "appellantSubscription", "subscribeSms");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, times(1)).sendEmail(any(), any(), any(), any());
        verify(notificationClient, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendSubscriptionUpdatedNotificationForSubscriptionUpdatedRequestWithNewEmailAddressForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = updateEmbeddedJson(json, "subscriptionUpdated", "event_id");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions",
                "appellantSubscription", "subscribeSms");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, times(1)).sendEmail(any(), any(), any(), any());
        verify(notificationClient, never()).sendSms(any(), any(), any(), any(), any());
    }

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
    public void shouldReturn400WhenAuthHeaderIsMissing() throws Exception {
        HttpServletResponse response = getResponse(getRequestWithoutAuthHeader(json));

        assertHttpStatus(response, HttpStatus.BAD_REQUEST);
        verify(authorisationService, never()).authorise(anyString());
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any(), any());
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

    private void updateJsonForPaperHearing() throws IOException {
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "appeal", "hearingOptions", "wantsToAttend");
        json = updateEmbeddedJson(json, "paper", "case_details", "case_data", "appeal", "hearingType");
    }

}
