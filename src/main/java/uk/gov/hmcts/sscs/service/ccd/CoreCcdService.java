package uk.gov.hmcts.sscs.service.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.sscs.config.properties.CoreCaseDataProperties;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.models.idam.IdamTokens;

@Service
public class CoreCcdService {

    private final CoreCaseDataApi coreCaseDataApi;
    private final CoreCaseDataProperties coreCaseDataProperties;

    @Autowired
    public CoreCcdService(CoreCaseDataApi coreCaseDataApi, CoreCaseDataProperties coreCaseDataProperties) {
        this.coreCaseDataApi = coreCaseDataApi;
        this.coreCaseDataProperties = coreCaseDataProperties;
    }

    public StartEventResponse startCase(String serviceAuthorization, String idamOauth2Token, String eventType) {
        return coreCaseDataApi.startForCaseworker(
            idamOauth2Token,
            serviceAuthorization,
            coreCaseDataProperties.getUserId(),
            coreCaseDataProperties.getJurisdictionId(),
            coreCaseDataProperties.getCaseTypeId(),
            eventType);
    }

    public StartEventResponse startEvent(String serviceAuthorization, String idamOauth2Token, String caseId,
                                         String eventType) {
        return coreCaseDataApi.startEventForCaseWorker(
            idamOauth2Token,
            serviceAuthorization,
            coreCaseDataProperties.getUserId(),
            coreCaseDataProperties.getJurisdictionId(),
            coreCaseDataProperties.getCaseTypeId(),
            caseId,
            eventType);
    }

    public CaseDetails submitForCaseworker(CcdResponse caseData, IdamTokens idamTokens, StartEventResponse startEventResponse) {
        CaseDataContent caseDataContent = CaseDataContent.builder()
                .eventToken(startEventResponse.getToken())
                .event(Event.builder()
                        .id(startEventResponse.getEventId())
                        .summary("GAPS2 Case")
                        .description("CaseLoader Case created")
                        .build())
                .data(caseData)
                .build();
        return coreCaseDataApi.submitForCaseworker(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getAuthenticationService(),
                coreCaseDataProperties.getUserId(),
                coreCaseDataProperties.getJurisdictionId(),
                coreCaseDataProperties.getCaseTypeId(),
                true,
                caseDataContent);
    }

    public CaseDetails submitEventForCaseworker(CcdResponse caseData, Long caseId, IdamTokens idamTokens, StartEventResponse startEventResponse) {
        CaseDataContent caseDataContent = CaseDataContent.builder()
                .eventToken(startEventResponse.getToken())
                .event(Event.builder()
                        .id(startEventResponse.getEventId())
                        .summary("GAPS2 Case")
                        .description("CaseLoader Case updated")
                        .build())
                .data(caseData)
                .build();
        return coreCaseDataApi.submitEventForCaseWorker(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getAuthenticationService(),
                coreCaseDataProperties.getUserId(),
                coreCaseDataProperties.getJurisdictionId(),
                coreCaseDataProperties.getCaseTypeId(),
                caseId.toString(),
                true,
                caseDataContent);
    }

}
