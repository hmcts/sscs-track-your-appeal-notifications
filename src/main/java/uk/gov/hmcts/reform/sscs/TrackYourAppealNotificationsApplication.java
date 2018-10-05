package uk.gov.hmcts.reform.sscs;

import static java.util.Arrays.asList;

import java.util.TimeZone;
import javax.annotation.PostConstruct;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.retry.annotation.EnableRetry;
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
import uk.gov.hmcts.reform.sscs.service.scheduler.*;
import uk.gov.service.notify.NotificationClient;

@SpringBootApplication
@ComponentScan(
    basePackages = "uk.gov.hmcts.reform.sscs",
    basePackageClasses = TrackYourAppealNotificationsApplication.class,
    lazyInit = true
)
@EnableFeignClients(basePackages =
        {
                "uk.gov.hmcts.reform.sscs.service.coh"
        })
@EnableRetry
public class TrackYourAppealNotificationsApplication {

    public static final String UTC = "UTC";

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
