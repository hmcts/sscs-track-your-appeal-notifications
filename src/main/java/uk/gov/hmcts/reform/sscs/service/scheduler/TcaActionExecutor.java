package uk.gov.hmcts.reform.sscs.service.scheduler;

import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.service.tca.TcaService;

public class TcaActionExecutor implements JobExecutor<TcaJobPayload> {
    private static final Logger LOG = getLogger(TcaActionExecutor.class);
    private final TcaService tcaService;
    private final CcdService ccdService;
    private final SscsCaseDataWrapperDeserializer deserializer;
    private final IdamService idamService;

    public TcaActionExecutor(TcaService tcaService, CcdService ccdService, SscsCaseDataWrapperDeserializer deserializer, IdamService idamService) {
        this.tcaService = tcaService;
        this.ccdService = ccdService;
        this.deserializer = deserializer;
        this.idamService = idamService;
    }

    @Override
    public void execute(String jobId, String jobGroup, String eventId, TcaJobPayload payload) {

        long caseId = payload.getCaseId();
        LOG.info("Scheduled event: {} triggered for case id: {}", eventId, caseId);

        IdamTokens idamTokens = idamService.getIdamTokens();

        SscsCaseDetails caseDetails = ccdService.getByCaseId(caseId, idamTokens);

        if (caseDetails != null) {
            SscsCaseDataWrapper wrapper = deserializer.buildSscsCaseDataWrapper(buildCcdNode(caseDetails, eventId));

            tcaService.performAction(wrapper);
        } else {
            LOG.warn("Case id: {} could not be found for event: {}", caseId, eventId);
        }
    }

    private ObjectNode buildCcdNode(SscsCaseDetails caseDetails, String eventId) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();

        node.set("case_details", new ObjectMapper().valueToTree(caseDetails));
        node.put("event_id", eventId);

        return node;
    }
}
