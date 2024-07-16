package uk.gov.hmcts.reform.sscs.smoke.smoketest;

import io.restassured.RestAssured;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

public class GetSmokeCase {

    private final String appUrl = System.getenv("TEST_URL");

    @Test
    public void givenASmokeCase_retrieveFromCcd() {
        RestAssured.baseURI = appUrl;
        RestAssured.useRelaxedHTTPSValidation();

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
