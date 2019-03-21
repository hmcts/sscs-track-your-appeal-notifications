package uk.gov.hmcts.reform.sscs.tya;

import static helper.IntegrationTestHelper.assertHttpStatus;
import static helper.IntegrationTestHelper.getRequestWithAuthHeader;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import helper.IntegrationTestHelper;
import java.io.File;
import java.nio.charset.StandardCharsets;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.controller.NotificationController;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
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
public class DwpResponseLateReminderIt {

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

    @Autowired
    @Qualifier("scheduler")
    private Scheduler quartzScheduler;

    @Autowired
    private CcdService ccdService;

    @Autowired
    private SscsCaseDataWrapperDeserializer deserializer;

    @Mock
    private OutOfHoursCalculator outOfHoursCalculator;

    @MockBean
    private IdamService idamService;

    @Before
    public void setup() throws NotificationClientException {
        ReflectionTestUtils.setField(notificationService, "outOfHoursCalculator", outOfHoursCalculator);
        controller = new NotificationController(notificationService, authorisationService, ccdService, deserializer, idamService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        outOfHoursCalculator = mock(OutOfHoursCalculator.class);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);

        when(client.sendEmail(any(), any(), any(), any()))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        when(client.sendSms(any(), any(), any(), any(), any()))
                .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());
    }

    @Test
    public void shouldScheduleDwpResponseLateReminderThenRemoveWhenReceived() throws Exception {

        List<String> eventsThatRemoveReminders =
            Arrays.asList(
                "appealDormant",
                "appealLapsed",
                "appealWithdrawn",
                "responseReceived"
            );

        for (String eventThatRemoveReminders : eventsThatRemoveReminders) {

            try {
                quartzScheduler.clear();
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }

            IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Job scheduler is empty at start", 0);

            sendEvent("appealReceived");

            IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "DWP response late reminder scheduled", "dwpResponseLateReminder", 1);

            IntegrationTestHelper.assertScheduledJobTriggerAt(
                quartzScheduler,
                "DWP response late reminder scheduled",
                "dwpResponseLateReminder",
                "2048-06-26T14:01:18.243Z"
            );

            sendEvent(eventThatRemoveReminders);

            IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "DWP response late reminders were removed", "dwpResponseLateReminder", 0);
        }
    }

    private void sendEvent(String event) throws Exception {

        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        String ccdResponseJson = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        ccdResponseJson = ccdResponseJson.replace("appealReceived", event);
        ccdResponseJson = ccdResponseJson.replace("2017-05-22T14:01:18.243", "2048-05-22T14:01:18.243");

        HttpServletResponse sendResponse = getResponse(getRequestWithAuthHeader(ccdResponseJson));
        assertHttpStatus(sendResponse, HttpStatus.OK);
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

}
