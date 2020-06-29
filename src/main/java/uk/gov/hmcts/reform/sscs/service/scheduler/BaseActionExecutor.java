package uk.gov.hmcts.reform.sscs.service.scheduler;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.getNotificationById;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

public abstract class BaseActionExecutor<T> implements JobExecutor<T> {
    protected static final Logger LOG = getLogger(BaseActionExecutor.class);
    protected final NotificationService notificationService;
    protected final CcdService ccdService;
    protected final IdamService idamService;
    private final SscsCaseCallbackDeserializer deserializer;


    BaseActionExecutor(NotificationService notificationService, CcdService ccdService, IdamService idamService, SscsCaseCallbackDeserializer deserializer) {
        this.notificationService = notificationService;
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.deserializer = deserializer;
    }

    @Override
    public void execute(String jobId, String jobGroup, String eventId, T payload) {

        long caseId = getCaseId(payload);
        try {
            LOG.info("Scheduled event: {} triggered for case id: {}", eventId, caseId);

            IdamTokens idamTokens = idamService.getIdamTokens();

            SscsCaseDetails caseDetails = ccdService.getByCaseId(caseId, idamTokens);

            if (caseDetails != null) {

                //The deserialiser does things the ccd find method doesn't do. e.g. sorts collections,
                // notifications relies on events being sorted. If there are multiple hearings on the case
                // the notification should refer to the latest one.
                Callback<SscsCaseData> callback = deserializer.deserialize(buildCcdNode(caseDetails, eventId));

                SscsCaseDataWrapper wrapper = buildSscsCaseDataWrapper(
                        callback.getCaseDetails().getCaseData(),
                        null,
                        getNotificationById(eventId),
                        caseDetails.getCreatedDate(),
                        callback.getCaseDetails().getState());

                notificationService.manageNotificationAndSubscription(getWrapper(wrapper, payload));
                if (wrapper.getNotificationEventType().isReminder()) {
                    updateCase(caseId, wrapper, idamTokens);
                }
            } else {
                LOG.warn("Case id: {} could not be found for event: {}", caseId, eventId);
            }
        } catch (Exception exc) {
            LOG.error("Failed to process job [" + jobId + "] for case [" + caseId + "] and event [" + eventId + "]", exc);
        }
    }

    private String buildCcdNode(SscsCaseDetails caseDetails, String jobName) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.valueToTree(caseDetails);
        ObjectNode node2 = (ObjectNode) jsonNode;
        ObjectNode node = JsonNodeFactory.instance.objectNode();

        node2.set("case_data", jsonNode.get("data"));
        node2.remove("data");

        node.set("case_details", node2);
        node = node.put("event_id", jobName);

        return mapper.writeValueAsString(node);
    }

    private SscsCaseDataWrapper buildSscsCaseDataWrapper(SscsCaseData caseData, SscsCaseData caseDataBefore, NotificationEventType event, LocalDateTime createdDate, State state) {
        return SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseDataBefore)
                .createdDate(createdDate)
                .state(state)
                .notificationEventType(event).build();
    }

    protected abstract void updateCase(Long caseId, SscsCaseDataWrapper wrapper, IdamTokens idamTokens);

    protected abstract NotificationWrapper getWrapper(SscsCaseDataWrapper wrapper, T payload);

    protected abstract long getCaseId(T payload);
}
