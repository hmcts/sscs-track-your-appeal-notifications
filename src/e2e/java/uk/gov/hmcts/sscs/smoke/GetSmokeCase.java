package uk.gov.hmcts.sscs.smoke;

import io.restassured.RestAssured;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class GetSmokeCase {

    private final String caseLoaderInstance = System.getenv("TEST_URL");

    @Test
    public void givenASmokeCase_retrieveFromCcd() {
        RestAssured.baseURI = caseLoaderInstance;
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



