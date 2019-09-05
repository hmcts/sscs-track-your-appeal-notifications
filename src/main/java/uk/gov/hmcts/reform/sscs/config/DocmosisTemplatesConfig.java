package uk.gov.hmcts.reform.sscs.config;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties("pdf-service")
@Getter
@Setter
public class DocmosisTemplatesConfig {
    private Map<String, String> coversheets;
    private String hmctsImgKey;
    private String hmctsImgVal;
    private String hmctsImgKey1;
}
