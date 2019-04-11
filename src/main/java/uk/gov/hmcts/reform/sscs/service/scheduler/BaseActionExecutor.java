package uk.gov.hmcts.reform.sscs.service.scheduler;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.getNotificationById;

import org.slf4j.Logger;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

public abstract class BaseActionExecutor<T> implements JobExecutor<T> {
    protected static final Logger LOG = getLogger(BaseActionExecutor.class);
    protected final NotificationService notificationService;
    protected final CcdService ccdService;
    protected final IdamService idamService;

    BaseActionExecutor(NotificationService notificationService, CcdService ccdService, IdamService idamService) {
        this.notificationService = notificationService;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public void execute(String jobId, String jobGroup, String eventId, T payload) {

        long caseId = getCaseId(payload);
        try {
            LOG.info("Scheduled event: {} triggered for case id: {}", eventId, caseId);

            IdamTokens idamTokens = idamService.getIdamTokens();

            SscsCaseDetails caseDetails = ccdService.getByCaseId(caseId, idamTokens);

            if (caseDetails != null) {
                SscsCaseDataWrapper wrapper = buildSscsCaseDataWrapper(caseDetails.getData(), null, getNotificationById(eventId));

                notificationService.manageNotificationAndSubscription(getWrapper(wrapper, payload));
                if (wrapper.getNotificationEventType().isReminder()) {
                    updateCase(caseId, wrapper, idamTokens);
                }
            } else {
                LOG.warn("Case id: {} could not be found for event: {}", caseId, eventId);
            }
        } catch (Exception exc) {
            LOG.error("Failed to process job [" + jobId + "] for case [" + caseId + "] and event [" + eventId + "]", exc);
            throw exc;
        }
    }

    private SscsCaseDataWrapper buildSscsCaseDataWrapper(SscsCaseData caseData, SscsCaseData caseDataBefore, NotificationEventType event) {
        return SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseDataBefore)
                .notificationEventType(event).build();
    }

    protected abstract void updateCase(Long caseId, SscsCaseDataWrapper wrapper, IdamTokens idamTokens);

    protected abstract NotificationWrapper getWrapper(SscsCaseDataWrapper wrapper, T payload);

    protected abstract long getCaseId(T payload);
}
