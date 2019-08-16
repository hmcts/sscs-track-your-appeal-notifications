package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.getNotificationByCcdEvent;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.getNotificationById;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.CohEvent;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
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
    private final SscsCaseCallbackDeserializer deserializer;
    private final IdamService idamService;

    @Autowired
    public NotificationController(NotificationService notificationService,
                                  AuthorisationService authorisationService,
                                  CcdService ccdService,
                                  SscsCaseCallbackDeserializer deserializer,
                                  IdamService idamService) {
        this.notificationService = notificationService;
        this.authorisationService = authorisationService;
        this.ccdService = ccdService;
        this.deserializer = deserializer;
        this.idamService = idamService;
    }

    @PostMapping(value = "/send", produces = APPLICATION_JSON_VALUE)
    public void sendNotification(
            @RequestHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody String message) {
        try {
            Callback<SscsCaseData> callback = deserializer.deserialize(message);

            CaseDetails<SscsCaseData> caseDetailsBefore = callback.getCaseDetailsBefore().orElse(null);

            if (callback.getEvent() != null) {
                log.info("Received a callback of event type " + callback.getEvent().getType());
            }

            SscsCaseDataWrapper sscsCaseDataWrapper = buildSscsCaseDataWrapper(
                callback.getCaseDetails().getCaseData(),
                caseDetailsBefore != null ? caseDetailsBefore.getCaseData() : null,
                getNotificationByCcdEvent(callback.getEvent()),
                callback.getCaseDetails().getState());

            log.info("Ccd Response received for case id: {} , {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(), sscsCaseDataWrapper.getNotificationEventType());

            authorisationService.authorise(serviceAuthHeader);
            notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(sscsCaseDataWrapper));
        } catch (Exception e) {
            log.info("Exception thrown", e);
            throw e;
        }
    }

    @PostMapping(value = "/coh-send", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sendCohNotification(
            @RequestHeader(AuthorisationService.SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
            @RequestBody CohEvent cohEvent) {
        String caseId = cohEvent.getCaseId();
        log.info("Coh Response received for case id: {} event: {}", caseId, cohEvent.getNotificationEventType());

        if (!NotificationEventType.checkEvent(cohEvent.getNotificationEventType())) {
            log.info("Coh Response bad request unhandled event for case id: {} event: {}", caseId, cohEvent.getNotificationEventType());
            return ResponseEntity.badRequest().body("Unhandled event: " + cohEvent.getNotificationEventType());
        }

        SscsCaseDetails caseDetails = ccdService.getByCaseId(Long.valueOf(caseId), idamService.getIdamTokens());

        String eventId = cohEvent.getNotificationEventType();
        if (caseDetails != null) {
            SscsCaseDataWrapper wrapper = buildSscsCaseDataWrapper(caseDetails.getData(),
                    null,
                    getNotificationById(eventId),
                    getStateFromString(caseDetails.getState())
            );
            notificationService.manageNotificationAndSubscription(new CohNotificationWrapper(cohEvent.getOnlineHearingId(), wrapper));
        } else {
            log.warn("Case id: {} could not be found for event: {}", caseId, eventId);
        }
        log.info("Coh Response handled for case id: {} event: {}", caseId, cohEvent.getNotificationEventType());
        return ResponseEntity.ok("");
    }

    private State getStateFromString(String value) {
        return Arrays.stream(State.values()).filter(s -> s.toString().equalsIgnoreCase(value)).findFirst().orElse(State.UNKNOWN);
    }

    private SscsCaseDataWrapper buildSscsCaseDataWrapper(SscsCaseData caseData, SscsCaseData caseDataBefore, NotificationEventType event, State state) {
        return SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseDataBefore)
                .notificationEventType(event)
                .state(state).build();
    }

}
