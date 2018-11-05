package uk.gov.hmcts.reform.sscs.personalisation;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.coh.QuestionRounds;
import uk.gov.hmcts.reform.sscs.service.coh.QuestionService;

@Component
public class CohPersonalisation extends Personalisation<CohNotificationWrapper> {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private NotificationDateConverterUtil notificationDateConverterUtil;

    @Override
    public Map<String, String> create(CohNotificationWrapper notificationWrapper) {
        Map<String, String> placeholders = super.create(notificationWrapper);

        String questionRequiredByDate = questionService.getQuestionRequiredByDate(notificationWrapper.getOnlineHearingId());

        String dateEmailFormat = notificationDateConverterUtil.toEmailDate(questionRequiredByDate);
        placeholders.put("questions_end_date", dateEmailFormat);

        return placeholders;
    }

    @Override
    public Template getTemplate(CohNotificationWrapper notificationWrapper, Benefit benefit,
                                SubscriptionType subscriptionType) {
        // If we remembered the question rounds before we would not need to make this call but currently Personalisation is a singleton
        QuestionRounds questionRounds = questionService.getQuestionRounds(notificationWrapper.getOnlineHearingId());
        if (questionRounds.getCurrentQuestionRound() == 1) {
            NotificationEventType type = notificationWrapper.getNotificationType();
            return config.getTemplate(type.getId(), type.getId(), benefit, notificationWrapper.getHearingType());
        }
        return config.getTemplate("follow_up_question_round_issued", "follow_up_question_round_issued", benefit, notificationWrapper.getHearingType());
    }
}
