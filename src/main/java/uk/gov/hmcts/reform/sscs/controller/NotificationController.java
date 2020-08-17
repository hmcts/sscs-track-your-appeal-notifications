package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.getNotificationByCcdEvent;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
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

            SscsCaseDataWrapper sscsCaseDataWrapper = buildSscsCaseDataWrapper(
                callback.getCaseDetails().getCaseData(),
                caseDetailsBefore != null ? caseDetailsBefore.getCaseData() : null,
                getNotificationByCcdEvent(callback.getEvent()),
                callback.getCaseDetails().getCreatedDate(),
                callback.getCaseDetails().getState());

            log.info("Ccd Response received for case id: {} , {}", sscsCaseDataWrapper.getNewSscsCaseData().getCcdCaseId(), sscsCaseDataWrapper.getNotificationEventType());

            callback.getCaseDetails().getCreatedDate();
            authorisationService.authorise(serviceAuthHeader);
            notificationService.manageNotificationAndSubscription(new CcdNotificationWrapper(sscsCaseDataWrapper));
        } catch (Exception e) {
            log.info("Exception thrown", e);
            throw e;
        }
    }

    private SscsCaseDataWrapper buildSscsCaseDataWrapper(SscsCaseData caseData, SscsCaseData caseDataBefore, NotificationEventType event, LocalDateTime createdDate, State state) {
        return SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseDataBefore)
                .notificationEventType(event)
                .createdDate(createdDate)
                .state(state).build();
    }

}
