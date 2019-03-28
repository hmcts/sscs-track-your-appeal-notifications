package uk.gov.hmcts.reform.sscs.tya;

import static helper.IntegrationTestHelper.assertHttpStatus;
import static helper.IntegrationTestHelper.getRequestWithAuthHeader;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import helper.IntegrationTestHelper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.controller.NotificationController;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.NotificationService;
import uk.gov.hmcts.reform.sscs.service.OutOfHoursCalculator;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

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

    @MockBean(name = "notificationClient")
    NotificationClient client;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Mock
    private SendSmsResponse sendSmsResponse;

    @MockBean
    private JobExecutor<String> jobExecutor;

    @MockBean
    private OutOfHoursCalculator outOfHoursCalculator;

    @Autowired
    @Qualifier("scheduler")
    private Scheduler quartzScheduler;

    @Autowired
    private CcdService ccdService;

    @Autowired
    private SscsCaseCallbackDeserializer deserializer;

    @MockBean
    private IdamService idamService;

    @Before
    public void setup() throws NotificationClientException {
        controller = new NotificationController(notificationService, authorisationService, ccdService, deserializer, idamService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();


        when(client.sendEmail(any(), any(), any(), any()))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        when(client.sendSms(any(), any(), any(), any(), any()))
                .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        outOfHoursCalculator = mock(OutOfHoursCalculator.class);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);
    }

    @Test
    public void shouldScheduleHearingHoldingReminderThenRemoveWhenBooked() throws Exception {

        List<String> eventsThatRemoveReminders =
            Arrays.asList(
                "appealDormant",
                "appealLapsed",
                "appealWithdrawn",
                "hearingBooked"
            );

        for (String eventThatRemoveReminders : eventsThatRemoveReminders) {

            try {
                quartzScheduler.clear();
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }

            IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Job scheduler is empty at start", 0);

            sendEvent("responseReceived");

            IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "First hearing holding reminder scheduled", "hearingHoldingReminder", 1);
            IntegrationTestHelper.assertScheduledJobTriggerAt(
                quartzScheduler,
                "First hearing holding reminder scheduled",
                "hearingHoldingReminder",
                "2048-07-19T14:01:18.243Z"
            );

            IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Second hearing holding reminder scheduled", "secondHearingHoldingReminder", 1);
            IntegrationTestHelper.assertScheduledJobTriggerAt(
                quartzScheduler,
                "Hearing holding reminder scheduled",
                "secondHearingHoldingReminder",
                "2048-08-30T14:01:18.243Z"
            );

            IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Third hearing holding reminder scheduled", "thirdHearingHoldingReminder", 1);
            IntegrationTestHelper.assertScheduledJobTriggerAt(
                quartzScheduler,
                "Hearing holding reminder scheduled",
                "thirdHearingHoldingReminder",
                "2048-10-11T14:01:18.243Z"
            );

            IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Final hearing holding reminder scheduled", "finalHearingHoldingReminder", 1);
            IntegrationTestHelper.assertScheduledJobTriggerAt(
                quartzScheduler,
                "Final hearing holding reminder scheduled",
                "finalHearingHoldingReminder",
                "2048-11-22T14:01:18.243Z"
            );

            sendEvent(eventThatRemoveReminders);

            IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "First hearing reminders were removed", "hearingHoldingReminder", 0);
            IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Second hearing reminders were removed", "secondHearingHoldingReminder", 0);
            IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Third hearing reminders were removed", "thirdHearingHoldingReminder", 0);
            IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Final hearing reminder was removed", "finalHearingHoldingReminder", 0);
        }
    }

    private void sendEvent(String event) throws Exception {

        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        String ccdResponseJson = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        ccdResponseJson = ccdResponseJson.replace("appealReceived", event);
        ccdResponseJson = ccdResponseJson.replace("2017-05-24T14:01:18.243", "2048-05-24T14:01:18.243");
        ccdResponseJson = ccdResponseJson.replace("2018-01-12", LocalDate.now().plusDays(2).toString());

        HttpServletResponse sendResponse = getResponse(getRequestWithAuthHeader(ccdResponseJson));
        assertHttpStatus(sendResponse, HttpStatus.OK);
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

}
