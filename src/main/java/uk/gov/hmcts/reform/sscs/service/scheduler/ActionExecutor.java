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
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

@Component
public class ActionExecutor implements JobExecutor<String> {

    private static final Logger LOG = getLogger(ActionExecutor.class);

    private final NotificationService notificationService;
    private final CcdClient ccdClient;
    private final SscsCaseDataWrapperDeserializer deserializer;

    @Autowired
    public ActionExecutor(NotificationService notificationService,
                          CcdClient ccdClient,
                          SscsCaseDataWrapperDeserializer deserializer) {
        this.notificationService = notificationService;
        this.ccdClient = ccdClient;
        this.deserializer = deserializer;
    }

    @Override
    public void execute(String jobId, String jobGroup, String eventId, String caseId) {

        LOG.info("Scheduled event: {} triggered for case id: {}", eventId, caseId);

        CaseDetails caseDetails = ccdClient.getByCaseId(caseId);

        if (caseDetails != null) {
            SscsCaseDataWrapper wrapper = deserializer.buildSscsCaseDataWrapper(buildCcdNode(caseDetails, eventId));
            notificationService.createAndSendNotification(new CcdNotificationWrapper(wrapper));
            ccdClient.updateCase(null, Long.valueOf(caseId), wrapper.getNewSscsCaseData().getNotificationType().getCcdType(), "CCD Case", "Notification Service updated case");
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
