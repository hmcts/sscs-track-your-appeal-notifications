package uk.gov.hmcts.sscs.service.ccd;

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
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;

@RunWith(MockitoJUnitRunner.class)
public class CreateCcdServiceTest {

    private static final String OAUTH2 = "token";
    private static final String S2SAUTH = "auth";
    private static final String USER_ID = "16";
    private static final String EVENT_ID = "appealCreated";

    @Mock
    private StartEventResponse response;
    @Mock
    private CoreCcdService coreCcdService;

    private CaseDetails caseDetails;

    private CcdResponse ccdResponse;
    private IdamTokens idamTokens;

    private CreateCcdService createCcdService;

    @Before
    public void setUp() {
        idamTokens = IdamTokens.builder()
            .idamOauth2Token(OAUTH2)
            .serviceAuthorization(S2SAUTH)
            .userId(USER_ID)
            .build();

        when(coreCcdService.startCase(idamTokens, EVENT_ID))
            .thenReturn(response);

        ccdResponse = CcdResponse.builder().build();

        caseDetails = CaseDetails.builder().caseTypeId("123").build();

        when(coreCcdService.submitForCaseworker(ccdResponse, idamTokens, response))
            .thenReturn(caseDetails);

        createCcdService = new CreateCcdService(coreCcdService);

    }

    @Test
    public void shouldCallCcdCreateMethodsGivenNewCase() {

        CaseDetails actual = createCcdService.create(ccdResponse, idamTokens);

        assertThat(actual, is(caseDetails));
    }

}
