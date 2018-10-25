package uk.gov.hmcts.reform.sscs.service.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

public class CohActionExecutor extends BaseActionExecutor<CohJobPayload> {

    @Autowired
    public CohActionExecutor(NotificationService notificationService,
                             CcdService ccdService,
                             SscsCaseDataWrapperDeserializer deserializer,
                             IdamService idamService,
                             Environment environment) {
        super(notificationService, ccdService, deserializer, idamService, environment);
    }

    @Override
    protected void updateCase(Long caseId, SscsCaseDataWrapper wrapper, IdamTokens idamTokens) {
    }

    @Override
    protected NotificationWrapper getWrapper(SscsCaseDataWrapper wrapper, CohJobPayload payload) {
        return new CohNotificationWrapper(payload.getOnlineHearingId(), wrapper);
    }

    @Override
    protected long getCaseId(CohJobPayload payload) {
        return payload.getCaseId();
    }

}
