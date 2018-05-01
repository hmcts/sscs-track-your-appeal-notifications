package uk.gov.hmcts.sscs.tya;

import static helper.IntegrationTestHelper.assertHttpStatus;
import static helper.IntegrationTestHelper.getRequestWithAuthHeader;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.sscs.client.RestClient;
import uk.gov.hmcts.sscs.controller.NotificationController;
import uk.gov.hmcts.sscs.factory.NotificationFactory;
import uk.gov.hmcts.sscs.service.AuthorisationService;
import uk.gov.hmcts.sscs.service.NotificationService;
import uk.gov.hmcts.sscs.service.ReminderService;
import uk.gov.service.notify.NotificationClient;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class RemindersIt {

    MockMvc mockMvc;

    NotificationController controller;

    ReminderService reminderService;

    RestClient restClient;

    @Mock
    NotificationClient client;

    @Mock
    Client jerseyClient;

    @Mock
    private WebResource webResource;

    @Mock
    private WebResource.Builder builder;

    @Mock
    private ClientResponse response;

    @MockBean
    private AuthorisationService authorisationService;

    @Autowired
    NotificationFactory factory;

    String json;

    @Before
    public void setup() throws IOException {
        initMocks(this);
        restClient = new RestClient(jerseyClient);
        reminderService = new ReminderService(restClient);
        NotificationService service = new NotificationService(client, factory, reminderService);
        controller = new NotificationController(service, authorisationService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        ReflectionTestUtils.setField(service, "isJobSchedulerEnabled", true);
    }

    @Test
    public void shouldSendReminderForAResponseReceivedRequest() throws Exception {
        json = json.replace("appealReceived", "responseReceived");

        ReflectionTestUtils.setField(reminderService, "callbackUrl", "www.callback.com");
        ReflectionTestUtils.setField(restClient, "url", "www.test.com");

        doReturn(webResource).when(jerseyClient).resource("www.test.com/jobs");
        doReturn(builder).when(webResource).type("application/json");
        doReturn(builder).when(builder).header("ServiceAuthorization", "sscs");
        doReturn(response).when(builder).post(eq(ClientResponse.class), any());
        doReturn(201).when(response).getStatus();

        HttpServletResponse result = getResponse(getRequestWithAuthHeader(json));
        assertHttpStatus(result, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any());
        verify(builder, times(1)).post(eq(ClientResponse.class), any());
    }

    @Test
    public void shouldNotSendReminderForNonReminderEvent() throws Exception {
        json = json.replace("appealReceived", "appealWithdrawn");

        HttpServletResponse result = getResponse(getRequestWithAuthHeader(json));
        assertHttpStatus(result, HttpStatus.OK);
        verify(client, times(1)).sendEmail(any(), any(), any(), any());
        verify(client, never()).sendSms(any(), any(), any(), any());
        verify(builder, times(0)).post(eq(ClientResponse.class), any());
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }
}
