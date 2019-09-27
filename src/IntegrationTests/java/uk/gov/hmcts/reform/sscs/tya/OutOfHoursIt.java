package uk.gov.hmcts.reform.sscs.tya;

import static helper.IntegrationTestHelper.*;
import static org.mockito.Mockito.*;

import helper.IntegrationTestHelper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
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
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.config.NotificationBlacklist;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.controller.NotificationController;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.service.*;
import uk.gov.hmcts.reform.sscs.service.docmosis.PdfLetterService;
import uk.gov.hmcts.reform.sscs.service.reminder.JobGroupGenerator;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class OutOfHoursIt {

    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    MockMvc mockMvc;

    NotificationController controller;

    @Mock
    NotificationClient notificationClient;

    @Mock
    private SendEmailResponse sendEmailResponse;

    @Mock
    private SendSmsResponse sendSmsResponse;


    @Mock
    ReminderService reminderService;

    @Autowired
    NotificationValidService notificationValidService;

    @MockBean
    private AuthorisationService authorisationService;

    @Mock
    NotificationBlacklist notificationBlacklist;

    @Autowired
    NotificationFactory factory;

    @Autowired
    private CcdService ccdService;

    @Autowired
    private SscsCaseCallbackDeserializer deserializer;

    @MockBean
    private IdamService idamService;

    String json;

    @Autowired
    private NotificationHandler notificationHandler;

    @MockBean
    private OutOfHoursCalculator outOfHoursCalculator;

    @Autowired
    private NotificationConfig notificationConfig;

    @Autowired
    private EvidenceManagementService evidenceManagementService;

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private JobGroupGenerator jobGroupGenerator;

    @Autowired
    private BundledLetterTemplateUtil bundledLetterTemplateUtil;

    @Autowired
    private PdfLetterService pdfLetterService;

    @Mock
    private SscsGeneratePdfService sscsGeneratePdfService;

    @Mock
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    @Autowired
    @Qualifier("scheduler")
    private Scheduler quartzScheduler;

    @Mock
    private MarkdownTransformationService markdownTransformationService;

    @Before
    public void setup() throws Exception {
        NotificationSender sender = new NotificationSender(notificationClient, null, notificationBlacklist, ccdNotificationsPdfService, markdownTransformationService, false);

        SendNotificationService sendNotificationService = new SendNotificationService(sender, evidenceManagementService, sscsGeneratePdfService, notificationHandler, notificationValidService, bundledLetterTemplateUtil, pdfLetterService);
        ReflectionTestUtils.setField(sendNotificationService, "bundledLettersOn", true);

        outOfHoursCalculator = mock(OutOfHoursCalculator.class);
        LocalDateTime dateBefore = LocalDateTime.now();
        ZonedDateTime zoned = ZonedDateTime.ofLocal(dateBefore, ZoneId.of(AppConstants.ZONE_ID), null);
        when(outOfHoursCalculator.getStartOfNextInHoursPeriod()).thenReturn(zoned);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(true);

        notificationHandler = new NotificationHandler(outOfHoursCalculator, jobScheduler, jobGroupGenerator);

        NotificationService service = new NotificationService(factory, reminderService, notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService);
        controller = new NotificationController(service, authorisationService, ccdService, deserializer, idamService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        when(notificationClient.sendEmail(any(), any(), any(), any()))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        when(notificationClient.sendSms(any(), any(), any(), any(), any()))
                .thenReturn(sendSmsResponse);
        when(sendSmsResponse.getNotificationId()).thenReturn(UUID.randomUUID());

    }

    @Test
    public void scheduleOutOfHoursNotificationWithAnAppellantSubscription() throws Exception {
        try {
            quartzScheduler.clear();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Job scheduler is empty at start", 0);

        json = updateEmbeddedJson(json, null, "case_details", "case_data", "subscriptions",
                "representativeSubscription");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Appeal received scheduled", "appealReceived", 1);
    }

    @Test
    public void scheduleOutOfHoursNotificationWithAnAppellantAndRepresentativeSubscription() throws Exception {
        try {
            quartzScheduler.clear();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Job scheduler is empty at start", 0);

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        IntegrationTestHelper.assertScheduledJobCount(quartzScheduler, "Appeal received scheduled", "appealReceived", 1);
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

}
