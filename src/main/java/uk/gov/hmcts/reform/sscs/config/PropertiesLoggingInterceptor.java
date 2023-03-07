package uk.gov.hmcts.reform.sscs.config;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

@Component
public class PropertiesLoggingInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesLoggingInterceptor.class);

    @EventListener
    public void printPropertiesFromApplicationContext(ContextRefreshedEvent event) {
        ConfigurableEnvironment env = (ConfigurableEnvironment) event.getApplicationContext().getEnvironment();

        env.getPropertySources().stream()
                .filter(propertySource -> propertySource instanceof MapPropertySource)
                .map(propertySource -> ((MapPropertySource) propertySource).getSource().keySet())
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .forEach(key -> {
                    String propValue = env.getProperty(key);
                    String propValueStripped = propValue.length() >= 5 ? propValue.substring(0, 4) : propValue;
                    LOG.info("{} : {}", key, propValueStripped);
                });
    }
}
