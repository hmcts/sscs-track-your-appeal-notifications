package uk.gov.hmcts.reform.sscs.service.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.properties.CoreCaseDataProperties;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
public class CoreCcdService {

    private final CoreCaseDataApi coreCaseDataApi;
    private final CoreCaseDataProperties coreCaseDataProperties;

    @Autowired
    public CoreCcdService(CoreCaseDataApi coreCaseDataApi, CoreCaseDataProperties coreCaseDataProperties) {
        this.coreCaseDataApi = coreCaseDataApi;
        this.coreCaseDataProperties = coreCaseDataProperties;
    }

    public StartEventResponse startCase(IdamTokens idamTokens, String eventType) {
        return coreCaseDataApi.startForCaseworker(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            coreCaseDataProperties.getJurisdictionId(),
            coreCaseDataProperties.getCaseTypeId(),
            eventType);
    }

    public StartEventResponse startEvent(IdamTokens idamTokens, String caseId, String eventType) {
        return coreCaseDataApi.startEventForCaseWorker(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            coreCaseDataProperties.getJurisdictionId(),
            coreCaseDataProperties.getCaseTypeId(),
            caseId,
            eventType);
    }

    public CaseDetails submitForCaseworker(SscsCaseData ccdResponse, IdamTokens idamTokens, StartEventResponse startEventResponse) {

        CaseDataContent caseDataContent = buildCaseDataContent(ccdResponse, startEventResponse, "Notification Service created case");

        return coreCaseDataApi.submitForCaseworker(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            coreCaseDataProperties.getJurisdictionId(),
            coreCaseDataProperties.getCaseTypeId(),
            true,
            caseDataContent);
    }

    public CaseDetails submitEventForCaseworker(SscsCaseData ccdResponse, Long caseId, IdamTokens idamTokens, StartEventResponse startEventResponse) {

        CaseDataContent caseDataContent = buildCaseDataContent(ccdResponse, startEventResponse, "Notification Service updated case");

        return coreCaseDataApi.submitEventForCaseWorker(
            idamTokens.getIdamOauth2Token(),
            idamTokens.getServiceAuthorization(),
            idamTokens.getUserId(),
            coreCaseDataProperties.getJurisdictionId(),
            coreCaseDataProperties.getCaseTypeId(),
            caseId.toString(),
            true,
            caseDataContent);
    }

    private CaseDataContent buildCaseDataContent(SscsCaseData ccdResponse,
                                                 StartEventResponse startEventResponse,
                                                 String description) {
        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder()
                .id(startEventResponse.getEventId())
                .summary("CCD Case")
                .description(description)
                .build())
            .data(ccdResponse)
            .build();
    }

}
