package uk.gov.hmcts.sscs.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


public class NotificationControllerTest {

    private NotificationController notificationController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        notificationController = new NotificationController();
        mockMvc = standaloneSetup(notificationController).build();
    }

    @Test
    public void shouldReturnHttpStatusCode200ForTheCcdResponse() throws Exception {
        String json = "{\"state\":\"ResponseRequested\",\"case_data\":{\"id\":{\"tya\":\"755TY68876\"},\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Maloney\",\"firstName\":\"J\"},\"contact\":{\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"phone\":\"07998445858\"}}}}";

        mockMvc.perform(post("/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }
}