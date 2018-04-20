//package uk.gov.hmcts.sscs.tya;
//
//import io.restassured.RestAssured;
//import io.restassured.http.ContentType;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.junit4.SpringRunner;
//import uk.gov.hmcts.sscs.models.idam.IdamTokens;
//import uk.gov.hmcts.sscs.service.idam.IdamService;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
//import static io.restassured.RestAssured.given;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//@ActiveProfiles("development")
//public class UpdateExistingCaseTest {
//
//
//    @Autowired
//    private IdamService idamService;
//
////    @Autowired
////    public UpdateExistingCaseTest(IdamService idamService){
////
////        this.idamService = idamService;
////
////    }
//
//    @Test
//    public void updateCase() throws IOException {
//
//        IdamTokens idamTokens = IdamTokens.builder()
//                .authenticationService(idamService.generateServiceAuthorization())
//                .idamOauth2Token(idamService.getIdamOauth2Token())
//                .build();
//
//        String requestBody = generateString("UpdateCase.json");
//
//        RestAssured.baseURI = "https://ccd-data-store-api-aat.service.core-compute-aat.internal";
//                given().
//                contentType(ContentType.JSON).
//                body(requestBody).
//                when().
//                post("\"caseworkers/6687/jurisdictions/SSCS/case-types/Benefit/cases/:case_reference\"").
//                then().assertThat().statusCode(200);
//
//
//    }
//
//    public static String generateString(String filename) throws IOException{
//        String filePath = System.getProperty("user.dir")+"\\json\\"+filename;
//        return new String(Files.readAllBytes(Paths.get(filePath)));
//    }
//}
