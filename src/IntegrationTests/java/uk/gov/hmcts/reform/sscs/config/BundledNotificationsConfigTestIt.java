package uk.gov.hmcts.reform.sscs.config;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.ORAL;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.PAPER;
import static uk.gov.hmcts.reform.sscs.config.NotificationEventTypeLists.EVENT_TYPES_FOR_BUNDLED_LETTER;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ACTION_POSTPONEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ACTION_POSTPONEMENT_REQUEST_WELSH;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.DECISION_ISSUED_WELSH;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_ADJOURNMENT_NOTICE_WELSH;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.ISSUE_FINAL_DECISION_WELSH;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junitparams.Parameters;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;

public class BundledNotificationsConfigTestIt extends AbstractNotificationConfigTest {
    private static final Set<NotificationEventType> BUNDLED_LETTER_EVENT_TYPES_IGNORED = EnumSet.of(
        ACTION_POSTPONEMENT_REQUEST,
        ACTION_POSTPONEMENT_REQUEST_WELSH,
        DECISION_ISSUED,
        DECISION_ISSUED_WELSH,
        ISSUE_FINAL_DECISION,
        ISSUE_FINAL_DECISION_WELSH,
        ISSUE_ADJOURNMENT_NOTICE,
        ISSUE_ADJOURNMENT_NOTICE_WELSH
    );
    private static final Set<AppealHearingType> APPEAL_HEARING_TYPES = Set.of(PAPER, ORAL);

    @Test
    @Parameters(method = "bundledLetterTemplateNames")
    public void given_bundledLetters_should_notHaveTemplate(NotificationEventType eventType) {
        List<Template> templates = getTemplates(eventType);

        assertThat(templates)
            .isNotEmpty()
            .allSatisfy(template -> {
                assertThat(template.getEmailTemplateId()).isNull();
                assertThat(template.getSmsTemplateId()).isEmpty();
                assertThat(template.getLetterTemplateId()).isNull();
                assertThat(template.getDocmosisTemplateId()).isNull();
            });
    }

    @NotNull
    private List<Template> getTemplates(NotificationEventType eventType) {
        List<Template> templates = new ArrayList<>();
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            for (AppealHearingType appealHearingType : APPEAL_HEARING_TYPES) {
                templates.add(getTemplate(eventType, subscriptionType, appealHearingType, true, null));
                templates.add(getTemplate(eventType, subscriptionType, appealHearingType, false, null));
            }
        }
        return templates;
    }

    private Object[] bundledLetterTemplateNames() {
        Set<NotificationEventType> bundledLetterEventTypes = new HashSet<>(EVENT_TYPES_FOR_BUNDLED_LETTER);
        bundledLetterEventTypes.removeAll(BUNDLED_LETTER_EVENT_TYPES_IGNORED);
        return bundledLetterEventTypes.toArray();
    }
}
