package uk.gov.hmcts.sscs.service.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.sscs.deserialize.CcdResponseWrapperDeserializer;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;
import uk.gov.hmcts.sscs.service.NotificationService;
import uk.gov.hmcts.sscs.service.ccd.SearchCcdService;
import uk.gov.hmcts.sscs.service.ccd.UpdateCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@Component
public class ActionExecutor implements JobExecutor<String> {

    private final NotificationService notificationService;
    private final SearchCcdService searchCcdService;
    private final UpdateCcdService updateCcdService;
    private final IdamService idamService;
    private final CcdResponseWrapperDeserializer deserializer;

    @Autowired
    public ActionExecutor(NotificationService notificationService,
                          SearchCcdService searchCcdService, UpdateCcdService updateCcdService,
                          IdamService idamService, CcdResponseWrapperDeserializer deserializer) {
        this.notificationService = notificationService;
        this.searchCcdService = searchCcdService;
        this.updateCcdService = updateCcdService;
        this.idamService = idamService;
        this.deserializer = deserializer;
    }

    @Override
    public void execute(String jobId, String jobGroup, String jobName, String caseId) {

        IdamTokens idamTokens = IdamTokens.builder()
                .idamOauth2Token(idamService.getIdamOauth2Token())
                .authenticationService(idamService.generateServiceAuthorization())
                .build();

        CaseDetails caseDetails = searchCcdService.getByCaseId(caseId, idamTokens);

        if (caseDetails != null) {
            CcdResponseWrapper wrapper = deserializer.buildCcdResponseWrapper(buildCcdNode(caseDetails, jobName));

            notificationService.createAndSendNotification(wrapper);

            updateCcdService.update(null, Long.valueOf(caseId), wrapper.getNewCcdResponse().getNotificationType().getId(), idamTokens);
        }
    }

    private ObjectNode buildCcdNode(CaseDetails caseDetails, String jobName) {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.valueToTree(caseDetails);
        ObjectNode node = JsonNodeFactory.instance.objectNode();

        node.set("case_details", jsonNode);
        node = node.put("event_id", jobName);

        return node;
    }
}

