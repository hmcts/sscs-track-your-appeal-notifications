package uk.gov.hmcts.sscs.functional;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.sscs.models.idam.IdamTokens;
import uk.gov.hmcts.sscs.service.idam.IdamService;



@RunWith(SpringRunner.class)
@SpringBootTest
public class UpdateExistingCaseTest {


    @Autowired
    private IdamService idamService;


    @Test
    public void updateCase() throws IOException {

        String eventId = "updateContactDetails";


        IdamTokens idamTokens = IdamTokens.builder()
                .authenticationService(idamService.generateServiceAuthorization())
                .idamOauth2Token(idamService.getIdamOauth2Token())
                .build();


        String requestBody = generateString("UpdateCase.json");

        RestAssured.baseURI = "http://localhost:4451";
        given()
        .header("idamOauth2Token", idamTokens.getIdamOauth2Token())
        .header("serviceAuthorization", idamTokens.getAuthenticationService())
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .get("/caseworkers/{userId}/jurisdictions/{jurisdictionId}/case-types/{caseType}/event-triggers/" + eventId + "/tokens")
        .then().assertThat().statusCode(200).log().all();
    }

    public static String generateString(String filename) throws IOException {
        String filePath = System.getProperty("user.dir") + "\\json\\" + filename;
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
}
