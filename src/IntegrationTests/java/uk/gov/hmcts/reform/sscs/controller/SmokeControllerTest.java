package uk.gov.hmcts.reform.sscs.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class SmokeControllerTest {

    @MockBean
    private CcdService ccdService;
    @MockBean
    private IdamService idamService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldReturn200WhenSendingRequestToController() throws Exception {
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        mockMvc.perform(get("/smoke-test"))
            .andDo(print())
            .andExpect(status().isOk());
    }

}
