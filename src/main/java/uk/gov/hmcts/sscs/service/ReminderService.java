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

    public void createJob(CcdResponse ccdResponse) {
        Reminder reminder = new Reminder(ccdResponse, callbackUrl);

        JSONObject result = new JSONObject(reminder);
        client.post(result, "jobs");
    }
}
