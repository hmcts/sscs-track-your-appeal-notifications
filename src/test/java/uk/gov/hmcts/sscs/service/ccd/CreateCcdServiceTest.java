package uk.gov.hmcts.sscs.service.ccd;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.models.idam.IdamTokens;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@RunWith(MockitoJUnitRunner.class)
public class CreateCcdServiceTest {

    private static final String OAUTH2 = "token";
    private static final String S2SAUTH = "auth";
    private static final String EVENT_ID = "appealCreated";
    private static final String CCD_TOKEN = "ccdToken";
    private static final String CCD_EVENT = "ccdEvent";

    @Mock
    private IdamService idamService;
    @Mock
    private CoreCaseDataApi ccdApi;
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
        stub(idamService.generateServiceAuthorization()).toReturn(S2SAUTH);
        stub(idamService.getIdamOauth2Token()).toReturn(OAUTH2);

        when(coreCcdService.startCase(S2SAUTH, OAUTH2, EVENT_ID))
            .thenReturn(response);

        idamTokens = IdamTokens.builder()
                .idamOauth2Token(OAUTH2)
                .authenticationService(S2SAUTH)
                .build();

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
