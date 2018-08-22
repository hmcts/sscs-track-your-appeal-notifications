package uk.gov.hmcts.sscs.controller;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.deserialize.CcdResponseWrapperDeserializer;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.CohEvent;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;
import uk.gov.hmcts.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.sscs.service.AuthorisationService;
import uk.gov.hmcts.sscs.service.NotificationService;
import uk.gov.hmcts.sscs.service.ccd.SearchCcdService;
import uk.gov.hmcts.sscs.service.idam.IdamTokensService;

@RestController
public class NotificationController {

    private static final org.slf4j.Logger LOG = getLogger(NotificationController.class);

    private final NotificationService service;
    private final AuthorisationService authorisationService;
    private final SearchCcdService searchCcdService;
    private final IdamTokensService idamTokensService;
    private final CcdResponseWrapperDeserializer deserializer;

    @Autowired
    public NotificationController(NotificationService service, AuthorisationService authorisationService, SearchCcdService searchCcdService, IdamTokensService idamTokensService, CcdResponseWrapperDeserializer deserializer) {
        this.service = service;
        this.authorisationService = authorisationService;
        this.searchCcdService = searchCcdService;
        this.idamTokensService = idamTokensService;
        this.deserializer = deserializer;
    }

    @RequestMapping(value = "/send", method = POST, produces = APPLICATION_JSON_VALUE)
    public void sendNotification(
            @RequestHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody CcdResponseWrapper ccdResponseWrapper) {
        LOG.info("Ccd Response received for case id: {}", ccdResponseWrapper.getNewCcdResponse().getCaseId());

        authorisationService.authorise(serviceAuthHeader);
        service.createAndSendNotification(new CcdNotificationWrapper(ccdResponseWrapper));
    }

    @RequestMapping(value = "/coh-send", method = POST, produces = APPLICATION_JSON_VALUE)
    public void sendCohNotification(
            @RequestHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody CohEvent cohEvent) {
        String caseId = cohEvent.getCaseId();
        LOG.info("Coh Response received for case id: {}", caseId);

        IdamTokens idamTokens = idamTokensService.getIdamTokens();

        CaseDetails caseDetails = searchCcdService.getByCaseId(caseId, idamTokens);

        String eventId = cohEvent.getEventType();
        if (caseDetails != null) {
            CcdResponseWrapper wrapper = deserializer.buildCcdResponseWrapper(buildCcdNode(caseDetails, eventId));
            service.createAndSendNotification(new CohNotificationWrapper(idamTokens, cohEvent.getOnlineHearingId(), wrapper));
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
