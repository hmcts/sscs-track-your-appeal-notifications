package uk.gov.hmcts.sscs.service.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;

@Component
public class CreateCcdService {

    private final CoreCcdService coreCcdService;

    @Autowired
    CreateCcdService(CoreCcdService coreCcdService) {
        this.coreCcdService = coreCcdService;
    }

    public CaseDetails create(CcdResponse caseData, IdamTokens idamTokens) {
        StartEventResponse startEventResponse = coreCcdService.startCase(idamTokens, "appealCreated");
        return coreCcdService.submitForCaseworker(caseData, idamTokens, startEventResponse);
    }

}
