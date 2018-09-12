package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.QUESTION_ROUND_ISSUED_NOTIFICATION;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

public class CohNotificationFunctionalTest extends AbstractFunctionalTest {
    private static final  String COH_URL = "http://coh-cor-aat.service.core-compute-aat.internal";

    @Value("${notification.question_round_issued.emailId}")
    private String questionRoundIssuedEmailTemplateId;

    @Value("${notification.question_round_issued.smsId}")
    private String questionRoundIssuedSmsTemplateId;


    @Test
    public void shouldSendQuestionsReadyNotifications() throws IOException, InterruptedException, NotificationClientException {
        String hearingId = createHearingWithQuestions(caseId);
        simulateCohCallback(QUESTION_ROUND_ISSUED_NOTIFICATION, hearingId);

        List<Notification> notifications = tryFetchNotificationsForTestCase(questionRoundIssuedEmailTemplateId, questionRoundIssuedSmsTemplateId);

        assertNotificationBodyContains(notifications, questionRoundIssuedEmailTemplateId, caseData.getCaseReference());
    }

    private String createHearingWithQuestions(Long caseId) throws InterruptedException {
        String hearingId = createHearing(caseId);
        createQuestion(hearingId);
        issueQuestions(hearingId);

        return hearingId;
    }

    private String createHearing(Long caseId) {
        String createHearingJson = "{\n"
                + "  \"case_id\": \"" + caseId + "\",\n"
                + "  \"jurisdiction\": \"SSCS\",\n"
                + "  \"start_date\": \"2018-08-17T15:20:37.746Z\",\n"
                + "  \"state\": \"string\"\n"
                + "}";

        RestAssured.useRelaxedHTTPSValidation();
        Response createHearingResponse = makeRequest(createHearingJson)
                .post(COH_URL + "/continuous-online-hearings");
        return checkResponseCreated(createHearingResponse)
                .contentType(ContentType.JSON)
                .extract().response()
                .jsonPath().getString("online_hearing_id");
    }

    private void createQuestion(String hearingId) {
        String createQuestionJson = "{\n"
                + "  \"owner_reference\": \"string\",\n"
                + "  \"question_body_text\": \"string\",\n"
                + "  \"question_header_text\": \"string\",\n"
                + "  \"question_ordinal\": \"1\",\n"
                + "  \"question_round\": \"1\"\n"
                + "}";
        Response createQuestionResponse = makeRequest(createQuestionJson)
                .post(COH_URL + "/continuous-online-hearings/" + hearingId + "/questions");
        checkResponseCreated(createQuestionResponse);
    }

    private void issueQuestions(String hearingId) throws InterruptedException {
        makeRequest("{\"state_name\": \"question_issue_pending\"}")
                .put(COH_URL + "/continuous-online-hearings/" + hearingId + "/questionrounds/1")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Need to wait 60 seconds for the question round to be issued, need to find a better way to do this.
        Thread.sleep(60000);
    }

    private ValidatableResponse checkResponseCreated(Response request) {
        return request
                .then()
                .statusCode(HttpStatus.CREATED.value());
    }

    private RequestSpecification makeRequest(String createQuestionJson) {
        return RestAssured
                .given()
                .header(HttpHeaders.AUTHORIZATION, "someValue")
                .header("ServiceAuthorization", "someValue")
                .contentType("application/json")
                .body(createQuestionJson)
                .when();
    }
}
