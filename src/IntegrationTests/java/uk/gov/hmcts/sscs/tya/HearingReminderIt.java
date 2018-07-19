package uk.gov.hmcts.sscs.tya;

import static helper.IntegrationTestHelper.assertHttpStatus;
import static helper.IntegrationTestHelper.getRequestWithAuthHeader;

import helper.IntegrationTestHelper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
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
import uk.gov.hmcts.sscs.service.NotificationSender;
import uk.gov.hmcts.sscs.service.NotificationService;
import uk.gov.service.notify.NotificationClient;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class HearingReminderIt {

    MockMvc mockMvc;

    NotificationController controller;

    @Autowired
    NotificationService notificationService;

    @MockBean
    private AuthorisationService authorisationService;

    @MockBean
    @Qualifier("notificationClient")
    private NotificationClient notificationClient;

    @MockBean
    @Qualifier("testNotificationClient")
    private NotificationClient testNotificationClient;

    @MockBean
    NotificationSender notificationSender;

    @MockBean
    private JobExecutor<String> jobExecutor;

    @Autowired
    @Qualifier("scheduler")
    private Scheduler quartzScheduler;

    @Before
    public void setup() {
        controller = new NotificationController(notificationService, authorisationService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void shouldScheduleHearingReminderThenRemoveWhenPostponed() throws Exception {

        try {
            quartzScheduler.clear();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Job scheduler is empty at start", 0);

        sendEvent("hearingBooked");

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Hearing reminders scheduled", "hearingReminder", 2);

        IntegrationTestHelper.assertScheduledJobTriggerAt(
            quartzScheduler,
            "First hearing reminder scheduled",
            "hearingReminder",
            "2048-01-05T11:00:00Z"
        );

        IntegrationTestHelper.assertScheduledJobTriggerAt(
            quartzScheduler,
            "Second hearing reminder scheduled",
            "hearingReminder",
            "2048-01-11T11:00:00Z"
        );

        sendEvent("hearingPostponed");

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Hearing reminders were removed", "hearingReminder", 0);
    }

    private void sendEvent(String event) throws Exception {

        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        String ccdResponseJson = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        ccdResponseJson = ccdResponseJson.replace("appealReceived", event);
        ccdResponseJson = ccdResponseJson.replace("\"hearingDate\": \"2018-01-12\"", "\"hearingDate\": \"2048-01-12\"");

        HttpServletResponse sendResponse = getResponse(getRequestWithAuthHeader(ccdResponseJson));
        assertHttpStatus(sendResponse, HttpStatus.OK);
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

}
