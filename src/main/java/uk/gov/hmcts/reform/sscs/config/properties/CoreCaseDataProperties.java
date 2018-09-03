package uk.gov.hmcts.reform.sscs.config.properties;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@Validated
@ConfigurationProperties(prefix = "ccd")
@Getter
@Setter
public class CoreCaseDataProperties {

    @NotBlank
    private String jurisdictionId;
    @NotBlank
    private String caseTypeId;
}
