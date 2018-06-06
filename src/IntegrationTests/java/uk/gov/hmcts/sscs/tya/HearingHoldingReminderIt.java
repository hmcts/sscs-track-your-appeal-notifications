package uk.gov.hmcts.sscs.tya;

import static helper.IntegrationTestHelper.assertHttpStatus;
import static helper.IntegrationTestHelper.getRequestWithAuthHeader;

import helper.IntegrationTestHelper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
public class HearingHoldingReminderIt {

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

    @Value("${reminder.hearingHoldingReminder.delay.seconds}")
    long hearingHoldingReminderDelay;

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
        ccdResponseJson = ccdResponseJson.replace("\"2017-05-24T14:01:18.243\"", "\"2048-05-24T14:01:18.243\"");

        try {
            quartzScheduler.clear();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldScheduleHearingHoldingReminderThenRemoveWhenBooked() throws Exception {

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Job scheduler is empty at start", 0);

        ccdResponseJson = ccdResponseJson.replace("appealReceived", "responseReceived");
        HttpServletResponse hearingBookedResponse = getResponse(getRequestWithAuthHeader(ccdResponseJson));
        assertHttpStatus(hearingBookedResponse, HttpStatus.OK);

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Hearing holding reminder scheduled", "hearingHoldingReminder", 1);

        IntegrationTestHelper.assertScheduledJobTriggerAt(
            quartzScheduler,
            "Hearing holding reminder scheduled",
            "hearingHoldingReminder",
            ZonedDateTime.parse("2048-05-24T14:01:18.243Z").plusSeconds(hearingHoldingReminderDelay).toString()
        );

        ccdResponseJson = ccdResponseJson.replace("responseReceived", "hearingBooked");
        HttpServletResponse hearingPostponedResponse = getResponse(getRequestWithAuthHeader(ccdResponseJson));
        assertHttpStatus(hearingPostponedResponse, HttpStatus.OK);

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Hearing reminders were removed", "hearingHoldingReminder", 0);
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

}
