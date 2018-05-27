package uk.gov.hmcts.sscs;

import java.util.TimeZone;
import javax.annotation.PostConstruct;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import uk.gov.hmcts.reform.sscs.jobscheduler.config.QuartzConfiguration;
import uk.gov.hmcts.sscs.deserialize.CcdResponseWrapperDeserializer;
import uk.gov.service.notify.NotificationClient;

@EnableFeignClients
@SpringBootApplication
@ComponentScan(
    basePackages = "uk.gov.hmcts.reform.sscs",
    basePackageClasses = TrackYourAppealNotificationsApplication.class,
    lazyInit = true
)
public class TrackYourAppealNotificationsApplication {

    public static final String UTC = "UTC";

    @Value("${gov.uk.notification.api.key}")
    private String apiKey;

    @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone(UTC));
    }

    public static void main(String[] args) {
        SpringApplication.run(TrackYourAppealNotificationsApplication.class, args);
    }

    @Bean
    public NotificationClient notificationClient() {
        return new NotificationClient(apiKey);
    }

    @Bean
    public CcdResponseWrapperDeserializer ccdResponseWrapperDeserializer() {
        return new CcdResponseWrapperDeserializer();
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource bean = new ReloadableResourceBundleMessageSource();
        bean.setBasename("classpath:application");
        bean.setDefaultEncoding("UTF-8");
        return bean;
    }

    @Bean
    public JobFactory jobFactory(ApplicationContext context, FlywayMigrationInitializer flywayInitializer) {
        return (new QuartzConfiguration()).jobFactory(context);
    }

}
