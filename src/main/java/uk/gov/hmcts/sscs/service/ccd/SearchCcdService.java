package uk.gov.hmcts.sscs.service.ccd;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.config.properties.CoreCaseDataProperties;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;

@Service
@Slf4j
public class SearchCcdService {

    private final CoreCaseDataApi coreCaseDataApi;
    private final CoreCaseDataProperties coreCaseDataProperties;

    public SearchCcdService(CoreCaseDataApi coreCaseDataApi, CoreCaseDataProperties coreCaseDataProperties) {
        this.coreCaseDataApi = coreCaseDataApi;
        this.coreCaseDataProperties = coreCaseDataProperties;
    }

    public List<CaseDetails> findCaseByCaseRef(String caseRef, IdamTokens idamTokens) {
        return coreCaseDataApi.searchForCaseworker(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getAuthenticationService(),
            coreCaseDataProperties.getUserId(),
            coreCaseDataProperties.getJurisdictionId(),
            coreCaseDataProperties.getCaseTypeId(),
            ImmutableMap.of("case.caseReference", caseRef)
        );
    }

    public CaseDetails getByCaseId(String caseId, IdamTokens idamTokens) {
        log.info("Get getByCaseId...");
        return coreCaseDataApi.readForCaseWorker(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getAuthenticationService(),
                coreCaseDataProperties.getUserId(),
                coreCaseDataProperties.getJurisdictionId(),
                coreCaseDataProperties.getCaseTypeId(),
                caseId
        );
    }

}
