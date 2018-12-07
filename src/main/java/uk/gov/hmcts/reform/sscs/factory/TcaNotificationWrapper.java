package uk.gov.hmcts.reform.sscs.factory;

import static java.lang.Long.valueOf;

import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.service.scheduler.TcaActionSerializer;
import uk.gov.hmcts.reform.sscs.service.scheduler.TcaJobPayload;

public class TcaNotificationWrapper extends CcdNotificationWrapper {

    public TcaNotificationWrapper(SscsCaseDataWrapper responseWrapper) {

        super(responseWrapper);
    }

    @Override
    public String getSchedulerPayload() {
        return new TcaActionSerializer().serialize(new TcaJobPayload(valueOf(getCaseId())));
    }

}
