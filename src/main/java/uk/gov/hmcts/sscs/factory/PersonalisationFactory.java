package uk.gov.hmcts.sscs.factory;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.config.NotificationConfig;
import uk.gov.hmcts.sscs.controller.NotificationController;
import uk.gov.hmcts.sscs.domain.notify.NotificationType;
import uk.gov.hmcts.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.sscs.placeholders.AppealReceivedPersonalisation;
import uk.gov.hmcts.sscs.placeholders.EvidenceReceivedPersonalisation;
import uk.gov.hmcts.sscs.placeholders.Personalisation;
import uk.gov.hmcts.sscs.placeholders.ResponseReceivedPersonalisation;

@Component
public class PersonalisationFactory implements Function<NotificationType, Personalisation> {

    private static final org.slf4j.Logger LOG = getLogger(NotificationController.class);

    private final NotificationConfig config;

    @Autowired
    public PersonalisationFactory(NotificationConfig config) {
        this.config = config;
    }

    @Override
    public Personalisation apply(NotificationType notificationType) {
        if (notificationType != null) {
            switch (notificationType) {
                case APPEAL_RECEIVED:
                    return new AppealReceivedPersonalisation(config);
                case DWP_RESPONSE_RECEIVED:
                    return new ResponseReceivedPersonalisation(config);
                case EVIDENCE_RECEIVED:
                    return new EvidenceReceivedPersonalisation(config);
                default:
                    String error = "Unknown Notification type received: " + notificationType;
                    LOG.error(error);
                    throw new NotificationClientRuntimeException(new Exception(error));
            }
        }
        return null;
    }
}
