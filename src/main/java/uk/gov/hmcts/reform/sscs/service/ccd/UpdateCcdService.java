package uk.gov.hmcts.reform.sscs.service.ccd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class UpdateCcdService {

    private final CoreCcdService coreCcdService;

    @Autowired
    public UpdateCcdService(CoreCcdService coreCcdService) {
        this.coreCcdService = coreCcdService;
    }

    public CaseDetails update(SscsCaseData caseData, Long caseId, String eventType, IdamTokens idamTokens) {
        StartEventResponse startEventResponse = coreCcdService.startEvent(idamTokens, caseId.toString(), eventType);
        return coreCcdService.submitEventForCaseworker(caseData, caseId, idamTokens, startEventResponse);
    }

}
