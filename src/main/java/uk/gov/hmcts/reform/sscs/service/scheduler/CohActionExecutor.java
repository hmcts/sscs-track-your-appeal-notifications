package uk.gov.hmcts.reform.sscs.service.scheduler;

import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.NotificationService;
import uk.gov.hmcts.reform.sscs.service.RetryNotificationService;

public class CohActionExecutor extends BaseActionExecutor<CohJobPayload> {

    private final int maxRetry;

    public CohActionExecutor(NotificationService notificationService,
                             RetryNotificationService retryNotificationService,
                             int maxRetry,
                             CcdService ccdService,
                             IdamService idamService,
                             SscsCaseCallbackDeserializer deserializer) {
        super(notificationService, retryNotificationService, ccdService, idamService, deserializer);
        this.maxRetry = maxRetry;
    }

    @Override
    protected void updateCase(Long caseId, SscsCaseDataWrapper wrapper, IdamTokens idamTokens) {
        // Void
    }

    @Override
    protected NotificationWrapper getWrapper(SscsCaseDataWrapper wrapper, CohJobPayload payload) {
        return new CohNotificationWrapper(payload.getOnlineHearingId(), wrapper);
    }

    @Override
    protected long getCaseId(CohJobPayload payload) {
        return payload.getCaseId();
    }

    @Override
    protected int getRetry(CohJobPayload payload) {
        return maxRetry;
    }

}
