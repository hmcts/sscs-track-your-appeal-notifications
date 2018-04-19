package uk.gov.hmcts.sscs.scheduler.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.inject.Singleton;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import uk.gov.hmcts.sscs.scheduler.service.FailedJobRescheduler;

@Configuration
@ConfigurationProperties
public class QuartzConfiguration {

    private final Map<String, String> quartzProperties = new HashMap<>();

    // this getter is needed by the framework
    public Map<String, String> getQuartzProperties() {
        return quartzProperties;
    }

    @Bean
    public JobFactory jobFactory(ApplicationContext appCtx) {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(appCtx);

        return jobFactory;
    }

    @Bean
    @DependsOn("flywayInitializer")
    public SchedulerFactoryBean schedulerFactoryBean(JobFactory jobFactory) {
        Properties properties = new Properties();
        properties.putAll(quartzProperties);

        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setJobFactory(jobFactory);
        schedulerFactory.setQuartzProperties(properties);

        return schedulerFactory;
    }

    @Bean
    @Singleton
    public Scheduler scheduler(
        SchedulerFactoryBean factory,
        @Value("${retryPolicy.maxNumberOfJobExecutions}") int maxJobExecutionAttempts,
        @Value("${retryPolicy.delayBetweenAttemptsInMs}") long delayBetweenAttemptsInMs
    ) throws SchedulerException {

        Scheduler scheduler = factory.getScheduler();

        FailedJobRescheduler failedJobRescheduler = new FailedJobRescheduler(
            maxJobExecutionAttempts,
            Duration.ofMillis(delayBetweenAttemptsInMs)
        );

        scheduler.getListenerManager().addJobListener(failedJobRescheduler);
        scheduler.start();

        return scheduler;
    }
}
