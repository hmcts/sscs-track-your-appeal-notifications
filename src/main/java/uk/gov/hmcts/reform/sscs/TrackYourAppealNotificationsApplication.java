package uk.gov.hmcts.reform.sscs;

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
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.jobscheduler.config.QuartzConfiguration;
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

}
