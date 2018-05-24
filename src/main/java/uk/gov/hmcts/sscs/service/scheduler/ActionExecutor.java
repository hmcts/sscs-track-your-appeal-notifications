package uk.gov.hmcts.sscs.service.scheduler;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;
import uk.gov.hmcts.sscs.service.NotificationService;
import uk.gov.hmcts.sscs.service.ccd.CcdUtil;
import uk.gov.hmcts.sscs.service.ccd.SearchCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@Component
public class ActionExecutor implements JobExecutor<String> {

    private final NotificationService notificationService;
    private final SearchCcdService searchCcdService;
    private final IdamService idamService;

    @Autowired
    public ActionExecutor(NotificationService notificationService,
                          SearchCcdService searchCcdService, IdamService idamService) {
        this.notificationService = notificationService;
        this.searchCcdService = searchCcdService;
        this.idamService = idamService;
    }

    @Override
    public void execute(String jobId, String caseId) {

        IdamTokens idamTokens = IdamTokens.builder()
                .idamOauth2Token(idamService.getIdamOauth2Token())
                .authenticationService(idamService.generateServiceAuthorization())
                .build();

        List<CaseDetails> caseDetails = searchCcdService.findCaseByCaseRef(caseId, idamTokens);

        if (!caseDetails.isEmpty()) {
            CcdResponse ccdResponse = CcdUtil.getCcdResponse(caseDetails.get(0));

            CcdResponseWrapper wrapper = CcdResponseWrapper.builder().newCcdResponse(ccdResponse).build();

            notificationService.createAndSendNotification(wrapper);
        }
    }
}

