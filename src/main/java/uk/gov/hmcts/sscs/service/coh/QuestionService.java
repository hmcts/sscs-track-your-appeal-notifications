package uk.gov.hmcts.sscs.service.coh;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;

@Service
public class QuestionService {
    private final CohClient cohClient;

    public QuestionService(@Autowired CohClient cohClient) {
        this.cohClient = cohClient;
    }

    public String getQuestionRequiredByDate(IdamTokens idamTokens, String onlineHearingId) {
        QuestionRounds questionRounds = cohClient.getQuestionRounds(
                idamTokens.getIdamOauth2Token(),
                idamTokens.getServiceAuthorization(),
                onlineHearingId
        );

        int currentQuestionRound = questionRounds.getCurrentQuestionRound();

        List<QuestionReferences> questionRefsForCurrentRound =
                questionRounds.getQuestionRounds().get(currentQuestionRound - 1).getQuestionReferences();
        if (questionRefsForCurrentRound != null && !questionRefsForCurrentRound.isEmpty()) {
            return questionRefsForCurrentRound.get(0).getDeadlineExpiryDate();
        } else {
            throw new IllegalStateException(
                    "Cannot get questions required by date as question round has been published with no questions in it"
            );
        }
    }
}
