package uk.gov.hmcts.reform.sscs.tya;

import static helper.IntegrationTestHelper.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.NotificationBlacklist;
import uk.gov.hmcts.reform.sscs.controller.NotificationController;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.*;
import uk.gov.service.notify.NotificationClient;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class NotificationsIt {

    MockMvc mockMvc;

    NotificationController controller;

    @Mock
    NotificationClient client;

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

    @Before
    public void setup() throws IOException {
        NotificationSender sender = new NotificationSender(client, null, notificationBlacklist);
        NotificationService service = new NotificationService(sender, factory, reminderService, notificationValidService, notificationHandler);
        controller = new NotificationController(service, authorisationService, ccdService, deserializer, idamService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        outOfHoursCalculator = mock(OutOfHoursCalculator.class);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);
    }

    @Test
    public void shouldSendNotificationForAnAppealReceivedRequestForAnOralHearing() throws Exception {
        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAnAppealReceivedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAnAdjournedRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "hearingAdjourned");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForAnAdjournedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "hearingAdjourned");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAnResponseReceivedRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "responseReceived");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForAnResponseReceivedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "responseReceived");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAnEvidenceReceivedRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "evidenceReceived");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForAnEvidenceReceivedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "evidenceReceived");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAHearingPostponedRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "hearingPostponed");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForAHearingPostponedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "hearingPostponed");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAppealLapsedRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "appealLapsed");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForAppealLapsedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "appealLapsed");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAppealWithdrawnRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "appealWithdrawn");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForAppealWithdrawnRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "appealWithdrawn");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForHearingBookedRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "hearingBooked");
        json = json.replace("2018-01-12", LocalDate.now().plusDays(2).toString());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForHearingBookedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "hearingBooked");
        json = json.replace("2018-01-12", LocalDate.now().plusDays(2).toString());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForHearingBookedRequestForHearingInThePastForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "hearingBooked");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForEvidenceReminderForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "evidenceReminder");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForEvidenceReminderForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "evidenceReminder");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForDwpResponseLateReminderForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "dwpResponseLateReminder");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForDwpResponseLateReminderForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "dwpResponseLateReminder");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForHearingReminderForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "hearingReminder");
        json = json.replace("2018-01-12", LocalDate.now().plusDays(2).toString());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForHearingReminderForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "hearingReminder");
        json = json.replace("2018-01-12", LocalDate.now().plusDays(2).toString());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForSyaAppealCreatedRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "appealCreated");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, times(1)).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForSyaAppealCreatedRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "appealCreated");
        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, times(1)).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendSubscriptionCreatedNotificationForSubscriptionUpdatedRequestWithNewSubscribeSmsRequestForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "subscriptionUpdated");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeEmail");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendSubscriptionCreatedNotificationForSubscriptionUpdatedRequestWithNewSubscribeSmsRequestForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "subscriptionUpdated");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeEmail");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendSubscriptionUpdatedNotificationForSubscriptionUpdatedRequestWithNewEmailAddressForAnOralHearing() throws Exception {
        json = json.replace("appealReceived", "subscriptionUpdated");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeSms");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldSendSubscriptionUpdatedNotificationForSubscriptionUpdatedRequestWithNewEmailAddressForAPaperHearing() throws Exception {
        updateJsonForPaperHearing();
        json = json.replace("appealReceived", "subscriptionUpdated");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeSms");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendSubscriptionUpdatedNotificationForSubscriptionUpdatedRequestWithSameEmailAddress() throws Exception {
        json = json.replace("appealReceived", "subscriptionUpdated");
        json = json.replace("sscstest@greencroftconsulting.com", "tester@hmcts.net");

        json = updateEmbeddedJson(json, "Yes", "case_details_before", "case_data", "subscriptions", "appellantSubscription", "subscribeEmail");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeSms");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldReturn400WhenAuthHeaderIsMissing() throws Exception {
        HttpServletResponse response = getResponse(getRequestWithoutAuthHeader(json));

        assertHttpStatus(response, HttpStatus.BAD_REQUEST);
        verify(authorisationService, never()).authorise(anyString());
        verify(client, never()).sendEmail(any(), any(), any(), any(), any());
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

    private void updateJsonForPaperHearing() throws IOException {
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "appeal", "hearingOptions", "wantsToAttend");
    }

}
