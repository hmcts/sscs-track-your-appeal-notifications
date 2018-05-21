package uk.gov.hmcts.sscs.service.ccd;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;

@RunWith(MockitoJUnitRunner.class)
public class CoreCcdServiceTest {

    private static final String OAUTH2 = "token";
    private static final String S2SAUTH = "auth";
    private static final String USER_ID = "16";
    private static final Long CASE_ID = 989897L;
    private static final String EVENT_ID = "appealCreated";
    private static final String CCD_TOKEN = "ccdToken";
    private static final String CCD_EVENT = "ccdEvent";

    @Mock
    private CoreCaseDataApi ccdApi;
    @Mock
    private StartEventResponse response;
    @Mock
    private CaseDetails caseDetails;
    @Mock
    private CcdResponse ccdResponse;
    private IdamTokens idamTokens;

    private CoreCaseDataProperties ccdProperties;

    private CoreCcdService coreCcdService;

    @Before
    public void setUp() {
        ccdProperties = new CoreCaseDataProperties();
        ccdProperties.setJurisdictionId("SSCS");
        ccdProperties.setCaseTypeId("Benefits");

        when(response.getToken()).thenReturn(CCD_TOKEN);
        when(response.getEventId()).thenReturn(CCD_EVENT);

        coreCcdService = new CoreCcdService(ccdApi, ccdProperties);
        idamTokens = IdamTokens.builder()
            .idamOauth2Token(OAUTH2)
            .serviceAuthorization(S2SAUTH)
            .userId(USER_ID)
            .build();
    }

    @Test
    public void givenANewCase_shouldCallStartForCaseworkerInCcd() {

        StartEventResponse expectedStartEventResponse =
            StartEventResponse.builder().eventId(EVENT_ID).caseDetails(caseDetails).token("1234").build();

        when(ccdApi.startForCaseworker(
            eq(OAUTH2),
            eq(S2SAUTH),
            eq(USER_ID),
            eq(ccdProperties.getJurisdictionId()),
            eq(ccdProperties.getCaseTypeId()),
            eq(EVENT_ID)))
            .thenReturn(expectedStartEventResponse);

        StartEventResponse eventResponse = coreCcdService.startCase(idamTokens, EVENT_ID);

        assertThat(eventResponse, is(expectedStartEventResponse));
    }

    @Test
    public void givenANewCaseWithStartEventResponse_shouldCallSubmitForCaseworkerInCcd() {

        StartEventResponse expectedStartEventResponse =
            StartEventResponse.builder().eventId(EVENT_ID).caseDetails(caseDetails).token("1234").build();

        when(ccdApi.submitForCaseworker(
            eq(OAUTH2),
            eq(S2SAUTH),
            eq(USER_ID),
            eq(ccdProperties.getJurisdictionId()),
            eq(ccdProperties.getCaseTypeId()),
            eq(true),
            any()))
            .thenReturn(caseDetails);

        CaseDetails actual = coreCcdService.submitForCaseworker(ccdResponse, idamTokens, expectedStartEventResponse);

        assertThat(actual, is(caseDetails));
    }

    @Test
    public void givenAnUpdateToACase_shouldCallStartEventForCaseworkerInCcd() {

        StartEventResponse expectedStartEventResponse =
            StartEventResponse.builder().eventId(EVENT_ID).caseDetails(caseDetails).token("1234").build();

        when(ccdApi.startEventForCaseWorker(
            eq(OAUTH2),
            eq(S2SAUTH),
            eq(USER_ID),
            eq(ccdProperties.getJurisdictionId()),
            eq(ccdProperties.getCaseTypeId()),
            eq(CASE_ID.toString()),
            eq(EVENT_ID)))
            .thenReturn(expectedStartEventResponse);

        StartEventResponse eventResponse = coreCcdService.startEvent(idamTokens, CASE_ID.toString(), EVENT_ID);

        assertThat(eventResponse, is(expectedStartEventResponse));
    }

    @Test
    public void givenAnUpdateToACaseWithStartEventResponse_shouldCallSubmitEventForCaseworkerInCcd() {

        StartEventResponse expectedStartEventResponse =
            StartEventResponse.builder().eventId(EVENT_ID).caseDetails(caseDetails).token("1234").build();

        when(ccdApi.submitEventForCaseWorker(
            eq(OAUTH2),
            eq(S2SAUTH),
            eq(USER_ID),
            eq(ccdProperties.getJurisdictionId()),
            eq(ccdProperties.getCaseTypeId()),
            eq(CASE_ID.toString()),
            eq(true),
            any()))
            .thenReturn(caseDetails);

        CaseDetails actual = coreCcdService.submitEventForCaseworker(ccdResponse, CASE_ID, idamTokens, expectedStartEventResponse);

        assertThat(actual, is(caseDetails));
    }

}
