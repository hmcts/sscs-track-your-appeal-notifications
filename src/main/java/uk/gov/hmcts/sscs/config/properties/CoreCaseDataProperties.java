package uk.gov.hmcts.sscs.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@Validated
@ConfigurationProperties(prefix = "core_case_data")
@Getter
@Setter
public class CoreCaseDataProperties {
    @NotBlank
    private String userId;
    @NotBlank
    private String jurisdictionId;
    @NotBlank
    private String caseTypeId;
    private Api api;

    @Getter
    @Setter
    @ToString
    public static class Api {
        @NotBlank
        private String url;
    }
}
