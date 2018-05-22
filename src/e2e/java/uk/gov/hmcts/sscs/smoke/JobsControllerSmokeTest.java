package uk.gov.hmcts.sscs.smoke;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class JobsControllerSmokeTest {

    @Value("${test-url:http://localhost:8484}")
    private String testUrl;

    @Before
    public void setup() {
        RestAssured.baseURI = testUrl;
    }

    @Test
    public void create_jobs_should_require_s2s_auth() {
        RestAssured.given()
            .relaxedHTTPSValidation()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .when().post("/jobs").then().statusCode(400);
    }

    @Test
    public void retrieve_jobs_should_require_s2s_auth() {
        RestAssured.given()
            .relaxedHTTPSValidation()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .when().get("/jobs").then().statusCode(400);
    }
}
