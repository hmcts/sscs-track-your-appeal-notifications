package uk.gov.hmcts.reform.sscs.config;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sscs.TrackYourAppealNotificationsApplication;

@Configuration
public class SwaggerConfiguration {

//    @Bean
//    public Docket api() {
//        return new Docket(DocumentationType.SWAGGER_2)
//            .useDefaultResponseMessages(false)
//            .select()
//            .apis(RequestHandlerSelectors.basePackage(TrackYourAppealNotificationsApplication.class.getPackage().getName() + ".controllers"))
//            .paths(PathSelectors.any())
//            .build();
//    }

    @Bean
    public GroupedOpenApi api() {
        return GroupedOpenApi.builder()
            .pathsToMatch("*")
            .build();
    }

}
