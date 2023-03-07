package uk.gov.hmcts.reform.sscs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class PropertiesLoggingInterceptor {

    Logger LOGGER = LoggerFactory.getLogger(this.getClass().getName());

    @EventListener
    public void printPropertiesFromApplicationContext(ContextRefreshedEvent event){
        ConfigurableEnvironment env = (ConfigurableEnvironment) event.getApplicationContext().getEnvironment();

        env.getPropertySources().stream()
                .filter(propertySource -> propertySource instanceof MapPropertySource)
                .map(propertySource -> ((MapPropertySource) propertySource).getSource().keySet())
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .forEach(key -> LOGGER.info("{} : {}", key, env.getProperty(key).substring(0, 4)));
    }
}
