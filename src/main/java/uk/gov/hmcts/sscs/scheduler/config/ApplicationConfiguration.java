package uk.gov.hmcts.sscs.scheduler.config;

import org.springframework.boot.actuate.trace.TraceProperties;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.api.filters.SensitiveHeadersRequestTraceFilter;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public SensitiveHeadersRequestTraceFilter requestTraceFilter(
        TraceRepository traceRepository,
        TraceProperties traceProperties
    ) {
        return new SensitiveHeadersRequestTraceFilter(traceRepository, traceProperties);
    }
}
