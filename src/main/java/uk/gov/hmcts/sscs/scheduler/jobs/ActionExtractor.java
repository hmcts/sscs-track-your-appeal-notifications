package uk.gov.hmcts.sscs.scheduler.jobs;

import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.scheduler.model.HttpAction;
import uk.gov.hmcts.sscs.scheduler.serialize.ActionSerializer;

@Component
public class ActionExtractor {
    private final ActionSerializer actionSerializer;

    public ActionExtractor(ActionSerializer actionSerializer) {
        this.actionSerializer = actionSerializer;
    }

    /**
     * Extracts action model from job execution context.
     */
    public HttpAction extract(JobExecutionContext ctx) {
        String json = ctx.getJobDetail().getJobDataMap().getString(JobDataKeys.PARAMS);
        return actionSerializer.deserialize(json);
    }
}
