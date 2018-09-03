package uk.gov.hmcts.reform.sscs.personalisation;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.service.coh.QuestionService;

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
}
