package uk.gov.hmcts.sscs.service.scheduler;

import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
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

    private static final Logger LOG = getLogger(ActionExecutor.class);

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
    public void execute(String jobId, String jobGroup, String eventId, String caseId) {

        LOG.info("Scheduled event: {} triggered for case: {}", eventId, caseId);

        String oauth2Token = idamService.getIdamOauth2Token();
        IdamTokens idamTokens = IdamTokens.builder()
            .idamOauth2Token(oauth2Token)
            .serviceAuthorization(idamService.generateServiceAuthorization())
            .userId(idamService.getUserId(oauth2Token))
            .build();

        CaseDetails caseDetails = searchCcdService.getByCaseId(caseId, idamTokens);

        if (caseDetails != null) {
            CcdResponseWrapper wrapper = deserializer.buildCcdResponseWrapper(buildCcdNode(caseDetails, eventId));
            notificationService.createAndSendNotification(wrapper);
            updateCcdService.update(null, Long.valueOf(caseId), wrapper.getNewCcdResponse().getNotificationType().getId(), idamTokens);
        } else {
            LOG.warn("Case: {} could not be found for event: {}", caseId, eventId);
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
