package uk.gov.hmcts.sscs.service.ccd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.models.idam.IdamTokens;

@Service
@Slf4j
public class UpdateCcdService {

    private final CoreCcdService coreCcdService;

    @Autowired
    public UpdateCcdService(CoreCcdService coreCcdService) {
        this.coreCcdService = coreCcdService;
    }

    public CaseDetails update(CcdResponse caseData, Long caseId, String eventType, IdamTokens idamTokens) {
        StartEventResponse startEventResponse = coreCcdService.startEvent(idamTokens.getAuthenticationService(),
            idamTokens.getIdamOauth2Token(), caseId.toString(), eventType);
        return coreCcdService.submitEventForCaseworker(caseData, caseId, idamTokens, startEventResponse);
    }

}
