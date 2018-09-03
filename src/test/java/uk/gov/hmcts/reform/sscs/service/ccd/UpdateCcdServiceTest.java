package uk.gov.hmcts.reform.sscs.service.ccd;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(MockitoJUnitRunner.class)
public class UpdateCcdServiceTest {

    private static final String OAUTH2 = "token";
    private static final String S2SAUTH = "auth";
    private static final String USER_ID = "16";
    private static final String EVENT_ID = "appealReceived";
    private static final Long CASE_ID = 1L;

    @Mock
    private StartEventResponse response;
    @Mock
    private CaseDetails caseDetails;
    @Mock
    private CoreCcdService coreCcdService;

    private SscsCaseData ccdResponse;
    private IdamTokens idamTokens;

    private UpdateCcdService updateCcdService;

    @Before
    public void setUp() {
        idamTokens = IdamTokens.builder()
            .idamOauth2Token(OAUTH2)
            .serviceAuthorization(S2SAUTH)
            .userId(USER_ID)
            .build();

        when(coreCcdService.startEvent(idamTokens, CASE_ID.toString(), EVENT_ID))
            .thenReturn(response);

        ccdResponse = SscsCaseData.builder().build();

        caseDetails = CaseDetails.builder().caseTypeId("123").build();

        when(coreCcdService.submitEventForCaseworker(ccdResponse, CASE_ID, idamTokens, response))
            .thenReturn(caseDetails);

        updateCcdService = new UpdateCcdService(coreCcdService);
    }

    @Test
    public void shouldCallCcdUpdateMethodsGivenUpdatedCase() {

        CaseDetails actual = updateCcdService.update(ccdResponse, CASE_ID, EVENT_ID, idamTokens);

        assertThat(actual, is(caseDetails));
    }

}
