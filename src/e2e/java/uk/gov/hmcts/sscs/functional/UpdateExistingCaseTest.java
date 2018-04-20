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
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${idam.jwt.url}")
    private String idamJwtCodeUrl;
    @Value("${idam.code.authorization.header.value}")
    private String idamJwtCodeauthheader;
    @Value("${idam.url}")
    private String idamUrl;
    @Value("${idam.oauth2.client.id}")
    private String idamClientId;
    @Value("${idam.oauth2.redirectUrl}")
    private String idamRedirectUrl;
    @Value("${idam.oauth2.client.secret}")
    private String idamClientSecret;
    @Value("${idam.grant-type}")
    private String idamGrantType;
    @Value("${s2s.url}")
    private String s2sUrl;
    @Value("${s2s.token.endpoint}")
    private String s2sEndpoint;
    @Value("${ccd.datastore.url}")
    private String ccdDataStoreUrl;
    @Value("${ccd.retrieve.endpoint}")
    private String ccdRetrieveEndpoint;




    @Test
    public void updateCase() throws IOException {

        String caseId = "1524129980637591";


        String code = RestAssured
                .given()
                .headers("Authorization", "Basic" + " " + idamJwtCodeauthheader)
                .when().post(idamUrl + idamJwtCodeUrl + "&client_id=" + idamClientId + "&redirect_uri="
                        + idamRedirectUrl + "&continue-url=" + idamRedirectUrl)
                .then().extract().jsonPath().get("code");


         String accessToken = RestAssured
                .given()
                 .headers("Content-Type", "application/x-www-form-urlencoded")
                .when().post(idamUrl + "/oauth2/token?code=" + code + "&client_secret=" + idamClientSecret +
                         "&client_id=" + idamClientId + "&redirect_uri=" + idamRedirectUrl + "&grant_type=" + idamGrantType)
                .then().extract().jsonPath().get("access_token");

        System.out.println("access token is =======" + " " + accessToken);


        String s2sAuth = RestAssured
                .given()
                .when().post(s2sUrl + s2sEndpoint)
                .then().extract().asString();

        System.out.println("s2s token is ======" + " " + s2sAuth);


        //Create a case====================================================================================

//        RestAssured.baseURI = "http://localhost:4452";
//        String createToken = RestAssured.given()
//                .header("Authorization", "Bearer"+ " " + accessToken)
//                .header("Content-Type", "application/json")
//                .header("ServiceAuthorization", s2sAuth)
//                .when()
//                .get("/caseworkers/23/jurisdictions/SSCS/case-types/Benefit/event-triggers/appealReceived/token?ignore-warning=")
//                .then().assertThat().statusCode(200)
//                .extract().jsonPath().get("token");
//
//        System.out.println("This is the update eventToken " + createToken);

        //===================================================================================================


        RestAssured.baseURI = ccdDataStoreUrl;
      String ccdRespond =  RestAssured.given()
                .header("authorization", "Bearer"+ " " + accessToken)
                .header("Content-Type", "application/json")
                .header("ServiceAuthorization", s2sAuth)
                .when()
                .get(ccdRetrieveEndpoint + caseId)
                .then().assertThat().statusCode(200)
              .extract().jsonPath().get().toString();

      //GET token to perform update

        RestAssured.baseURI = "http://localhost:4452";
        String eventToken = RestAssured.given()
                .header("Authorization", "Bearer"+ " " + accessToken)
                .header("Content-Type", "application/json")
                .header("ServiceAuthorization", s2sAuth)
                .when()
                .get("/caseworkers/23/jurisdictions/sscs/case-types/Benefit/cases/1523358358599932/event-triggers/appealReceived/token")
                .then().assertThat().statusCode(200)
                .extract().jsonPath().get("token");

        System.out.println("This is the update eventToken" + eventToken);
    }

//    public static String generateString(String filename) throws IOException {
//        String filePath = System.getProperty("user.dir") + "\\json\\" + filename;
//        return new String(Files.readAllBytes(Paths.get(filePath)));
//    }
}
