package uk.gov.hmcts.sscs.tya;

import static helper.IntegrationTestHelper.assertHttpStatus;
import static helper.IntegrationTestHelper.getRequestWithAuthHeader;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.sscs.controller.NotificationController;
import uk.gov.hmcts.sscs.service.AuthorisationService;
import uk.gov.hmcts.sscs.service.NotificationService;
import uk.gov.service.notify.NotificationClient;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class HearingPostponedIt {

    MockMvc mockMvc;

    NotificationController controller;

    @Autowired
    NotificationService notificationService;

    @MockBean
    private AuthorisationService authorisationService;

    @MockBean
    NotificationClient client;

    @MockBean
    private JobExecutor<String> jobExecutor;

    @Autowired
    @Qualifier("scheduler")
    private Scheduler quartzScheduler;

    String ccdResponseJson;

    @Before
    public void setup() throws IOException {
        controller = new NotificationController(notificationService, authorisationService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        ccdResponseJson = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        ccdResponseJson = ccdResponseJson.replace("\"hearingDate\": \"2018-01-12\"", "\"hearingDate\": \"2048-01-12\"");
    }

    @Test
    public void shouldRemoveHearingReminderWhenPostponed() throws Exception {

        assertScheduledJobCount("Job scheduler is empty at start", 0);

        ccdResponseJson = ccdResponseJson.replace("appealReceived", "hearingBooked");
        HttpServletResponse hearingBookedResponse = getResponse(getRequestWithAuthHeader(ccdResponseJson));
        assertHttpStatus(hearingBookedResponse, HttpStatus.OK);

        assertScheduledJobCount("Hearing reminders scheduled", 2);

        ccdResponseJson = ccdResponseJson.replace("hearingBooked", "hearingPostponed");
        HttpServletResponse hearingPostponedResponse = getResponse(getRequestWithAuthHeader(ccdResponseJson));
        assertHttpStatus(hearingPostponedResponse, HttpStatus.OK);

        assertScheduledJobCount("Hearing reminders were removed", 0);
    }

    public void assertScheduledJobCount(
        String message,
        int expectedValue
    ) {

        try {

            int scheduledJobCount =
                quartzScheduler
                    .getJobKeys(GroupMatcher.anyGroup())
                    .size();

            assertTrue(
                message + " (" + expectedValue + " != " + expectedValue + ")",
                scheduledJobCount == expectedValue
            );

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

}
