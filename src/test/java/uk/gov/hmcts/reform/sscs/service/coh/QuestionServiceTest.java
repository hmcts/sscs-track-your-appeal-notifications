package uk.gov.hmcts.reform.sscs.service.coh;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class QuestionServiceTest {

    private CohClient cohClient;
    private String someHearingId;
    private String expectedDate;
    private String authHeader;
    private String serviceAuthHeader;
    private IdamTokens idamTokens;

    @Before
    public void setUp() {
        cohClient = mock(CohClient.class);
        someHearingId = "someHearingId";
        expectedDate = "expectedDate";
        authHeader = "authHeader";
        serviceAuthHeader = "serviceAuthHeader";
        idamTokens = IdamTokens.builder().idamOauth2Token(authHeader).serviceAuthorization(serviceAuthHeader).build();
    }

    @Test
    public void getsRequiredByDateForFirstRound() {
        when(cohClient.getQuestionRounds(authHeader, serviceAuthHeader, someHearingId))
                .thenReturn(new QuestionRounds(1, singletonList(
                        new QuestionRound(Collections.singletonList(new QuestionReferences(expectedDate))))
                ));
        String questionRequiredByDate = new QuestionService(cohClient).getQuestionRequiredByDate(idamTokens, someHearingId);

        assertThat(questionRequiredByDate, is(expectedDate));
    }

    @Test
    public void getsRequiredByDateForSecondRound() {
        when(cohClient.getQuestionRounds(authHeader, serviceAuthHeader, someHearingId))
                .thenReturn(new QuestionRounds(2, asList(
                        new QuestionRound(Collections.singletonList(new QuestionReferences("Different date"))),
                        new QuestionRound(Collections.singletonList(new QuestionReferences(expectedDate))))
                ));
        String questionRequiredByDate = new QuestionService(cohClient).getQuestionRequiredByDate(idamTokens, someHearingId);

        assertThat(questionRequiredByDate, is(expectedDate));
    }

    @Test(expected = IllegalStateException.class)
    public void questionRoundsMustHaveAtLeastOneQuestion() {
        when(cohClient.getQuestionRounds(authHeader, serviceAuthHeader, someHearingId))
                .thenReturn(new QuestionRounds(1, singletonList(new QuestionRound(emptyList()))));

        new QuestionService(cohClient).getQuestionRequiredByDate(idamTokens, someHearingId);
    }
}