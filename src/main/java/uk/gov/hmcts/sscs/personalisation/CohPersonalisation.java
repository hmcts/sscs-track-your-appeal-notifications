package uk.gov.hmcts.sscs.personalisation;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.domain.Benefit;
import uk.gov.hmcts.sscs.domain.notify.Template;
import uk.gov.hmcts.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.sscs.service.coh.QuestionRounds;
import uk.gov.hmcts.sscs.service.coh.QuestionService;

@Component
public class CohPersonalisation extends Personalisation<CohNotificationWrapper> {

    @Autowired
    private QuestionService questionService;

    @Override
    public Map<String, String> create(CohNotificationWrapper notificationWrapper) {
        Map<String, String> placeholders = super.create(notificationWrapper);

        String questionRequiredByDate = questionService.getQuestionRequiredByDate(notificationWrapper.getIdamTokens(), notificationWrapper.getOnlineHearingId());

        placeholders.put("questions_end_date", questionRequiredByDate);

        return placeholders;
    }

    public Template getTemplate(CohNotificationWrapper notificationWrapper, Benefit benefit) {
        // If we remembered the question rounds before we would not need to make this call but currently Personalisation is a singleton
        QuestionRounds questionRounds = questionService.getQuestionRounds(notificationWrapper.getIdamTokens(), notificationWrapper.getOnlineHearingId());
        if (questionRounds.getCurrentQuestionRound() == 1) {
            return super.getTemplate(notificationWrapper, benefit);
        }
        return config.getTemplate("follow_up_question_round_issued", "follow_up_question_round_issued", benefit);
    }
}
