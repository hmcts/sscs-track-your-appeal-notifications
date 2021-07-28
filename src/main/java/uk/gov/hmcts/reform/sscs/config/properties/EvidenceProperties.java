package uk.gov.hmcts.reform.sscs.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


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
        private String scottishLine3;
        private String town;
        private String county;
        private String postcode;
        private String scottishPostcode;
        private String telephone;
        private String telephoneWelsh;

        public String getLine3(SscsCaseData ccdResponse) {
            return "Yes".equalsIgnoreCase(ccdResponse.getIsScottishCase()) ? getScottishLine3() : getLine3();
        }

        public String getPostcode(SscsCaseData ccdResponse) {
            return "Yes".equalsIgnoreCase(ccdResponse.getIsScottishCase()) ? getScottishPostcode() : getPostcode();
        }
    }
}
