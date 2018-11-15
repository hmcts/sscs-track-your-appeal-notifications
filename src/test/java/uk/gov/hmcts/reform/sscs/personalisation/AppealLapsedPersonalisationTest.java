package uk.gov.hmcts.reform.sscs.personalisation;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.AppConstants;

@RunWith(JUnitParamsRunner.class)
public class AppealLapsedPersonalisationTest {

    private static final String CASE_ID = "54321";

    private SscsCaseData response;

    private WithRepresentativePersonalisation withRepresentativePersonalisation =
        new WithRepresentativePersonalisation();

    @Test
    @Parameters(method = "generateSscsCaseDataForTest")
    public void givenSyaAppealCreated_shouldSetRepresentativeNameIfPresent(
            SscsCaseData sscsCaseData, String expected) {
        Map<String, String> personalisation = withRepresentativePersonalisation.setRepresentativeName(
                new HashMap<>(), sscsCaseData);
        assertEquals(expected, personalisation.get(AppConstants.REPRESENTATIVE_NAME));
    }

    private Object[] generateSscsCaseDataForTest() {
        SscsCaseData sscsCaseDataWithReps = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder()
                    .name(Name.builder()
                        .firstName("Manish")
                        .lastName("Sharma")
                        .title("Mrs")
                        .build())
                    .build())
                .build())
            .build();
        SscsCaseData sscsCaseDataWithNoReps = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(null)
                .build())
            .build();
        SscsCaseData sscsCaseDataWithEmptyReps = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder().build())
                .build())
            .build();
        SscsCaseData sscsCaseDataWithEmptyRepsAndEmptyNames = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder()
                    .name(Name.builder().build())
                    .build())
                .build())
            .build();
        return new Object[]{
            new Object[]{sscsCaseDataWithReps, "Manish Sharma"},
            new Object[]{sscsCaseDataWithNoReps, null},
            new Object[]{sscsCaseDataWithEmptyReps, null},
            new Object[]{sscsCaseDataWithEmptyRepsAndEmptyNames, "null null"}
        };
    }
}
