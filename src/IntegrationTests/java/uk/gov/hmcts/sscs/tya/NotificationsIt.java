package uk.gov.hmcts.sscs.tya;

import static helper.IntegrationTestHelper.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.sscs.controller.NotificationController;
import uk.gov.hmcts.sscs.factory.NotificationFactory;
import uk.gov.hmcts.sscs.service.AuthorisationService;
import uk.gov.hmcts.sscs.service.NotificationService;
import uk.gov.hmcts.sscs.service.ReminderService;
import uk.gov.service.notify.NotificationClient;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class NotificationsIt {

    MockMvc mockMvc;

    NotificationController controller;

    @Mock
    NotificationClient client;

    @Mock
    ReminderService reminderService;

    @MockBean
    private AuthorisationService authorisationService;

    @Autowired
    NotificationFactory factory;

    String json;

    @Before
    public void setup() throws IOException {
        NotificationService service = new NotificationService(client, factory, reminderService);
        controller = new NotificationController(service, authorisationService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
    }

    @Test
    public void shouldSendNotificationForAnAppealReceivedRequest() throws Exception {
        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any());
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
        verify(client, never()).sendSms(any(), any(), any(), any());
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

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any());
    }

    @Test
    public void shouldSendNotificationForSyaAppealCreated() throws Exception {
        json = json.replace("appealReceived", "appealCreated");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any());
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
        json = json.replace("updatedemail@hmcts.net", "tester@hmcts.net");

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
