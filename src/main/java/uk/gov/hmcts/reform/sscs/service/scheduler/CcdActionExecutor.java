package uk.gov.hmcts.reform.sscs.service.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

public class CcdActionExecutor extends BaseActionExecutor<String> {

    @Autowired
    public CcdActionExecutor(NotificationService notificationService,
                             CcdService ccdService,
                             SscsCaseDataWrapperDeserializer deserializer,
                             IdamService idamService) {
        super(notificationService, ccdService, deserializer, idamService);
    }

    @Override
    protected NotificationWrapper getWrapper(SscsCaseDataWrapper wrapper, String payload) {
        return new CcdNotificationWrapper(wrapper);
    }

    @Override
    protected long getCaseId(String payload) {
        return Long.valueOf(payload);
    }
}
