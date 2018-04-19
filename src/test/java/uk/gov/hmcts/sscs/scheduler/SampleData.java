package uk.gov.hmcts.sscs.scheduler;

import com.google.common.collect.ImmutableMap;
import java.time.ZonedDateTime;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.sscs.scheduler.model.HttpAction;
import uk.gov.hmcts.sscs.scheduler.model.Job;
import uk.gov.hmcts.sscs.scheduler.model.Trigger;

public final class SampleData {

    private SampleData() {
    }

    public static String jobJson() {
        return jobJson("https://my-cool-service.gov.uk/do-something");
    }

    public static String jobJson(String targetUrl) {
        return new JSONObject()
            .put("name", "my-job-name")
            .put("action", new JSONObject()
                .put("url", targetUrl)
                .put("method", "POST")
                .put("headers", new JSONObject()
                    .put("Authorization", "some-auth-token")
                )
                .put("body", "hello")
            )
            .put("trigger", new JSONObject()
                .put("start_date_time", "2042-08-11T12:11:00Z")
            )
            .toString();
    }

    public static Job validJob() {
        return new Job(
            "my-job-name",
            new HttpAction(
                "https://not-existing-service.gov.uk/do-stuff",
                HttpMethod.POST,
                ImmutableMap.of("Authorization", "token-goes-here"),
                null
            ),
            new Trigger(
                ZonedDateTime.now()
            )
        );
    }
}
