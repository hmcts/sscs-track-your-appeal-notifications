package uk.gov.hmcts.reform.sscs.service;

import junitparams.JUnitParamsRunner;
import lombok.Getter;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.docmosis.PdfLetterService;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
@Getter
public class NotificationServiceBase {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    static final String DATE = "2018-01-01T14:01:18.243";
    private static final String APPEAL_NUMBER = "GLSCRR";
    static final String YES = "Yes";
    static final String NO = "No";
    static final String CASE_REFERENCE = "ABC123";
    static final String CASE_ID = "1000001";
    static final String EMAIL_TEST_1 = "test1@email.com";
    static final String EMAIL_TEST_2 = "test2@email.com";
    static final String MOBILE_NUMBER_1 = "+447983495065";
    static final String MOBILE_NUMBER_2 = "+447123456789";

    private NotificationService notificationService;

    @Autowired
    private NotificationValidService notificationValidService;

    @Autowired
    private NotificationFactory notificationFactory;

    @Autowired
    private NotificationConfig notificationConfig;

    @Autowired
    private NotificationHandler notificationHandler;

    @Autowired
    private BundledLetterTemplateUtil bundledLetterTemplateUtil;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private ReminderService reminderService;

    @Mock
    private OutOfHoursCalculator outOfHoursCalculator;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private SscsGeneratePdfService sscsGeneratePdfService;

    @Mock
    private PdfLetterService pdfLetterService;

    @Mock
    private IdamService idamService;

    private final Subscription subscription = Subscription.builder()
        .tya(NotificationServiceBase.APPEAL_NUMBER)
        .email(NotificationServiceBase.EMAIL_TEST_1)
        .mobile(NotificationServiceBase.MOBILE_NUMBER_1)
        .subscribeEmail(NotificationServiceBase.YES)
        .subscribeSms(NotificationServiceBase.YES)
        .wantSmsNotifications(NotificationServiceBase.YES)
        .build();


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        notificationService = getNotificationService();

        Mockito.when(outOfHoursCalculator.isItOutOfHours()).thenReturn(false);

        String authHeader = "authHeader";
        String serviceAuthHeader = "serviceAuthHeader";
        IdamTokens idamTokens = IdamTokens.builder().idamOauth2Token(authHeader).serviceAuthorization(serviceAuthHeader).build();

        Mockito.when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    NotificationService getNotificationService() {
        SendNotificationService sendNotificationService = new SendNotificationService(notificationSender,
            evidenceManagementService, sscsGeneratePdfService, notificationHandler, notificationValidService,
            bundledLetterTemplateUtil, pdfLetterService);
        ReflectionTestUtils.setField(sendNotificationService, "bundledLettersOn", false);
        ReflectionTestUtils.setField(sendNotificationService, "lettersOn", false);
        ReflectionTestUtils.setField(sendNotificationService, "interlocLettersOn", false);
        ReflectionTestUtils.setField(sendNotificationService, "docmosisLettersOn", false);
        return new NotificationService(notificationFactory, reminderService, notificationValidService,
            notificationHandler, outOfHoursCalculator, notificationConfig, sendNotificationService,
            true
        );
    }
}