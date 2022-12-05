package uk.gov.hmcts.reform.sscs.service.quartz;

import static uk.gov.hmcts.reform.sscs.model.jobs.JobDataKeys.PAYLOAD;

import java.time.Instant;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.model.jobs.JobMapper;
import uk.gov.hmcts.reform.sscs.model.jobs.JobMapping;

@Component
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class QuartzExecutionHandler implements Job {

    private static final Logger log = LoggerFactory.getLogger(QuartzExecutionHandler.class);

    private final JobMapper jobMapper;

    public QuartzExecutionHandler(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDetail jobDetail = context.getJobDetail();
        String jobId = jobDetail.getKey().getName();
        String jobGroup = jobDetail.getKey().getGroup();
        String jobName = jobDetail.getDescription();

        log.info("Executing job {}", jobId);

        try {

            Instant jobStart = Instant.now();

            String payloadSource = "";

            if (jobDetail.getJobDataMap().containsKey(PAYLOAD.getKey())) {

                payloadSource =
                    jobDetail
                        .getJobDataMap()
                        .getString(PAYLOAD.getKey());
            }

            JobMapping jobMapping = jobMapper.getJobMapping(payloadSource);
            jobMapping.execute(jobId, jobGroup, jobName, payloadSource);

            log.info(
                "Job {} executed in {}ms.",
                jobId, (Instant.now().toEpochMilli() - jobStart.toEpochMilli())
            );

        } catch (Exception e) {

            String errorMessage = String.format("Job failed. Job ID: %s", jobId);
            log.error(errorMessage, e);

            throw new JobExecutionException(errorMessage, e);
        }
    }

}
