package uk.gov.hmcts.sscs.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.client.RestClient;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.reminder.Reminder;

@Service
public class ReminderService {

    private final RestClient client;

    @Value("${job.scheduler.callbackUrl}")
    private String callbackUrl;

    @Autowired
    public ReminderService(RestClient client) {
        this.client = client;
    }

    public void createJob(CcdResponse ccdResponse) throws Exception {
        Reminder reminder = new Reminder(ccdResponse, callbackUrl);

//        JSONObject expectedJson = new JSONObject();
//        expectedJson.put("name", "SSCS_evidenceReminder");
//        JSONObject actionJson = new JSONObject();
//        actionJson.put("url", "test");
//        actionJson.put("method", "POST");
//        JSONObject bodyJson = new JSONObject();
//        bodyJson.put("appealNumber", "123456");
//        bodyJson.put("reminderType", "evidenceReminder");
//        actionJson.put("body", bodyJson.toString());
//        expectedJson.put("action", actionJson);
//        JSONObject triggerJson = new JSONObject();
//        triggerJson.put("dateTime", "2018-04-03T00:00:00+01:00");
//        expectedJson.put("trigger", triggerJson);

        JSONObject result = new JSONObject(reminder);
        client.post(result, "jobs");
    }
}
