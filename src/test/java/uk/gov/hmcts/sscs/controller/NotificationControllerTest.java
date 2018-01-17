package uk.gov.hmcts.sscs.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.service.NotificationService;


public class NotificationControllerTest {

    private NotificationController notificationController;

    private MockMvc mockMvc;

    private CcdResponse ccdResponse;

    @Mock
    NotificationService service;


    @Before
    public void setUp() {
        initMocks(this);

        notificationController = new NotificationController(service);
        mockMvc = standaloneSetup(notificationController).build();
        ccdResponse = new CcdResponse();
    }

    @Test
    public void shouldReturnHttpStatusCode200ForTheCcdResponse() throws Exception {
        String json = "{\"state\":\"ResponseRequested\",\"case_data\":{\"id\":{\"tya\":\"755TY68876\"},\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Maloney\",\"firstName\":\"J\"},\"contact\":{\"email\":\"test@testing.com\",\"mobile\":\"01234556634\"}}}}";

        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldCreateAndSendNotificationForCcdResponse() throws Exception {
        notificationController.sendNotification(ccdResponse);
        verify(service).createAndSendNotification(ccdResponse);
    }
}