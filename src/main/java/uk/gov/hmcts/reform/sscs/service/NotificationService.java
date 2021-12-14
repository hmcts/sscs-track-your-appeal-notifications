package uk.gov.hmcts.reform.sscs.service;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCodeOrThrowException;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SUBSCRIPTION_UPDATED;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.getNotificationByCcdEvent;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.getSubscription;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.isOkToSendNotification;
import static uk.gov.hmcts.reform.sscs.service.NotificationValidService.isMandatoryLetterEventType;

import java.time.ZonedDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.NotificationConfig;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.Destination;
import uk.gov.hmcts.reform.sscs.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Reference;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.factory.NotificationFactory;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.utility.PhoneNumbersUtil;

@Service
@Slf4j
public class NotificationService {
    private static final List<String> PROCESS_AUDIO_VIDEO_ACTIONS_THAT_REQUIRES_NOTICE = asList("issueDirectionsNotice", "excludeEvidence", "admitEvidence");
    private static final String READY_TO_LIST = "readyToList";

    private final NotificationFactory notificationFactory;
    private final ReminderService reminderService;
    private final NotificationValidService notificationValidService;
    private final NotificationHandler notificationHandler;
    private final OutOfHoursCalculator outOfHoursCalculator;
    private final NotificationConfig notificationConfig;

    @SuppressWarnings("squid:S107")
    @Autowired
    public NotificationService(
            NotificationFactory notificationFactory,
            ReminderService reminderService,
            NotificationValidService notificationValidService,
            NotificationHandler notificationHandler,
            OutOfHoursCalculator outOfHoursCalculator,
            NotificationConfig notificationConfig,
            SendNotificationService sendNotificationService,
            @Value("${feature.covid19}") boolean covid19Feature) {

        this.notificationFactory = notificationFactory;
        this.reminderService = reminderService;
        this.notificationValidService = notificationValidService;
        this.notificationHandler = notificationHandler;
        this.outOfHoursCalculator = outOfHoursCalculator;
        this.notificationConfig = notificationConfig;
        this.sendNotificationService = sendNotificationService;
        this.covid19Feature = covid19Feature;
    }

    private final SendNotificationService sendNotificationService;

    private final boolean covid19Feature;

    public void manageNotificationAndSubscription(NotificationWrapper notificationWrapper, boolean fromReminderService) {
        NotificationEventType notificationType = notificationWrapper.getNotificationType();
        final String caseId = notificationWrapper.getCaseId();

        log.info("Checking if notification event {} is valid for case id {}", notificationType.getId(), caseId);
        if (!isEventAllowedToProceedWithValidData(notificationWrapper, notificationType)) {
            return;
        }

        log.info("Notification event triggered {} for case id {}", notificationType.getId(), caseId);

        if (notificationType.isAllowOutOfHours() || !outOfHoursCalculator.isItOutOfHours()) {
            if (notificationType.isToBeDelayed()
                    && !fromReminderService
                    && !functionalTest(notificationWrapper.getNewSscsCaseData())) {

                log.info("Notification event {} is delayed and scheduled for case id {}", notificationType.getId(), caseId);
                notificationHandler.scheduleNotification(notificationWrapper, ZonedDateTime.now().plusSeconds(notificationType.getDelayInSeconds()));
            } else {
                sendNotificationPerSubscription(notificationWrapper);
                reminderService.createReminders(notificationWrapper);
                sendSecondNotificationForLongLetters(notificationWrapper);
            }
        } else if (outOfHoursCalculator.isItOutOfHours()) {
            log.info("Notification event {} is out of hours and scheduled for case id {}", notificationType.getId(), caseId);
            notificationHandler.scheduleNotification(notificationWrapper);
        }
    }

    private boolean functionalTest(SscsCaseData newSscsCaseData) {
        return YesNo.isYes(newSscsCaseData.getFunctionalTest());
    }

    private void sendSecondNotificationForLongLetters(NotificationWrapper notificationWrapper) {
        if (notificationWrapper.getNotificationType().equals(ISSUE_FINAL_DECISION_WELSH)) {
            // Gov Notify has a limit of 10 pages, so for long notifications (especially Welsh) we need to split the sending into 2 parts
            notificationWrapper.getSscsCaseDataWrapper().setNotificationEventType(ISSUE_FINAL_DECISION);
            notificationWrapper.setSwitchLanguageType(true);
            sendNotificationPerSubscription(notificationWrapper);
        }
    }

    private void sendNotificationPerSubscription(NotificationWrapper notificationWrapper) {
        overrideNotificationType(notificationWrapper);
        for (SubscriptionWithType subscriptionWithType : notificationWrapper.getSubscriptionsBasedOnNotificationType()) {
            if (isSubscriptionValidToSendAfterOverride(notificationWrapper, subscriptionWithType)
                    && isValidNotification(notificationWrapper, subscriptionWithType)) {

                sendNotification(notificationWrapper, subscriptionWithType);
                resendLastNotification(notificationWrapper, subscriptionWithType);

            } else {
                log.error("Is not a valid notification event {} for case id {}, not sending notification.",
                        notificationWrapper.getNotificationType().getId(), notificationWrapper.getCaseId());
            }
        }
    }

    private void resendLastNotification(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        if (subscriptionWithType.getSubscription() != null && shouldProcessLastNotification(notificationWrapper, subscriptionWithType)) {
            NotificationEventType lastEvent = getNotificationByCcdEvent(notificationWrapper.getNewSscsCaseData().getEvents().get(0)
                    .getValue().getEventType());
            log.info("Resending the last notification for event {} and case id {}.", lastEvent.getId(), notificationWrapper.getCaseId());
            scrubEmailAndSmsIfSubscribedBefore(notificationWrapper, subscriptionWithType);
            notificationWrapper.getSscsCaseDataWrapper().setNotificationEventType(lastEvent);
            sendNotification(notificationWrapper, subscriptionWithType);
            notificationWrapper.getSscsCaseDataWrapper().setNotificationEventType(SUBSCRIPTION_UPDATED_NOTIFICATION);
        }
    }

    private void overrideNotificationType(NotificationWrapper wrapper) {

        if (REISSUE_DOCUMENT.equals(wrapper.getNotificationType()) && null != wrapper.getNewSscsCaseData().getReissueArtifactUi().getReissueFurtherEvidenceDocument()) {
            String code = wrapper.getNewSscsCaseData().getReissueArtifactUi().getReissueFurtherEvidenceDocument().getValue().getCode();
            if (code.equals(EventType.ISSUE_FINAL_DECISION.getCcdType())) {
                wrapper.setNotificationType(ISSUE_FINAL_DECISION);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.ISSUE_FINAL_DECISION_WELSH.getCcdType())) {
                wrapper.setNotificationType(ISSUE_FINAL_DECISION_WELSH);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.DECISION_ISSUED.getCcdType())) {
                wrapper.setNotificationType(DECISION_ISSUED);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.DIRECTION_ISSUED.getCcdType())) {
                wrapper.setNotificationType(DIRECTION_ISSUED);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.ISSUE_ADJOURNMENT_NOTICE.getCcdType())) {
                wrapper.setNotificationType(ISSUE_ADJOURNMENT_NOTICE);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.ISSUE_ADJOURNMENT_NOTICE_WELSH.getCcdType())) {
                wrapper.setNotificationType(ISSUE_ADJOURNMENT_NOTICE_WELSH);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.DECISION_ISSUED_WELSH.getCcdType())) {
                wrapper.setNotificationType(DECISION_ISSUED_WELSH);
                wrapper.setNotificationEventTypeOverridden(true);
            } else if (code.equals(EventType.DIRECTION_ISSUED_WELSH.getCcdType())) {
                wrapper.setNotificationType(DIRECTION_ISSUED_WELSH);
                wrapper.setNotificationEventTypeOverridden(true);
            }
        } else if (DRAFT_TO_VALID_APPEAL_CREATED.equals(wrapper.getNotificationType())) {
            wrapper.setNotificationType(VALID_APPEAL_CREATED);
        } else if (DRAFT_TO_NON_COMPLIANT_NOTIFICATION.equals(wrapper.getNotificationType())) {
            wrapper.setNotificationType(NON_COMPLIANT_NOTIFICATION);
        }
    }

    private static boolean isSubscriptionValidToSendAfterOverride(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType) {
        if (wrapper.hasNotificationEventBeenOverridden()) {
            if ((APPELLANT.equals(subscriptionWithType.getSubscriptionType()) || APPOINTEE.equals(subscriptionWithType.getSubscriptionType()))
                    && !YesNo.YES.equals(wrapper.getNewSscsCaseData().getReissueArtifactUi().getResendToAppellant())) {
                return false;
            }
            if (REPRESENTATIVE.equals(subscriptionWithType.getSubscriptionType())
                    && !YesNo.YES.equals(wrapper.getNewSscsCaseData().getReissueArtifactUi().getResendToRepresentative())) {
                return false;
            }
        }
        return true;
    }

    private void scrubEmailAndSmsIfSubscribedBefore(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        Subscription oldSubscription = getSubscription(notificationWrapper.getOldSscsCaseData(), subscriptionWithType.getSubscriptionType());
        Subscription newSubscription = subscriptionWithType.getSubscription();
        String email = oldSubscription != null && oldSubscription.isEmailSubscribed() ? null : newSubscription.getEmail();
        String mobile = oldSubscription != null && oldSubscription.isSmsSubscribed() ? null : newSubscription.getMobile();
        subscriptionWithType.setSubscription(newSubscription.toBuilder().email(email).mobile(mobile).build());
    }

    private boolean shouldProcessLastNotification(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        return SUBSCRIPTION_UPDATED_NOTIFICATION.equals(notificationWrapper.getSscsCaseDataWrapper().getNotificationEventType())
                && hasCaseJustSubscribed(subscriptionWithType.getSubscription(), getSubscription(notificationWrapper.getOldSscsCaseData(), subscriptionWithType.getSubscriptionType()))
                && thereIsALastEventThatIsNotSubscriptionUpdated(notificationWrapper.getNewSscsCaseData());
    }

    static Boolean hasCaseJustSubscribed(Subscription newSubscription, Subscription oldSubscription) {
        return ((oldSubscription == null || !oldSubscription.isEmailSubscribed()) && newSubscription.isEmailSubscribed()
                || ((oldSubscription == null || !oldSubscription.isSmsSubscribed()) && newSubscription.isSmsSubscribed()));
    }

    private static boolean thereIsALastEventThatIsNotSubscriptionUpdated(final SscsCaseData newSscsCaseData) {
        boolean thereIsALastEventThatIsNotSubscriptionUpdated = newSscsCaseData.getEvents() != null
                && !newSscsCaseData.getEvents().isEmpty()
                && newSscsCaseData.getEvents().get(0).getValue().getEventType() != null
                && !SUBSCRIPTION_UPDATED.equals(newSscsCaseData.getEvents().get(0).getValue().getEventType());
        if (!thereIsALastEventThatIsNotSubscriptionUpdated) {
            log.info("Not re-sending the last subscription as there is no last event for ccdCaseId {}.", newSscsCaseData.getCcdCaseId());
        }
        return thereIsALastEventThatIsNotSubscriptionUpdated;
    }

    private void sendNotification(NotificationWrapper notificationWrapper, SubscriptionWithType subscriptionWithType) {
        Notification notification = notificationFactory.create(notificationWrapper, subscriptionWithType);
        sendNotificationService.sendEmailSmsLetterNotification(notificationWrapper, notification, subscriptionWithType, notificationWrapper.getNotificationType());
        processOldSubscriptionNotifications(notificationWrapper, notification, subscriptionWithType, notificationWrapper.getNotificationType());
    }

    private boolean isValidNotification(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType) {
        Subscription subscription = subscriptionWithType.getSubscription();

        return (isMandatoryLetterEventType(wrapper.getNotificationType())
                || isOkToSendNotification(wrapper, wrapper.getNotificationType(), subscription, notificationValidService));
    }

    private void processOldSubscriptionNotifications(NotificationWrapper wrapper, Notification notification, SubscriptionWithType subscriptionWithType, NotificationEventType eventType) {
        if (wrapper.getNotificationType() == SUBSCRIPTION_UPDATED_NOTIFICATION) {
            Subscription newSubscription;
            Subscription oldSubscription;
            if (REPRESENTATIVE.equals(subscriptionWithType.getSubscriptionType())) {
                newSubscription = wrapper.getNewSscsCaseData().getSubscriptions().getRepresentativeSubscription();
                oldSubscription = wrapper.getOldSscsCaseData().getSubscriptions().getRepresentativeSubscription();
            } else if (APPOINTEE.equals(subscriptionWithType.getSubscriptionType())) {
                newSubscription = wrapper.getNewSscsCaseData().getSubscriptions().getAppointeeSubscription();
                oldSubscription = wrapper.getOldSscsCaseData().getSubscriptions().getAppointeeSubscription();
            } else {
                newSubscription = wrapper.getNewSscsCaseData().getSubscriptions().getAppellantSubscription();
                oldSubscription = wrapper.getOldSscsCaseData().getSubscriptions().getAppellantSubscription();
            }

            String emailAddress = getSubscriptionDetails(newSubscription.getEmail(), oldSubscription.getEmail());
            String smsNumber = getSubscriptionDetails(newSubscription.getMobile(), oldSubscription.getMobile());

            Destination destination = Destination.builder().email(emailAddress)
                    .sms(PhoneNumbersUtil.cleanPhoneNumber(smsNumber).orElse(smsNumber)).build();

            Benefit benefit = getBenefitByCodeOrThrowException(wrapper.getSscsCaseDataWrapper()
                    .getNewSscsCaseData().getAppeal().getBenefitType().getCode());

            Template template = notificationConfig.getTemplate(
                    NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                    NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                    NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                    NotificationEventType.SUBSCRIPTION_OLD_NOTIFICATION.getId(),
                    benefit,
                    wrapper,
                    "validAppeal"
            );

            Notification oldNotification = Notification.builder().template(template).appealNumber(notification.getAppealNumber())
                    .destination(destination)
                    .reference(new Reference(wrapper.getOldSscsCaseData().getCaseReference()))
                    .appealNumber(notification.getAppealNumber())
                    .placeholders(notification.getPlaceholders()).build();

            SubscriptionWithType updatedSubscriptionWithType = new SubscriptionWithType(oldSubscription, subscriptionWithType.getSubscriptionType());
            sendNotificationService.sendEmailSmsLetterNotification(wrapper, oldNotification, updatedSubscriptionWithType, eventType);
        }
    }

    private String getSubscriptionDetails(String newSubscription, String oldSubscription) {
        String subscription = "";
        if (null != newSubscription && null != oldSubscription) {
            subscription = newSubscription.equals(oldSubscription) ? null : oldSubscription;
        } else if (null == newSubscription && null != oldSubscription) {
            subscription = oldSubscription;
        }
        return subscription;
    }

    private boolean isEventAllowedToProceedWithValidData(NotificationWrapper notificationWrapper,
                                                         NotificationEventType notificationType) {
        if (REQUEST_INFO_INCOMPLETE.equals(notificationType)) {
            if (StringUtils.isEmpty(notificationWrapper.getNewSscsCaseData().getInformationFromAppellant())
                    || "No".equalsIgnoreCase(notificationWrapper.getNewSscsCaseData().getInformationFromAppellant())) {

                log.info("Request Incomplete Information with empty or no Information From Appellant for ccdCaseId {}.", notificationWrapper.getNewSscsCaseData().getCcdCaseId());
                return false;
            }
        }

        if (notificationWrapper.getSscsCaseDataWrapper().getState() != null
                && notificationWrapper.getSscsCaseDataWrapper().getState().equals(State.DORMANT_APPEAL_STATE)) {
            if (!(NotificationEventType.APPEAL_DORMANT_NOTIFICATION.equals(notificationType)
                    || NotificationEventType.APPEAL_LAPSED_NOTIFICATION.equals(notificationType)
                    || NotificationEventType.HMCTS_APPEAL_LAPSED_NOTIFICATION.equals(notificationType)
                    || NotificationEventType.DWP_APPEAL_LAPSED_NOTIFICATION.equals(notificationType)
                    || ADMIN_APPEAL_WITHDRAWN.equals(notificationType)
                    || APPEAL_WITHDRAWN_NOTIFICATION.equals(notificationType)
                    || STRUCK_OUT.equals(notificationType)
                    || DECISION_ISSUED.equals(notificationType)
                    || DIRECTION_ISSUED.equals(notificationType)
                    || DECISION_ISSUED_WELSH.equals(notificationType)
                    || DIRECTION_ISSUED_WELSH.equals(notificationType)
                    || ISSUE_FINAL_DECISION.equals(notificationType)
                    || ISSUE_FINAL_DECISION_WELSH.equals(notificationType)
                    || REISSUE_DOCUMENT.equals(notificationType)
                    || PROVIDE_APPOINTEE_DETAILS.equals(notificationType))) {
                log.info(format("Cannot complete notification %s as the appeal was dormant for caseId %s.",
                        notificationType.getId(), notificationWrapper.getCaseId()));
                return false;
            }
        }
        if (notificationWrapper.getNewSscsCaseData().isLanguagePreferenceWelsh() && (ISSUE_FINAL_DECISION.equals(notificationType) || DECISION_ISSUED.equals(notificationType) || DIRECTION_ISSUED.equals(notificationType) || ISSUE_ADJOURNMENT_NOTICE.equals(notificationType) || PROCESS_AUDIO_VIDEO.equals(notificationType) || ACTION_POSTPONEMENT_REQUEST.equals(notificationType))) {
            log.info(format("Cannot complete notification %s as the appeal is Welsh  for caseId %s.",
                    notificationType.getId(), notificationWrapper.getCaseId()));
            return false;
        }
        final String processAudioVisualAction = ofNullable(notificationWrapper.getNewSscsCaseData().getProcessAudioVideoAction())
                        .map(f -> f.getValue().getCode()).orElse(null);

        if (notificationType.equals(PROCESS_AUDIO_VIDEO)
                && !PROCESS_AUDIO_VIDEO_ACTIONS_THAT_REQUIRES_NOTICE.contains(processAudioVisualAction)) {
            log.info(format("Cannot complete notification %s since the action %s does not require a notice to be sent for caseId %s.",
                    notificationType.getId(), processAudioVisualAction, notificationWrapper.getCaseId()));
            return false;
        }

        if (!isDigitalCase(notificationWrapper) && DWP_UPLOAD_RESPONSE_NOTIFICATION.equals(notificationType)) {
            log.info(format("Cannot complete notification %s as the appeal was dwpUploadResponse for caseId %s.",
                    notificationType.getId(), notificationWrapper.getCaseId()));
            return false;
        }

        if (DWP_RESPONSE_RECEIVED_NOTIFICATION.equals(notificationType) && isDigitalCase(notificationWrapper)) {
            log.info(format("Cannot complete notification %s as the appeal was digital for caseId %s.",
                    notificationType.getId(), notificationWrapper.getCaseId()));
            return false;
        }

        if (covid19Feature && (HEARING_BOOKED_NOTIFICATION.equals(notificationType) || HEARING_REMINDER_NOTIFICATION.equals(notificationType))) {
            log.info(format("Notification not valid to send as covid 19 feature flag on for case id %s and event %s in state %s", notificationWrapper.getCaseId(), notificationType.getId(), notificationWrapper.getSscsCaseDataWrapper().getState()));
            return false;
        }
        log.info(format("Notification valid to send for case id %s and event %s in state %s", notificationWrapper.getCaseId(), notificationType.getId(), notificationWrapper.getSscsCaseDataWrapper().getState()));
        return true;
    }

    private boolean isDigitalCase(final NotificationWrapper notificationWrapper) {
        return READY_TO_LIST.equals(notificationWrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getCreatedInGapsFrom());
    }
}
