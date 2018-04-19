package uk.gov.hmcts.sscs.scheduler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.sscs.scheduler.SampleData.validJob;

import java.sql.Date;
import org.junit.Test;
import org.quartz.SimpleScheduleBuilder;
import uk.gov.hmcts.sscs.scheduler.jobs.JobDataKeys;
import uk.gov.hmcts.sscs.scheduler.model.Trigger;

public class TriggerConverterTest {

    @Test
    public void should_use_default_scheduler_builder_and_convert_triggers_correctly() {
        Trigger originalTrigger = validJob().trigger;
        org.quartz.Trigger trigger = TriggerConverter.toQuartzTrigger(originalTrigger);

        // dummy assertion to remind in the future of different schedulers in use and verify
        assertThat(trigger.getScheduleBuilder()).isOfAnyClassIn(SimpleScheduleBuilder.class);
        assertThat(trigger.getJobDataMap().getIntValue(JobDataKeys.ATTEMPT)).isEqualTo(1);
        assertThat(trigger.getStartTime()).isEqualTo(Date.from(originalTrigger.startDateTime.toInstant()));

        Trigger rebuiltTrigger = TriggerConverter.toPlatformTrigger(trigger);

        assertThat(rebuiltTrigger).isEqualToComparingFieldByFieldRecursively(originalTrigger);
    }
}
