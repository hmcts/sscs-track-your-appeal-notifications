package uk.gov.hmcts.reform.sscs.smoke;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.http.HttpStatus;

@Slf4j
public class GetSmokeCase {

    private final String appUrl = System.getenv("TEST_URL");

    @Test
    public void givenASmokeCase_retrieveFromCcd() {
        RestAssured.baseURI = appUrl;
        RestAssured.useRelaxedHTTPSValidation();

        log.info("************* appUrl=" + appUrl);

        RestAssured
            .given()
            .when()
            .get("/smoke-test/")
            .then()
            .statusCode(HttpStatus.OK.value())
            .and()
            .extract().body().asString();
    }
}
