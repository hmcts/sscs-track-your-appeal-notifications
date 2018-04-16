package uk.gov.hmcts.sscs.functional;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import springfox.documentation.spring.web.json.Json;
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


//        IdamTokens idamTokens = IdamTokens.builder()
//                .authenticationService(idamService.generateServiceAuthorization())
//                .idamOauth2Token(idamService.getIdamOauth2Token())
//                .build();
//
//


        String code = RestAssured
                .given()
                .headers("Authorization", "Basic ZWR3YXJkLmJlbnNvbkBobWN0cy5uZXQ6cGFzc3dvcmQ=")
                .when().post("http://localhost:4501/oauth2/authorize?response_type=code&client_id=sscs&redirect_uri=https://localhost:9000/poc&continue-url=https://localhost:9000/poc")
                .then().extract().jsonPath().get("code");

        System.out.println("Jwt token is....." + code);

         String accessToken = RestAssured
                .given()
                 .headers("Content-Type", "application/x-www-form-urlencoded")
                .when().post("http://localhost:4501/oauth2/token?code=" + code + "&client_secret=QM5RQQ53LZFOSIXJ&client_id=sscs&redirect_uri=https://localhost:9000/poc&grant_type=authorization_code")
                .then().extract().jsonPath().get("access_token");

         System.out.println("The value of token is: ....." + accessToken );

//        String requestBody = generateString("UpdateCase.json");
        String s2sAuth = RestAssured
                .given()
//                .headers("Authorization", "Basic ZWR3YXJkLmJlbnNvbkBobWN0cy5uZXQ6cGFzc3dvcmQ=")
                .when().post("http://localhost:4502/testing-support/lease?microservice=ccd_gw")
                .then().extract().asString();

        System.out.println("s2s token is....." + s2sAuth);


        RestAssured.baseURI = "http://localhost:4452";
      String ccdRespond =  RestAssured.given()
                .header("authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("ServiceAuthorization", s2sAuth)
                .when()
                .get("/caseworkers/23/jurisdictions/SSCS/case-types/Benefit/cases/1523358358599932")
                .then().assertThat().statusCode(200).log().all()
              .extract().jsonPath().get().toString();

      System.out.println("This is the ccd response body" + ccdRespond);



    }

//    public static String generateString(String filename) throws IOException {
//        String filePath = System.getProperty("user.dir") + "\\json\\" + filename;
//        return new String(Files.readAllBytes(Paths.get(filePath)));
//    }
}
