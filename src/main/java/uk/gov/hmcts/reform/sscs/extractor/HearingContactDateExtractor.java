package uk.gov.hmcts.reform.sscs.extractor;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

@Component
public class HearingContactDateExtractor {

    private final DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;
    private final long initialDelay;
    private final long subsequentDelay;

    @Autowired
    public HearingContactDateExtractor(
        DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor,
        @Value("${reminder.hearingContactDate.initialDelay.seconds}") long initialDelay,
        @Value("${reminder.hearingContactDate.subsequentDelay.seconds}") long subsequentDelay
    ) {
        this.dwpResponseReceivedDateExtractor = dwpResponseReceivedDateExtractor;
        this.initialDelay = initialDelay;
        this.subsequentDelay = subsequentDelay;
    }

    public Optional<ZonedDateTime> extract(SscsCaseDataWrapper wrapper) {
        return extractForReferenceEvent(wrapper.getNewSscsCaseData(), wrapper.getNotificationEventType());
    }

    public Optional<ZonedDateTime> extractForReferenceEvent(
        SscsCaseData sscsCaseData,
        NotificationEventType referenceNotificationEventType
    ) {
        long delay;

        switch (referenceNotificationEventType) {

            case DWP_RESPONSE_RECEIVED_NOTIFICATION:
            case ADJOURNED_NOTIFICATION:
            case POSTPONEMENT_NOTIFICATION:
                delay = initialDelay;
                break;

            case FIRST_HEARING_HOLDING_REMINDER_NOTIFICATION:
                delay = initialDelay + subsequentDelay;
                break;

            case SECOND_HEARING_HOLDING_REMINDER_NOTIFICATION:
                delay = initialDelay + (subsequentDelay * 2);
                break;

            case THIRD_HEARING_HOLDING_REMINDER_NOTIFICATION:
                delay = initialDelay + (subsequentDelay * 3);
                break;

            default:
                return Optional.empty();
        }

        Optional<ZonedDateTime> optionalDwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(sscsCaseData);

        return optionalDwpResponseReceivedDate
            .map(dwpResponseReceivedDate -> dwpResponseReceivedDate.plusSeconds(delay));
    }

}
