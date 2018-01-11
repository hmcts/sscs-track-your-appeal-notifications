package uk.gov.hmcts.sscs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import uk.gov.service.notify.NotificationClient;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.util.TimeZone;

@SpringBootApplication
public class TrackYourAppealNotificationsApplication {

    public static final String UTC = "UTC";

//    @Value("${gov.uk.notification.api.key}")
    private String apiKey = "test";

    @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone(UTC));
    }

    public static void main(String[] args) {
        SpringApplication.run(TrackYourAppealNotificationsApplication.class, args);
    }

    @Bean
    public NotificationClient notificationClient() throws MalformedURLException {
        return new NotificationClient(apiKey);
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource bean = new ReloadableResourceBundleMessageSource();
        bean.setBasename("classpath:application");
        bean.setDefaultEncoding("UTF-8");
        return bean;
    }

}
