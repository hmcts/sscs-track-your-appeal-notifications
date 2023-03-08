package uk.gov.hmcts.reform.sscs.config;

import java.util.Arrays;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.*;
import org.springframework.stereotype.Component;

@Component
public class PropertiesLoggingInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesLoggingInterceptor.class);

    @EventListener
    public void printPropertiesFromApplicationContext(ContextRefreshedEvent event) {
        //        ConfigurableEnvironment env = (ConfigurableEnvironment) event.getApplicationContext().getEnvironment();
        //
        //        env.getPropertySources().stream()
        //                .filter(propertySource -> propertySource instanceof MapPropertySource)
        //                .map(propertySource -> ((MapPropertySource) propertySource).getSource().keySet())
        //                .flatMap(Collection::stream)
        //                .distinct()
        //                .sorted()
        //                .forEach(key -> {
        //                    String propValue = env.getProperty(key);
        //                    String propValueStripped = propValue.length() >= 5 ? propValue.substring(0, 4) : propValue;
        //                    LOG.info("{} : {}", key, propValueStripped);
        //                });

        final Environment env = event.getApplicationContext().getEnvironment();
        LOG.info("====== Environment and configuration ======");
        LOG.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
        final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
        StreamSupport.stream(sources.spliterator(), false)
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                .flatMap(Arrays::stream)
                .distinct()
                .forEach(prop -> {
                    String propValue = env.getProperty(prop);
                    String propValueStripped = propValue.length() >= 5 ? propValue.substring(0, 4) : propValue;
                    LOG.info("{} : {}", prop, propValueStripped);
                });
        LOG.info("===========================================");
    }
}
