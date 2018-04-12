package uk.gov.hmcts.reform.sscs.functional;

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

        IdamTokens idamTokens = IdamTokens.builder()
                .authenticationService(idamService.generateServiceAuthorization())
                .idamOauth2Token(idamService.getIdamOauth2Token())
                .build();

        //        String requestBody = generateString("UpdateCase.json");
        //
        //        RestAssured.baseURI = "https://ccd-data-store-api-aat.service.core-compute-aat.internal";
        //        given().
        //                relaxedHTTPSValidation().
        //                header("Authorization", idamTokens).
        //                contentType(ContentType.JSON).
        //                body(requestBody).
        //                when().
        //                post("\"caseworkers/6687/jurisdictions/SSCS/case-types/Benefit/cases/:case_reference\"").
        //                then().assertThat().statusCode(200);


    }

    public static String generateString(String filename) throws IOException {
        String filePath = System.getProperty("user.dir") + "\\json\\" + filename;
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
}
