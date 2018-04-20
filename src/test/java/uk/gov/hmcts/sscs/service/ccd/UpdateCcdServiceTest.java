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
import uk.gov.hmcts.sscs.config.properties.CoreCaseDataProperties;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.models.idam.IdamTokens;
import uk.gov.hmcts.sscs.service.idam.IdamService;

@RunWith(MockitoJUnitRunner.class)
public class UpdateCcdServiceTest {

    private static final String OAUTH2 = "token";
    private static final String S2SAUTH = "auth";
    private static final String EVENT_ID = "appealReceived";
    private static final Long CASE_ID = 1L;

    @Mock
    private IdamService idamService;
    @Mock
    private StartEventResponse response;
    @Mock
    private CaseDetails caseDetails;
    @Mock
    private CoreCcdService coreCcdService;

    private CcdResponse ccdResponse;
    private IdamTokens idamTokens;

    private UpdateCcdService updateCcdService;

    @Before
    public void setUp() {
        stub(idamService.generateServiceAuthorization()).toReturn(S2SAUTH);
        stub(idamService.getIdamOauth2Token()).toReturn(OAUTH2);

        when(coreCcdService.startEvent(S2SAUTH, OAUTH2, CASE_ID.toString(), EVENT_ID))
            .thenReturn(response);

        idamTokens = IdamTokens.builder()
                .idamOauth2Token(OAUTH2)
                .authenticationService(S2SAUTH)
                .build();

        ccdResponse = CcdResponse.builder().build();

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
