package uk.gov.hmcts.sscs.tya;

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
import uk.gov.hmcts.sscs.config.NotificationBlacklist;
import uk.gov.hmcts.sscs.controller.NotificationController;
import uk.gov.hmcts.sscs.deserialize.CcdResponseWrapperDeserializer;
import uk.gov.hmcts.sscs.factory.NotificationFactory;
import uk.gov.hmcts.sscs.service.*;
import uk.gov.hmcts.sscs.service.ccd.SearchCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamTokensService;
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
    private SearchCcdService searchCcdService;

    @Autowired
    private IdamTokensService idamTokensService;

    @Autowired
    private CcdResponseWrapperDeserializer deserializer;

    String json;

    @Before
    public void setup() throws IOException {
        NotificationSender sender = new NotificationSender(client, null, notificationBlacklist);
        NotificationService service = new NotificationService(sender, factory, reminderService, notificationValidService);
        controller = new NotificationController(service, authorisationService, searchCcdService, idamTokensService, deserializer);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
    }

    @Test
    public void shouldSendNotificationForAnAppealReceivedRequest() throws Exception {
        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, times(1)).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAnAdjournedRequest() throws Exception {
        json = json.replace("appealReceived", "hearingAdjourned");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, times(1)).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAnResponseReceivedRequest() throws Exception {
        json = json.replace("appealReceived", "responseReceived");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, times(1)).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAnEvidenceReceivedRequest() throws Exception {
        json = json.replace("appealReceived", "evidenceReceived");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, times(1)).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAHearingPostponedRequest() throws Exception {
        json = json.replace("appealReceived", "hearingPostponed");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAppealLapsedRequest() throws Exception {
        json = json.replace("appealReceived", "appealLapsed");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForAppealWithdrawnRequest() throws Exception {
        json = json.replace("appealReceived", "appealWithdrawn");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForHearingBookedRequest() throws Exception {
        json = json.replace("appealReceived", "hearingBooked");
        json = json.replace("2018-01-12", LocalDate.now().plusDays(2).toString());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, times(1)).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldNotSendNotificationForHearingBookedRequestForHearingInThePast() throws Exception {
        json = json.replace("appealReceived", "hearingBooked");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(0)).sendEmail(any(), any(), any(), any());
        verify(client, times(0)).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForEvidenceReminder() throws Exception {
        json = json.replace("appealReceived", "evidenceReminder");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, times(1)).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForHearingReminder() throws Exception {
        json = json.replace("appealReceived", "hearingReminder");
        json = json.replace("2018-01-12", LocalDate.now().plusDays(2).toString());

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, times(1)).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForSyaAppealCreated() throws Exception {
        json = json.replace("appealReceived", "appealCreated");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, times(1)).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendSubscriptionCreatedNotificationForSubscriptionUpdatedRequestWithNewSubscribeSmsRequest() throws Exception {
        json = json.replace("appealReceived", "subscriptionUpdated");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeEmail");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, never()).sendEmail(any(), any(), any(), any());
        verify(client, times(1)).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendSubscriptionUpdatedNotificationForSubscriptionUpdatedRequestWithNewEmailAddress() throws Exception {
        json = json.replace("appealReceived", "subscriptionUpdated");
        json = updateEmbeddedJson(json, "No", "case_details", "case_data", "subscriptions", "appellantSubscription", "subscribeSms");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any());
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
        verify(client, never()).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldReturn400WhenAuthHeaderIsMissing() throws Exception {
        HttpServletResponse response = getResponse(getRequestWithoutAuthHeader(json));

        assertHttpStatus(response, HttpStatus.BAD_REQUEST);
        verify(authorisationService, never()).authorise(anyString());
        verify(client, never()).sendEmail(any(), any(), any(), any());
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

}
