package uk.gov.hmcts.sscs.tya;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.sscs.controller.NotificationController;
import uk.gov.hmcts.sscs.factory.NotificationFactory;
import uk.gov.hmcts.sscs.service.NotificationService;
import uk.gov.service.notify.NotificationClient;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class NotificationsIt {

    MockMvc mockMvc;

    NotificationController controller;

    @Mock
    NotificationClient client;

    @Autowired
    NotificationFactory factory;

    String path = "src/IntegrationTests/resources/json/ccdCallbackResponse.json";

    @Before
    public void setup() {
        initMocks(this);
        NotificationService service = new NotificationService(client, factory);
        controller = new NotificationController(service);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void shouldSendNotificationForAnAppealReceivedRequest() throws Exception {

        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldSendNotificationForAnAdjournedRequest() throws Exception {

        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = json.replace("appealReceived", "hearingAdjourned");

        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldSendNotificationForAnResponseReceivedRequest() throws Exception {

        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = json.replace("appealReceived", "responseReceived");

        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldSendNotificationForAnEvidenceReceivedRequest() throws Exception {

        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = json.replace("appealReceived", "evidenceReceived");

        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldSendNotificationForAHearingPostponedRequest() throws Exception {

        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        json = json.replace("appealReceived", "hearingPostponed");

        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

}
