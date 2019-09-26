package uk.gov.hmcts.reform.sscs.tya;

import static helper.IntegrationTestHelper.*;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Collections;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.NotificationBlacklist;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.controller.NotificationController;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.*;
import uk.gov.hmcts.reform.sscs.service.coh.CohClient;
import uk.gov.hmcts.reform.sscs.service.coh.QuestionReferences;
import uk.gov.hmcts.reform.sscs.service.coh.QuestionRound;
import uk.gov.hmcts.reform.sscs.service.coh.QuestionRounds;
import uk.gov.hmcts.reform.sscs.service.docmosis.PdfLetterService;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
// NB These could fail if it is out of hours check the config for AAT if this test has started to fail.
public class CohNotificationsIt {
    private static final String TEMPLATE_PATH = "/templates/non_compliant_case_letter_template.html";

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

    @MockBean
    private AuthorisationService authorisationService;

    @MockBean
    private IdamService idamService;

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private CohClient cohClient;

    @Mock
    NotificationBlacklist notificationBlacklist;

    @MockBean
    private OutOfHoursCalculator outOfHoursCalculator;

    @Autowired
    NotificationFactory factory;

    @Autowired
    private SscsCaseCallbackDeserializer deserializer;

    @Autowired
    private NotificationValidService notificationValidService;

    @Autowired
    private NotificationHandler notificationHandler;

    @Autowired
    private EvidenceManagementService evidenceManagementService;

    @Value("${notification.question_round_issued.emailId}")
    private String emailTemplateId;

    @Autowired
    private CcdService ccdService;

    @Autowired
    private NotificationConfig notificationConfig;

    @Autowired
    private BundledLetterTemplateUtil bundledLetterTemplateUtil;


    @Mock
    private SscsGeneratePdfService sscsGeneratePdfService;

    @Mock
    private PdfLetterService pdfLetterService;

    @Mock
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    String json;

    @Mock
    private MarkdownTransformationService markdownTransformationService;

    @Before
    public void setup() throws Exception {
        Boolean saveCorrespondence = false;
        NotificationSender sender = new NotificationSender(notificationClient, null, notificationBlacklist, ccdNotificationsPdfService, markdownTransformationService, saveCorrespondence);

        SendNotificationService sendNotificationService = new SendNotificationService(sender, evidenceManagementService, sscsGeneratePdfService, notificationHandler, notificationValidService, bundledLetterTemplateUtil, pdfLetterService);
        ReflectionTestUtils.setField(sendNotificationService, "bundledLettersOn", true);
        ReflectionTestUtils.setField(sendNotificationService, "lettersOn", true);

        NotificationService service = new NotificationService(factory, reminderService, notificationValidService, notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService, true);
        controller = new NotificationController(service, authorisationService, ccdService, deserializer, idamService);

        ObjectMapper mapper = new ObjectMapper();
        File src = new File(getClass().getClassLoader().getResource("json/cohCcdCase.json").getFile());
        CaseDetails caseDetails = mapper.readValue(src, CaseDetails.class);

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().userId("user-id").build());
        when(coreCaseDataApi.readForCaseWorker(any(), any(), any(), any(), any(), any()))
                .thenReturn(caseDetails);
        when(cohClient.getQuestionRounds(any(), any(), any())).thenReturn(
                new QuestionRounds(1, singletonList(
                        new QuestionRound(Collections.singletonList(new QuestionReferences("2018-08-11T23:59:59Z")))
                ))
        );

        when(notificationClient.sendEmail(any(), any(), any(), any()))
                .thenReturn(sendEmailResponse);
        when(sendEmailResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        json = "{\n"
                + "   \"case_id\":\"12345\",\n"
                + "   \"online_hearing_id\":\"9a0e278a-e294-47dd-aa18-02b85b40ca93\",\n"
                + "   \"event_type\":\"question_round_issued\",\n"
                + "   \"expiry_date\":\"2018-08-12T23:59:59Z\",\n"
                + "   \"reason\":\"foo\"\n"
                + "}";

        outOfHoursCalculator = mock(OutOfHoursCalculator.class);
        when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);
    }

    @Test
    public void shouldSendCohNotification() throws Exception {
        HttpServletResponse response = getResponse(getCohRequestWithAuthHeader(json));

        assertHttpStatus(response, HttpStatus.OK);
        verify(notificationClient, times(1)).sendEmail(eq(emailTemplateId), eq("joe@bloggs.com"),
                argThat(argument -> "11 August 2018".equals(argument.get("questions_end_date"))),
                any()
        );
    }

    @Test
    public void shouldReturn400WhenAuthHeaderIsMissing() throws Exception {
        HttpServletResponse response = getResponse(getCohRequestWithoutAuthHeader(json));

        assertHttpStatus(response, HttpStatus.BAD_REQUEST);
        verify(authorisationService, never()).authorise(anyString());
        verify(notificationClient, never()).sendEmail(any(), any(), any(), any(), any());
    }

    private MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

}
