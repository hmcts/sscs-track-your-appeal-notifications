package uk.gov.hmcts.reform.sscs.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;


@ActiveProfiles("integration")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CcdService ccdService;

    @MockBean
    private IdamService idamService;

    @Test
    public void checkUnknownEvent() throws Exception {
        String content = "{\n"
                + "  \"case_id\": \"1546942528346226\",\n"
                + "  \"online_hearing_id\":\"13f8480c-fca4-4549-b4e4-17bef753d3ef\",\n"
                + "  \"event_type\":\"answers_submitted\",\n"
                + "  \"expiry_date\":\"2018-08-12T23:59:59Z\",\n"
                + "  \"reason\":\"foo\"\n"
                + "}";
        mockMvc.perform(post("/coh-send")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ServiceAuthorization", "")
                .content(content)).andExpect(status().isBadRequest());
    }

    @Test
    public void checkKnownEvent() throws Exception {
        when(idamService.getIdamTokens()).thenReturn(null);
        when(ccdService.getByCaseId(Long.valueOf("1546942528346226"), null)).thenReturn(null);

        String content = "{\n"
                + "  \"case_id\": \"1546942528346226\",\n"
                + "  \"online_hearing_id\":\"13f8480c-fca4-4549-b4e4-17bef753d3ef\",\n"
                + "  \"event_type\":\"question_round_issued\",\n"
                + "  \"expiry_date\":\"2018-08-12T23:59:59Z\",\n"
                + "  \"reason\":\"foo\"\n"
                + "}";
        mockMvc.perform(post("/coh-send")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ServiceAuthorization", "")
                .content(content)).andExpect(status().isOk());
    }
}
