package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.SscsCaseDataUtils.builderSscsCaseData;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SYA_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.OnlinePanel;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClientException;

public class CohNotificationFunctionalTest extends AbstractFunctionalTest {
    private static final String COH_URL = "http://coh-cor-aat.service.core-compute-aat.internal";

    @Value("${notification.question_round_issued.emailId}")
    private String questionRoundIssuedEmailTemplateId;

    @Value("${notification.question_round_issued.smsId}")
    private String questionRoundIssuedSmsTemplateId;

    @Value("${notification.follow_up_question_round_issued.emailId}")
    private String followupQuestionRoundIssuedEmailTemplateId;

    @Value("${notification.follow_up_question_round_issued.smsId}")
    private String followupQuestionRoundIssuedSmsTemplateId;

    @Value("${notification.question_deadline_elapsed.emailId}")
    private String questionDeadlineElapsedEmailTemplateId;

    @Value("${notification.question_deadline_elapsed.smsId}")
    private String questionDeadlineElapsedSmsTemplateId;

    @Value("${notification.online.responseReceived.emailId}")
    private String onlineResponseReceivedEmailId;

    @Value("${notification.online.responseReceived.smsId}")
    private String onlineResponseReceivedSmsId;

    @Value("${notification.decision_issued.emailId}")
    private String viewIssuedEmailTemplateId;

    @Value("${notification.decision_issued.smsId}")
    private String viewIssuedSmsIdTemplate;

    public CohNotificationFunctionalTest() {
        super(30);
    }

    @Override
    protected SscsCaseData createCaseData() {
        SscsCaseData.SscsCaseDataBuilder sscsCaseDataBuilder = builderSscsCaseData(caseReference, "Yes", "Yes", SYA_APPEAL_CREATED, "cor");
        return sscsCaseDataBuilder.onlinePanel(
                OnlinePanel.builder()
                        .assignedTo("Judge")
                        .medicalMember("medic")
                        .disabilityQualifiedMember("disQualMember")
                        .build())
                .build();
    }

    @Test
    @Ignore //TODO remove ignore once the Coh service in AAT is working fine again
    public void shouldSendQuestionsReadyNotifications() throws IOException, InterruptedException, NotificationClientException {
        String hearingId = createHearingWithQuestions(caseId);
        // Issuing the question round will cause these notifications to be fired from AAT
        tryFetchNotificationsForTestCase(questionRoundIssuedEmailTemplateId, questionRoundIssuedSmsTemplateId);

        simulateCohCallback(QUESTION_ROUND_ISSUED_NOTIFICATION, hearingId);

        // Need to check for two sets of notifications one from AAT and from the test being run.
        List<Notification> notifications = tryFetchNotificationsForTestCase(questionRoundIssuedEmailTemplateId, questionRoundIssuedEmailTemplateId,
                questionRoundIssuedSmsTemplateId, questionRoundIssuedSmsTemplateId);

        assertNotificationBodyContains(notifications, questionRoundIssuedEmailTemplateId, caseData.getCaseReference());
    }

    @Test
    public void shouldSendFollowUpQuestionsReadyNotifications() throws IOException, InterruptedException, NotificationClientException {
        String hearingId = createHearingWithQuestions(caseId);
        createQuestion(hearingId, 2);
        issueQuestions(hearingId, 2);
        // Issuing the question round will cause these notifications to be fired from AAT todo put in once this is deployed to AAT
        //tryFetchNotificationsForTestCase(followupQuestionRoundIssuedEmailTemplateId, followupQuestionRoundIssuedSmsTemplateId);

        simulateCohCallback(QUESTION_ROUND_ISSUED_NOTIFICATION, hearingId);

        // Need to check for two sets of notifications one from AAT and from the test being run.
        List<Notification> notifications = tryFetchNotificationsForTestCase(
                followupQuestionRoundIssuedEmailTemplateId,
                followupQuestionRoundIssuedSmsTemplateId
        );

        assertNotificationBodyContains(notifications, followupQuestionRoundIssuedEmailTemplateId, caseData.getCaseReference());
    }

    @Test
    public void shouldSendQuestionDeadlineElapsedNotifications() throws IOException, InterruptedException, NotificationClientException {
        String hearingId = createHearingWithQuestions(caseId);

        simulateCohCallback(QUESTION_DEADLINE_ELAPSED_NOTIFICATION, hearingId);

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                questionDeadlineElapsedEmailTemplateId, questionDeadlineElapsedSmsTemplateId);

        assertNotificationBodyContains(notifications, questionDeadlineElapsedEmailTemplateId, caseData.getCaseReference());
    }

    @Test
    public void shouldSendViewIssuedNotifications() throws IOException, InterruptedException, NotificationClientException {
        String hearingId = createHearingWithQuestions(caseId);

        simulateCohCallback(VIEW_ISSUED, hearingId);

        List<Notification> notifications = tryFetchNotificationsForTestCase(
                viewIssuedEmailTemplateId, viewIssuedSmsIdTemplate);

        assertNotificationBodyContains(notifications, viewIssuedEmailTemplateId, caseData.getCaseReference());
    }

    private String createHearingWithQuestions(Long caseId) throws InterruptedException {
        String hearingId = createHearing(caseId);
        System.out.println("Created online hearing [" + hearingId + "] case id [" + caseId + "]");
        createQuestion(hearingId, 1);
        issueQuestions(hearingId, 1);

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

    private void createQuestion(String hearingId, int round) {
        String createQuestionJson = "{\n"
                + "  \"owner_reference\": \"string\",\n"
                + "  \"question_body_text\": \"string\",\n"
                + "  \"question_header_text\": \"string\",\n"
                + "  \"question_ordinal\": \"1\",\n"
                + "  \"question_round\": \"" + round + "\"\n"
                + "}";
        Response createQuestionResponse = makeRequest(createQuestionJson)
                .post(COH_URL + "/continuous-online-hearings/" + hearingId + "/questions");
        checkResponseCreated(createQuestionResponse);
    }

    private void issueQuestions(String hearingId, int round) throws InterruptedException {
        makeRequest("{\"state_name\": \"question_issue_pending\"}")
                .put(COH_URL + "/continuous-online-hearings/" + hearingId + "/questionrounds/" + round)
                .then()
                .statusCode(HttpStatus.OK.value());

        waitUntil(roundIssued(hearingId, round), 20L);
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

    private Supplier<Boolean> roundIssued(String hearingId, int round) {
        return () -> {
            Response response = RestAssured
                    .given()
                    .header(HttpHeaders.AUTHORIZATION, "someValue")
                    .header("ServiceAuthorization", "someValue")
                    .when()
                    .get(COH_URL + "/continuous-online-hearings/" + hearingId + "/questionrounds/" + round);
            String roundState = response.then()
                    .statusCode(HttpStatus.OK.value())
                    .contentType(ContentType.JSON)
                    .extract()
                    .response()
                    .jsonPath().getString("question_round_state.state_name");
            return "question_issued".equals(roundState);
        };
    }

    private static void waitUntil(Supplier<Boolean> condition, long timeoutInSeconds) throws InterruptedException {
        long timeout = timeoutInSeconds * 1000L * 1000000L;
        long startTime = System.nanoTime();
        while (true) {
            if (condition.get()) {
                System.out.println("Question round issued after [" + ((System.nanoTime() - startTime) / (1000L * 1000000L)) + "] seconds");
                break;
            } else if (System.nanoTime() - startTime >= timeout) {
                throw new RuntimeException("Question round has not been issues in [" + timeoutInSeconds + "] seconds.");
            }
            Thread.sleep(100L);
        }
    }
}
