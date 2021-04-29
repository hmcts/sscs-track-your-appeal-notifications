package uk.gov.hmcts.reform.sscs.tya;

import static helper.IntegrationTestHelper.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

public class NotificationsDelayedNotificationsIt extends NotificationsItBase {

    @Before
    public void setup() throws Exception {
        super.setup();

        setupReminderController(getNotificationService());
    }

    @Test
    @Parameters(method = "generateAppointeeNotificationScenariosForDelayedNotifications")
    public void shouldSendAppointeeNotificationsForADelayedNotificationEventForAnOralOrPaperHearingAndForEachSubscription(
            NotificationEventType notificationEventType, String hearingType, List<String> expectedEmailTemplateIds,
            List<String> expectedSmsTemplateIds, List<String> expectedLetterTemplateIds, String appointeeEmailSubs,
            String appointeeSmsSubs, int wantedNumberOfSendEmailInvocations, int wantedNumberOfSendSmsInvocations,
            int wantedNumberOfSendLetterInvocations, String expectedName) throws Exception {

        String path = getClass().getClassLoader().getResource("json/ccdResponseWithAppointee.json").getFile();
        String jsonAppointee = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        jsonAppointee = updateEmbeddedJson(jsonAppointee, hearingType, "case_details", "case_data", "appeal", "hearingType");

        if (notificationEventType.getId().contains("Welsh")) {
            jsonAppointee = updateEmbeddedJson(jsonAppointee, "Yes", "case_details", "case_data", "languagePreferenceWelsh");
        }
        jsonAppointee = updateEmbeddedJson(jsonAppointee, appointeeEmailSubs, "case_details", "case_data", "subscriptions",
                "appointeeSubscription", "subscribeEmail");
        jsonAppointee = updateEmbeddedJson(jsonAppointee, appointeeSmsSubs, "case_details", "case_data", "subscriptions",
                "appointeeSubscription", "subscribeSms");

        if (notificationEventType.equals(HEARING_BOOKED_NOTIFICATION)) {
            jsonAppointee = jsonAppointee.replace("appealReceived", "hearingBooked");
            jsonAppointee = jsonAppointee.replace("2018-01-12", LocalDate.now().plusDays(2).toString());
        }

        if (notificationEventType.equals(REQUEST_INFO_INCOMPLETE)) {
            jsonAppointee = updateEmbeddedJson(jsonAppointee, "Yes", "case_details", "case_data", "informationFromAppellant");
        }

        jsonAppointee = updateEmbeddedJson(jsonAppointee, notificationEventType.getId(), "event_id");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(jsonAppointee, "/reminder"));

        assertHttpStatus(response, HttpStatus.OK);

        validateEmailNotifications(expectedEmailTemplateIds, wantedNumberOfSendEmailInvocations, expectedName);
        validateSmsNotifications(expectedSmsTemplateIds, wantedNumberOfSendSmsInvocations);
        validateLetterNotifications(expectedLetterTemplateIds, wantedNumberOfSendLetterInvocations, expectedName);
    }

    @Test
    @Parameters(method = "generateRepsNotificationScenariosForDelayedNotifications")
    public void shouldSendRepsNotificationsForADelayedNotificationEventForAnOralOrPaperHearingAndForEachSubscription(
            NotificationEventType notificationEventType, String hearingType, List<String> expectedEmailTemplateIds,
            List<String> expectedSmsTemplateIds, List<String> expectedLetterTemplateIds, String appellantEmailSubs, String appellantSmsSubs, String repsEmailSubs,
            String repsSmsSubs, int wantedNumberOfSendEmailInvocations, int wantedNumberOfSendSmsInvocations, int wantedNumberOfSendLetterInvocations) throws Exception {
        json = updateEmbeddedJson(json, hearingType, "case_details", "case_data", "appeal", "hearingType");
        json = updateEmbeddedJson(json, appellantEmailSubs, "case_details", "case_data", "subscriptions",
                "appellantSubscription", "subscribeEmail");
        json = updateEmbeddedJson(json, appellantSmsSubs, "case_details", "case_data", "subscriptions",
                "appellantSubscription", "subscribeSms");
        json = updateEmbeddedJson(json, repsEmailSubs, "case_details", "case_data", "subscriptions",
                "representativeSubscription", "subscribeEmail");
        json = updateEmbeddedJson(json, repsSmsSubs, "case_details", "case_data", "subscriptions",
                "representativeSubscription", "subscribeSms");
        json = updateEmbeddedJson(json, notificationEventType.getId(), "event_id");
        if (notificationEventType.getId().contains("Welsh")) {
            json = updateEmbeddedJson(json, "Yes", "case_details", "case_data", "languagePreferenceWelsh");
        }
        if (notificationEventType.equals(REQUEST_INFO_INCOMPLETE)) {
            json = updateEmbeddedJson(json, "Yes", "case_details", "case_data", "informationFromAppellant");
        }

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/reminder"));
        assertHttpStatus(response, HttpStatus.OK);

        String expectedName = "Harry Potter";
        validateEmailNotifications(expectedEmailTemplateIds, wantedNumberOfSendEmailInvocations, expectedName);
        validateSmsNotifications(expectedSmsTemplateIds, wantedNumberOfSendSmsInvocations);
        validateLetterNotifications(expectedLetterTemplateIds, wantedNumberOfSendLetterInvocations, expectedName);
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] generateAppointeeNotificationScenariosForDelayedNotifications() {
        return new Object[]{
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("78cf9c9c-e2b8-44d7-bcf1-220311f114cb"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                Collections.singletonList("TB-SCS-GNO-ENG-00060.doc"),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "oral",
                Collections.singletonList("78cf9c9c-e2b8-44d7-bcf1-220311f114cb"),
                Collections.singletonList("ede384aa-0b6e-4311-9f01-ee547573a07b"),
                Collections.singletonList("TB-SCS-GNO-ENG-00060.doc"),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("TB-SCS-GNO-ENG-00060.doc"),
                "no",
                "no",
                "0",
                "0",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "0",
                "0",
                "0",
                ""
            },
            new Object[]{
                DRAFT_TO_VALID_APPEAL_CREATED,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                DRAFT_TO_VALID_APPEAL_CREATED,
                "oral",
                Collections.singletonList("362d9a85-e0e4-412b-b874-020c0464e2b4"),
                Collections.singletonList("f41222ef-c05c-4682-9634-6b034a166368"),
                Collections.emptyList(),
                "yes",
                "yes",
                "1",
                "1",
                "0",
                "Appointee Appointee"
            },
            new Object[]{
                DRAFT_TO_VALID_APPEAL_CREATED,
                "oral",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "0",
                "0",
                "0",
                ""
            },
        };
    }

    private Object[] generateRepsNotificationScenariosForDelayedNotifications() {
        return new Object[]{
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "oral",
                Arrays.asList("d5fd9f65-1283-4533-a1be-10043dae7af6", "4b1ee55b-abd1-4e7e-b0ed-693d8df1e741"),
                Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b", "99bd4a56-256c-4de8-b187-d43a8dde466f"),
                Arrays.asList("TB-SCS-GNO-ENG-00060.doc", "TB-SCS-GNO-ENG-00079.doc"),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.singletonList("4b1ee55b-abd1-4e7e-b0ed-693d8df1e741"),
                Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b", "99bd4a56-256c-4de8-b187-d43a8dde466f"),
                Arrays.asList("TB-SCS-GNO-ENG-00060.doc", "TB-SCS-GNO-ENG-00079.doc"),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                APPEAL_RECEIVED_NOTIFICATION,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("TB-SCS-GNO-ENG-00060.doc", "TB-SCS-GNO-ENG-00079.doc"),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "paper",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "oral",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "paper",
                Collections.singletonList("652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                VALID_APPEAL_CREATED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
            new Object[]{
                DRAFT_TO_VALID_APPEAL_CREATED,
                "paper",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                DRAFT_TO_VALID_APPEAL_CREATED,
                "oral",
                Arrays.asList("01293b93-b23e-40a3-ad78-2c6cd01cd21c", "652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "yes",
                "yes",
                "yes",
                "yes",
                "2",
                "2",
                "0"
            },
            new Object[]{
                DRAFT_TO_VALID_APPEAL_CREATED,
                "paper",
                Collections.singletonList("652753bf-59b4-46eb-9c24-bd762338a098"),
                Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368", "a6c09fad-6265-4c7c-8b95-36245ffa5352"),
                Collections.emptyList(),
                "no",
                "yes",
                "yes",
                "yes",
                "1",
                "2",
                "0"
            },
            new Object[]{
                DRAFT_TO_VALID_APPEAL_CREATED,
                "paper",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "no",
                "no",
                "no",
                "no",
                "0",
                "0",
                "0"
            },
        };
    }
}