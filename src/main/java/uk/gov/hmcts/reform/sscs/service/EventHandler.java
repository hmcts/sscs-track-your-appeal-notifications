package uk.gov.hmcts.reform.sscs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.service.reminder.JobGroupGenerator;

@Service
public class EventHandler {

    private final EventCalculator eventCalculator;
    private final JobScheduler jobScheduler;
    private final JobGroupGenerator jobGroupGenerator;

    @Autowired
    public EventHandler(EventCalculator eventCalculator, JobScheduler jobScheduler, JobGroupGenerator jobGroupGenerator) {
        this.eventCalculator = eventCalculator;
        this.jobScheduler = jobScheduler;
        this.jobGroupGenerator = jobGroupGenerator;
    }

    public void scheduleEvent(NotificationWrapper wrapper) {
        final String caseId = wrapper.getCaseId();
        String eventId = wrapper.getNotificationType().getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        jobScheduler.schedule(new Job<>(
                jobGroup,
                eventId,
                wrapper.getSchedulerPayload(),
                eventCalculator.getEventStart(wrapper.getNotificationType())
        ));
    }

}
