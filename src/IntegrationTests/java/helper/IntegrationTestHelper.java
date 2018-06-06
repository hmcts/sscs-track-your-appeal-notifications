package helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.sscs.service.AuthorisationService;

public class IntegrationTestHelper {

    private IntegrationTestHelper() {

    }

    public static MockHttpServletRequestBuilder getRequestWithAuthHeader(String json) {

        return getRequestWithoutAuthHeader(json)
            .header(AuthorisationService.SERVICE_AUTHORISATION_HEADER, "some-auth-header");
    }

    public static MockHttpServletRequestBuilder getRequestWithoutAuthHeader(String json) {

        return post("/send")
            .contentType(APPLICATION_JSON)
            .content(json);
    }

    public static void assertHttpStatus(HttpServletResponse response, HttpStatus status) {
        assertThat(response.getStatus()).isEqualTo(status.value());
    }

    public static String updateEmbeddedJson(String json, String value, String... keys) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map map = objectMapper.readValue(json, Map.class);
        Map t = map;
        for (int i = 0; i < keys.length - 1; i++) {
            t = (Map) t.get(keys[i]);
        }

        t.put(keys[keys.length - 1], value);

        return objectMapper.writeValueAsString(map);
    }

    public static void assertScheduledJobCount(
        Scheduler quartzScheduler,
        String message,
        int expectedValue
    ) {
        try {

            int scheduledJobCount =
                quartzScheduler
                    .getJobKeys(GroupMatcher.anyGroup())
                    .size();

            assertTrue(
                message + " (" + expectedValue + " != " + expectedValue + ")",
                scheduledJobCount == expectedValue
            );

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertScheduledJobCount(
        Scheduler quartzScheduler,
        String message,
        String groupMatch,
        int expectedValue
    ) {
        try {

            int scheduledJobCount =
                quartzScheduler
                    .getJobKeys(GroupMatcher.jobGroupContains(groupMatch))
                    .size();

            assertTrue(
                message + " -- " + expectedValue + " != " + scheduledJobCount,
                scheduledJobCount == expectedValue
            );

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertScheduledJobTriggerAt(
        Scheduler quartzScheduler,
        String message,
        String groupMatch,
        String expectedTriggerAt
    ) {
        try {

            Set<JobKey> jobKeys =
                quartzScheduler.getJobKeys(GroupMatcher.jobGroupContains(groupMatch));

            if (jobKeys.isEmpty()) {
                assertTrue(message + " -- job group match not found", false);
            }

            List<String> triggersAt =
                quartzScheduler
                    .getJobKeys(GroupMatcher.jobGroupContains(groupMatch))
                    .stream()
                    .flatMap(jobKey -> {
                        try {
                            return quartzScheduler.getTriggersOfJob(jobKey).stream();
                        } catch (SchedulerException ignore) {
                            return Collections.<Trigger>emptyList().stream();
                        }
                    })
                    .map(trigger -> trigger.getStartTime().toInstant().toString())
                    .collect(Collectors.toList());

            assertTrue(
                message + " -- " + expectedTriggerAt + " not found in collection [" + String.join(", ", triggersAt) + "]",
                triggersAt.contains(expectedTriggerAt)
            );

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

}
