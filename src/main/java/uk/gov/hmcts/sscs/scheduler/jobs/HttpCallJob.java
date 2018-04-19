package uk.gov.hmcts.sscs.scheduler.jobs;

import java.time.Instant;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.sscs.scheduler.model.HttpAction;

@Component
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class HttpCallJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(HttpCallJob.class);

    private final RestTemplate restTemplate;
    private final ActionExtractor actionExtractor;

    public HttpCallJob(
        RestTemplate restTemplate,
        ActionExtractor actionExtractor
    ) {
        this.restTemplate = restTemplate;
        this.actionExtractor = actionExtractor;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String jobId = context.getJobDetail().getKey().getName();

        logger.info("Executing job {}", jobId);

        HttpAction action = null;
        Instant start = Instant.now();

        try {
            action = actionExtractor.extract(context);

            ResponseEntity<String> response =
                restTemplate
                    .exchange(
                        action.url,
                        action.method,
                        toHttpEntity(action),
                        String.class
                    );

            logger.info("Job {} executed. Response code: {}", jobId, response.getStatusCodeValue());
        } catch (Exception e) {
            String errorMessage = String.format("Job failed. Job ID: %s", jobId);
            logger.error(errorMessage, e);

            throw new JobExecutionException(errorMessage, e);
        }
    }

    private static HttpEntity<String> toHttpEntity(HttpAction action) {
        HttpHeaders httpHeaders = new HttpHeaders();
        action.headers.forEach(httpHeaders::add);

        return new HttpEntity<>(action.body, httpHeaders);
    }

}
