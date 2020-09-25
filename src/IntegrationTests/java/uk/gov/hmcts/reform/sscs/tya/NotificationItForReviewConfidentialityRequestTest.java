package uk.gov.hmcts.reform.sscs.tya;

import static helper.IntegrationTestHelper.getRequestWithAuthHeader;
import static helper.IntegrationTestHelper.updateEmbeddedJson;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class NotificationItForReviewConfidentialityRequestTest extends NotificationsIt {

    @Test
    @Parameters({"granted", "refused"})
    public void givenAppellantConfidentialityRequest_shouldSendConfidentialityLetter(String requestOutcome) throws Exception {
        String path = getClass().getClassLoader().getResource("json/ccdResponseWithJointParty.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = updateEmbeddedJson(json, "reviewConfidentialityRequest", "event_id");
        json = updateEmbeddedJson(json, requestOutcome, "case_details", "case_data", "confidentialityRequestOutcomeAppellant");

        getResponse(getRequestWithAuthHeader(json));

        verify(notificationClient, times(0)).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(0)).sendSms(any(), any(), any(), any(), any());
        verify(notificationClient, times(1)).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    @Parameters({"granted", "refused"})
    public void givenJointPartyConfidentialityRequest_shouldSendConfidentialityLetter(String requestOutcome) throws Exception {
        String path = getClass().getClassLoader().getResource("json/ccdResponseWithJointParty.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = updateEmbeddedJson(json, "reviewConfidentialityRequest", "event_id");
        json = updateEmbeddedJson(json, requestOutcome, "case_details", "case_data", "confidentialityRequestOutcomeJointParty");

        getResponse(getRequestWithAuthHeader(json));

        verify(notificationClient, times(0)).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(0)).sendSms(any(), any(), any(), any(), any());
        verify(notificationClient, times(1)).sendPrecompiledLetterWithInputStream(any(), any());
    }

    @Test
    @Parameters({"granted", "refused"})
    public void givenJointPartyAndAppellantConfidentialityRequest_shouldSendBothConfidentialityLetters(String requestOutcome) throws Exception {
        String path = getClass().getClassLoader().getResource("json/ccdResponseWithJointParty.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        json = updateEmbeddedJson(json, "reviewConfidentialityRequest", "event_id");
        json = updateEmbeddedJson(json, requestOutcome, "case_details", "case_data", "confidentialityRequestOutcomeAppellant");
        json = updateEmbeddedJson(json, requestOutcome, "case_details", "case_data", "confidentialityRequestOutcomeJointParty");

        getResponse(getRequestWithAuthHeader(json));

        verify(notificationClient, times(0)).sendEmail(any(), any(), any(), any());
        verify(notificationClient, times(0)).sendSms(any(), any(), any(), any(), any());
        verify(notificationClient, times(2)).sendPrecompiledLetterWithInputStream(any(), any());
    }
}
