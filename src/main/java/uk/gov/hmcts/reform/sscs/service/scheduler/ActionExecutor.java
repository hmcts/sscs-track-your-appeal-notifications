package uk.gov.hmcts.reform.sscs.service.scheduler;

import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

@Component
public class ActionExecutor implements JobExecutor<String> {

    private static final Logger LOG = getLogger(ActionExecutor.class);

    private final NotificationService notificationService;
    private final CcdService ccdService;
    private final SscsCaseDataWrapperDeserializer deserializer;
    private final IdamService idamService;

    @Autowired
    public ActionExecutor(NotificationService notificationService,
                          CcdService ccdService,
                          SscsCaseDataWrapperDeserializer deserializer,
                          IdamService idamService) {
        this.notificationService = notificationService;
        this.ccdService = ccdService;
        this.deserializer = deserializer;
        this.idamService = idamService;
    }

    @Override
    public void execute(String jobId, String jobGroup, String eventId, String caseId) {

        LOG.info("Scheduled event: {} triggered for case id: {}", eventId, caseId);

        IdamTokens idamTokens = idamService.getIdamTokens();

        CaseDetails caseDetails = ccdService.getByCaseId(Long.valueOf(caseId), idamTokens);

        if (caseDetails != null) {
            SscsCaseDataWrapper wrapper = deserializer.buildSscsCaseDataWrapper(buildCcdNode(caseDetails, eventId));
            notificationService.createAndSendNotification(new CcdNotificationWrapper(wrapper));
            ccdService.updateCase(null, Long.valueOf(caseId), wrapper.getNotificationEventType().getId(), "CCD Case", "Notification Service updated case", idamTokens);
        } else {
            LOG.warn("Case id: {} could not be found for event: {}", caseId, eventId);
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
