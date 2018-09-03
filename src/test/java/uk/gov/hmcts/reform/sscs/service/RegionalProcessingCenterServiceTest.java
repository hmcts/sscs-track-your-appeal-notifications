package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;

public class RegionalProcessingCenterServiceTest {

    public static final String SSCS_LIVERPOOL = "SSCS Liverpool";

    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Before
    public void setUp() {
        regionalProcessingCenterService = new RegionalProcessingCenterService();
    }

    @Test
    public void givenVenuesCvsFile_shouldLoadScCodeToRpcMap() {
        //When
        regionalProcessingCenterService.init();

        //Then
        Map<String, String> scCodeRegionalProcessingCenterMap = regionalProcessingCenterService.getScCodeRegionalProcessingCenterMap();
        assertThat(scCodeRegionalProcessingCenterMap.size(), equalTo(245));
        assertThat(scCodeRegionalProcessingCenterMap.get("SC038"), equalTo("SSCS Birmingham"));
        assertThat(scCodeRegionalProcessingCenterMap.get("SC001"), equalTo("SSCS Leeds"));
        assertThat(scCodeRegionalProcessingCenterMap.get("SC293"), equalTo("SSCS Cardiff"));
    }

    @Test
    public void givenRpcMetaData_shouldLoadRpcMetadataToMap() {
        //When
        regionalProcessingCenterService.init();

        //Then
        Map<String, RegionalProcessingCenter> regionalProcessingCenterMap = regionalProcessingCenterService.getRegionalProcessingCenterMap();

        assertThat(regionalProcessingCenterMap.size(), equalTo(6));
        RegionalProcessingCenter regionalProcessingCenter = regionalProcessingCenterMap.get(SSCS_LIVERPOOL);
        assertThat(regionalProcessingCenter.getName(), equalTo("LIVERPOOL"));
        assertThat(regionalProcessingCenter.getAddress1(), equalTo("HMCTS"));
        assertThat(regionalProcessingCenter.getAddress2(), equalTo("SSCS Appeals"));
        assertThat(regionalProcessingCenter.getAddress3(), equalTo("Prudential Buildings"));
        assertThat(regionalProcessingCenter.getAddress4(), equalTo("36 Dale Street"));
        assertThat(regionalProcessingCenter.getCity(), equalTo("Liverpool"));
        assertThat(regionalProcessingCenter.getPostcode(), equalTo("L2 5UZ"));
    }

    @Test
    public void shouldReturnRegionalProcessingCenterForGivenAppealReferenceNumber() {
        //Given
        String referenceNumber = "SC274/13/00010";
        regionalProcessingCenterService.init();

        //When
        RegionalProcessingCenter regionalProcessingCenter = regionalProcessingCenterService.getByScReferenceCode(referenceNumber);

        //Then
        assertThat(regionalProcessingCenter.getName(), equalTo("LIVERPOOL"));
        assertThat(regionalProcessingCenter.getAddress1(), equalTo("HMCTS"));
        assertThat(regionalProcessingCenter.getAddress2(), equalTo("SSCS Appeals"));
        assertThat(regionalProcessingCenter.getAddress3(), equalTo("Prudential Buildings"));
        assertThat(regionalProcessingCenter.getAddress4(), equalTo("36 Dale Street"));
        assertThat(regionalProcessingCenter.getCity(), equalTo("Liverpool"));
        assertThat(regionalProcessingCenter.getPostcode(), equalTo("L2 5UZ"));
    }

    @Test
    public void shouldReturnBirminghamRegionalProcessingCenterAsDefault() {

        //Given
        String referenceNumber = "SC000/13/00010";
        regionalProcessingCenterService.init();

        //When
        RegionalProcessingCenter regionalProcessingCenter = regionalProcessingCenterService.getByScReferenceCode(referenceNumber);

        //Then
        assertThat(regionalProcessingCenter.getName(), equalTo("BIRMINGHAM"));
        assertThat(regionalProcessingCenter.getAddress1(), equalTo("HMCTS"));
        assertThat(regionalProcessingCenter.getAddress2(), equalTo("SSCS Appeals"));
        assertThat(regionalProcessingCenter.getAddress3(), equalTo("Administrative Support Centre"));
        assertThat(regionalProcessingCenter.getAddress4(), equalTo("PO Box 14620"));
        assertThat(regionalProcessingCenter.getCity(), equalTo("Birmingham"));
        assertThat(regionalProcessingCenter.getPostcode(), equalTo("B16 6FR"));

    }

    @Test
    public void shouldReturnBirminghamRegionalProcessingCenterWhenCaseReferenceIsNull() {

        //Given
        regionalProcessingCenterService.init();

        //When
        RegionalProcessingCenter regionalProcessingCenter = regionalProcessingCenterService.getByScReferenceCode(null);

        //Then
        assertThat(regionalProcessingCenter.getName(), equalTo("BIRMINGHAM"));
        assertThat(regionalProcessingCenter.getAddress1(), equalTo("HMCTS"));
        assertThat(regionalProcessingCenter.getAddress2(), equalTo("SSCS Appeals"));
        assertThat(regionalProcessingCenter.getAddress3(), equalTo("Administrative Support Centre"));
        assertThat(regionalProcessingCenter.getAddress4(), equalTo("PO Box 14620"));
        assertThat(regionalProcessingCenter.getCity(), equalTo("Birmingham"));
        assertThat(regionalProcessingCenter.getPostcode(), equalTo("B16 6FR"));

    }
}