package uk.gov.hmcts.sscs.service.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.sscs.deserialize.CcdResponseDeserializer;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;
import uk.gov.hmcts.sscs.domain.notify.EventType;
import uk.gov.hmcts.sscs.service.NotificationService;
import uk.gov.hmcts.sscs.service.ccd.SearchCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@Component
public class ActionExecutor implements JobExecutor<String> {

    private final NotificationService notificationService;
    private final SearchCcdService searchCcdService;
    private final IdamService idamService;
    private final CcdResponseDeserializer deserializer;

    @Autowired
    public ActionExecutor(NotificationService notificationService,
                          SearchCcdService searchCcdService, IdamService idamService, CcdResponseDeserializer deserializer) {
        this.notificationService = notificationService;
        this.searchCcdService = searchCcdService;
        this.idamService = idamService;
        this.deserializer = deserializer;
    }

    @Override
    public void execute(String jobId, String jobName, String caseId) {

        IdamTokens idamTokens = IdamTokens.builder()
                .idamOauth2Token(idamService.getIdamOauth2Token())
                .authenticationService(idamService.generateServiceAuthorization())
                .build();

        CaseDetails caseDetails = searchCcdService.getByCaseId(caseId, idamTokens);

        if (caseDetails != null) {
            CcdResponse ccdResponse = deserializer.buildCcdResponseWrapper(buildCcdNode(caseDetails));
            ccdResponse.setNotificationType(EventType.getNotificationById(jobName));

            CcdResponseWrapper wrapper = CcdResponseWrapper.builder().newCcdResponse(ccdResponse).build();

            notificationService.createAndSendNotification(wrapper);
        }
    }

    private JsonNode buildCcdNode(CaseDetails caseDetails) {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.valueToTree(caseDetails);
        ObjectNode node = JsonNodeFactory.instance.objectNode();

        node.set("case_details", jsonNode);

        return node;
    }
}

