package uk.gov.hmcts.reform.sscs.smoke;

import io.restassured.RestAssured;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class GetSmokeCase {

    private final String appUrl = System.getenv("TEST_URL");

    @Test
    public void givenASmokeCase_retrieveFromCcd() {
        RestAssured.baseURI = appUrl;
        RestAssured.useRelaxedHTTPSValidation();

        System.out.println("************* appUrl=" + appUrl);

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
