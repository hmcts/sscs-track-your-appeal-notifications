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
        String json = "{\"case_details\":{\"case_data\":{\"subscriptions\":{"
                + "\"appellantSubscription\":{\"tya\":\"543212345\",\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"reason\":null,\"subscribeSms\":\"No\",\"subscribeEmail\":\"Yes\"},"
                + "\"supporterSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@live.co.uk\",\"mobile\":\"07925289702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}},"
                + "\"caseReference\":\"SC/1234/23\",\"appeal\":{"
                + "\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Vasquez\",\"firstName\":\"Dexter\",\"middleName\":\"Ali Sosa\"}},"
                + "\"supporter\":{\"name\":{\"title\":\"Mrs\",\"lastName\":\"Wilder\",\"firstName\":\"Amber\",\"middleName\":\"Clark Eaton\"}}}}},\"event_id\": \"appealReceived\"\n}";

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