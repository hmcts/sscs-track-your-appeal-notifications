package uk.gov.hmcts.reform.sscs.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "evidence")
@Getter
public class EvidenceProperties {
    @Getter
    @Setter
    private EvidenceAddress address;

    @Getter
    @Setter
    public static class EvidenceAddress {
        private String line1;
        private String line2;
        private String line3;
        private String town;
        private String county;
        private String postcode;
        private String telephone;
        private String telephoneWelsh;
    }
}
