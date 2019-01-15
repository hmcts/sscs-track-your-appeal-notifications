package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.deserialize.SscsCaseDataWrapperDeserializer;
import uk.gov.hmcts.reform.sscs.domain.CohEvent;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

@RestController
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthorisationService authorisationService;
    private final CcdService ccdService;
    private final SscsCaseDataWrapperDeserializer deserializer;
    private final IdamService idamService;

    @Autowired
    public NotificationController(NotificationService notificationService, AuthorisationService authorisationService, CcdService ccdService, SscsCaseDataWrapperDeserializer deserializer, IdamService idamService) {
        this.notificationService = notificationService;
        this.authorisationService = authorisationService;
        this.ccdService = ccdService;
        this.deserializer = deserializer;
        this.idamService = idamService;
    }

    @PostMapping(value = "/send", produces = APPLICATION_JSON_VALUE)
    void sendNotification(
            @RequestHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody SscsCaseDataWrapper sscsCaseDataWrapper) {
        log.info("Ccd Response received for case id: {} , {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(), sscsCaseDataWrapper.getNotificationEventType());

        authorisationService.authorise(serviceAuthHeader);
        notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(sscsCaseDataWrapper));
    }

    @PostMapping(value = "/coh-send", produces = APPLICATION_JSON_VALUE)
    void sendCohNotification(
            @RequestHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody CohEvent cohEvent) {
        String caseId = cohEvent.getCaseId();
        log.info("Coh Response received for case id: {}", caseId);
        log.info("Coh Response received for event: {}", cohEvent.getNotificationEventType());

        SscsCaseDetails caseDetails = ccdService.getByCaseId(Long.valueOf(caseId), idamService.getIdamTokens());

        String eventId = cohEvent.getNotificationEventType();
        if (caseDetails != null) {
            SscsCaseDataWrapper wrapper = deserializer.buildSscsCaseDataWrapper(buildCcdNode(caseDetails, eventId));
            notificationService.manageNotificationAndSubscription(new CohNotificationWrapper(cohEvent.getOnlineHearingId(), wrapper));
        } else {
            log.warn("Case id: {} could not be found for event: {}", caseId, eventId);
        }
    }

    private ObjectNode buildCcdNode(SscsCaseDetails caseDetails, String jobName) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.valueToTree(caseDetails);
        ObjectNode node = JsonNodeFactory.instance.objectNode();

        node.set("case_details", jsonNode);
        node = node.put("event_id", jobName);

        return node;
    }

}
