package uk.gov.hmcts.reform.sscs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties("pdf-service")
@Getter
@Setter
public class DocmosisTemplatesConfig {
    private Map<String, String> templates;
}
