package uk.gov.hmcts.reform.sscs;

import static java.util.Arrays.asList;

import com.microsoft.applicationinsights.web.internal.ApplicationInsightsServletContextListener;
import java.util.Properties;
import java.util.TimeZone;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContextListener;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.jobscheduler.config.QuartzConfiguration;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.JobClassMapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.JobClassMapping;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.JobMapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.JobMapping;
import uk.gov.hmcts.reform.sscs.service.NotificationService;
import uk.gov.hmcts.reform.sscs.service.scheduler.CcdActionDeserializer;
import uk.gov.hmcts.reform.sscs.service.scheduler.CcdActionExecutor;
import uk.gov.hmcts.reform.sscs.service.scheduler.CcdActionSerializer;
import uk.gov.hmcts.reform.sscs.service.scheduler.CohActionDeserializer;
import uk.gov.hmcts.reform.sscs.service.scheduler.CohActionExecutor;
import uk.gov.hmcts.reform.sscs.service.scheduler.CohActionSerializer;
import uk.gov.hmcts.reform.sscs.service.scheduler.CohJobPayload;
import uk.gov.service.notify.NotificationClient;

@SpringBootApplication
@EnableFeignClients(basePackages =
        {
                "uk.gov.hmcts.reform.sscs.service.coh"
        })
@EnableRetry
public class TrackYourAppealNotificationsApplication {

    public static final String UTC = "UTC";

    @Value("${appeal.email.host}")
    private String emailHost;

    @Value("${appeal.email.port}")
    private int emailPort;

    @Value("${appeal.email.smtp.tls.enabled}")
    private String smtpTlsEnabled;

    @Value("${gov.uk.notification.api.key}")
    private String apiKey;

    @Value("${gov.uk.notification.api.testKey}")
    private String testApiKey;

    @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone(UTC));
    }

    public static void main(String[] args) {
        SpringApplication.run(TrackYourAppealNotificationsApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ServletListenerRegistrationBean<ServletContextListener> appInsightsServletContextListenerRegistrationBean(
                                ApplicationInsightsServletContextListener applicationInsightsServletContextListener) {
        ServletListenerRegistrationBean<ServletContextListener> srb =
            new ServletListenerRegistrationBean<>();
        srb.setListener(applicationInsightsServletContextListener);
        return srb;
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationInsightsServletContextListener applicationInsightsServletContextListener() {
        return new ApplicationInsightsServletContextListener();
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(emailHost);
        javaMailSender.setPort(emailPort);
        Properties properties = new Properties();
        properties.setProperty("mail.transport.protocol","smtp");
        properties.setProperty("mail.smtp.starttls.enable", smtpTlsEnabled);
        properties.put("mail.smtp.ssl.trust","*");
        javaMailSender.setJavaMailProperties(properties);
        return javaMailSender;
    }

    @Bean
    @Primary
    public NotificationClient notificationClient() {
        return new NotificationClient(apiKey);
    }

    @Bean
    public NotificationClient testNotificationClient() {
        return new NotificationClient(testApiKey);
    }

    @Bean
    public SscsCaseDataWrapperDeserializer sscsCaseDataWrapperDeserializer() {
        return new SscsCaseDataWrapperDeserializer();
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource bean = new ReloadableResourceBundleMessageSource();
        bean.setBasename("classpath:application");
        bean.setDefaultEncoding("UTF-8");
        return bean;
    }

    @Bean
    @ConditionalOnProperty("spring.flyway.enabled")
    public JobFactory jobFactory(ApplicationContext context, FlywayMigrationInitializer flywayInitializer) {
        return (new QuartzConfiguration()).jobFactory(context);
    }

    @Bean
    public CcdRequestDetails getRequestDetails(@Value("${core_case_data.jurisdictionId}") String coreCaseDataJurisdictionId,
                                               @Value("${core_case_data.caseTypeId}") String coreCaseDataCaseTypeId) {
        return CcdRequestDetails.builder()
                .caseTypeId(coreCaseDataCaseTypeId)
                .jurisdictionId(coreCaseDataJurisdictionId)
                .build();
    }

    @Bean
    public JobMapper getJobMapper(CohActionDeserializer cohActionDeserializer,
                                  CcdActionDeserializer ccdActionDeserializer,
                                  NotificationService notificationService,
                                  CcdService ccdService,
                                  SscsCaseDataWrapperDeserializer deserializer,
                                  IdamService idamService) {
        // Had to wire these up like this Spring will not wire up CcdActionExecutor otherwise.
        CohActionExecutor cohActionExecutor = new CohActionExecutor(notificationService, ccdService, deserializer, idamService);
        CcdActionExecutor ccdActionExecutor = new CcdActionExecutor(notificationService, ccdService, deserializer, idamService);
        return new JobMapper(asList(
                new JobMapping<>(payload -> payload.contains("onlineHearingId"), cohActionDeserializer, cohActionExecutor),
                new JobMapping<>(payload -> !payload.contains("onlineHearingId"), ccdActionDeserializer, ccdActionExecutor)
        ));
    }

    @Bean
    public JobClassMapper getJobClassMapper(CohActionSerializer cohActionSerializer,
                                            CcdActionSerializer ccdActionSerializer) {
        return new JobClassMapper(asList(
                new JobClassMapping<>(CohJobPayload.class, cohActionSerializer),
                new JobClassMapping<>(String.class, ccdActionSerializer)
        ));
    }
}
