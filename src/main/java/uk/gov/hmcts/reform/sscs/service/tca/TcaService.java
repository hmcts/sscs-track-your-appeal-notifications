package uk.gov.hmcts.reform.sscs.service.tca;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
public class TcaService {
    private final TcaClient tcaClient;
    private final IdamService idamService;

    @Autowired
    public TcaService(TcaClient tcaClient, IdamService idamService) {
        this.tcaClient = tcaClient;
        this.idamService = idamService;
    }

    public void performAction(SscsCaseDataWrapper sscsCaseDataWrapper) {

        NotificationEventType eventType = sscsCaseDataWrapper.getNotificationEventType();

        if (NotificationEventType.CREATE_APPEAL_PDF == eventType
                || NotificationEventType.RESEND_CASE_TO_GAPS2 == eventType) {

            callTca(sscsCaseDataWrapper);
        }
    }

    private void callTca(SscsCaseDataWrapper sscsCaseDataWrapper) {
        IdamTokens idamTokens = idamService.getIdamTokens();

        tcaClient.performAction(
                idamTokens.getServiceAuthorization(),
                createWrapper(sscsCaseDataWrapper));
    }

    private Map<String, Object> createWrapper(SscsCaseDataWrapper sscsCaseDataWrapper) {
        return ImmutableMap.of(
                "case_details", buildCaseDetails(sscsCaseDataWrapper.getNewSscsCaseData()),
                "case_details_before", buildCaseDetails(sscsCaseDataWrapper.getNewSscsCaseData()),
                "event_id", sscsCaseDataWrapper.getNotificationEventType().getId());
    }

    private SscsCaseDetails buildCaseDetails(SscsCaseData caseData) {
        return SscsCaseDetails.builder()
                    .id(Long.parseLong(caseData.getCcdCaseId()))
                    .data(caseData)
                    .build();
    }
}
