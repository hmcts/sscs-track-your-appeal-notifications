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
public class WithRepresentativePersonalisationTest {

    private WithRepresentativePersonalisation withRepresentativePersonalisation =
        new WithRepresentativePersonalisation();

    @Test
    @Parameters(method = "generateSscsCaseDataForTest")
    public void givenSscsCaseData_shouldSetRepresentativeNameIfPresent(
            SscsCaseData sscsCaseData, String expected) {
        Map<String, String> personalisation = withRepresentativePersonalisation.setRepresentativeName(
                new HashMap<>(), sscsCaseData);
        assertEquals(expected, personalisation.get(AppConstants.REPRESENTATIVE_NAME));
    }

    @Test
    @Parameters(method = "generateRepsForTest")
    public void isValidRepsTest(Representative rep, Boolean expected) {
        assertEquals(expected, withRepresentativePersonalisation.isValidReps(rep));
    }

    @SuppressWarnings({"unused"})
    private Object[] generateSscsCaseDataForTest() {
        SscsCaseData sscsCaseDataWithRepsFlagYes = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder()
                    .hasRepresentative("yes")
                    .name(Name.builder()
                        .firstName("Manish")
                        .lastName("Sharma")
                        .title("Mrs")
                        .build())
                    .build())
                .build())
            .build();
        SscsCaseData sscsCaseDataWithRepsFlagNo = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder()
                    .hasRepresentative("no")
                    .name(Name.builder()
                        .firstName("Manish")
                        .lastName("Sharma")
                        .title("Mrs")
                        .build())
                    .build())
                .build())
            .build();
        SscsCaseData sscsCaseDataWithRepsOrgOnlyFlagYes = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder()
                    .hasRepresentative("yes")
                    .name(Name.builder().build())
                        .organisation("organisation")
                        .build())
                .build())
            .build();
        SscsCaseData sscsCaseDataWithRepsOrgOnlyFlagNo = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder()
                    .hasRepresentative("no")
                    .name(Name.builder().build())
                        .organisation("organisation")
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
        SscsCaseData sscsCaseDataWithEmptyRepsAndEmptyNamesFlagYes = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(Representative.builder()
                    .hasRepresentative("yes")
                    .name(Name.builder().build())
                    .build())
                .build())
            .build();
        return new Object[]{
            new Object[]{sscsCaseDataWithRepsFlagYes, "Manish Sharma"},
            new Object[]{sscsCaseDataWithRepsFlagNo, null},
            new Object[]{sscsCaseDataWithRepsOrgOnlyFlagYes, AppConstants.REP_SALUTATION},
            new Object[]{sscsCaseDataWithRepsOrgOnlyFlagNo, null},
            new Object[]{sscsCaseDataWithNoReps, null},
            new Object[]{sscsCaseDataWithEmptyReps, null},
            new Object[]{sscsCaseDataWithEmptyRepsAndEmptyNamesFlagYes, null}
        };
    }

    @SuppressWarnings({"unused"})
    private Object[] generateRepsForTest() {
        Representative nameNullOrgNull = Representative.builder().build();
        
        Representative nameFieldsNullOrgNull = Representative.builder().name(
                                                    Name.builder().build()
                                                ).build();

        Representative nameFieldsNullOrgBlank = Representative.builder().name(
                                                    Name.builder().build()
                                                ).organisation("").build();

        Representative nameFieldsNullOrgStr = Representative.builder().name(
                                                    Name.builder().build()
                                                ).organisation("o").build();

        Representative nameFieldsBlankOrgNull = Representative.builder().name(
                                                    Name.builder().firstName("").lastName("").build()
                                                ).build();

        Representative nameFieldsBlankOrgBlank = Representative.builder().name(
                                                    Name.builder().firstName("").lastName("").build()
                                                ).organisation("").build();

        Representative nameFieldsBlankOrgStr = Representative.builder().name(
                                                    Name.builder().firstName("").lastName("").build()
                                                ).organisation("o").build();

        Representative firstNameBlankLastNameNullOrgNull = Representative.builder().name(
                                                    Name.builder().firstName("").build()
                                                ).build();

        Representative firstNameBlankLastNameNullOrgBlank = Representative.builder().name(
                                                    Name.builder().firstName("").build()
                                                ).organisation("").build();

        Representative firstNameBlankLastNameNullOrgStr = Representative.builder().name(
                                                    Name.builder().firstName("").build()
                                                ).organisation("o").build();

        Representative firstNameStrLastNameNullOrgNull = Representative.builder().name(
                                                    Name.builder().firstName("f").build()
                                                ).build();

        Representative firstNameStrLastNameNullOrgBlank = Representative.builder().name(
                                                    Name.builder().firstName("f").build()
                                                ).organisation("").build();

        Representative firstNameStrLastNameNullOrgStr = Representative.builder().name(
                                                    Name.builder().firstName("f").build()
                                                ).organisation("o").build();

        Representative firstNameNullLastNameBlankOrgNull = Representative.builder().name(
                                                    Name.builder().lastName("").build()
                                                ).build();

        Representative firstNameNullLastNameBlankOrgBlank = Representative.builder().name(
                                                    Name.builder().lastName("").build()
                                                ).organisation("").build();

        Representative firstNameNullLastNameBlankOrgStr = Representative.builder().name(
                                                    Name.builder().lastName("").build()
                                                ).organisation("o").build();

        Representative firstNameNullLastNameStrOrgNull = Representative.builder().name(
                                                    Name.builder().lastName("l").build()
                                                ).build();

        Representative firstNameNullLastNameStrOrgBlank = Representative.builder().name(
                                                    Name.builder().lastName("l").build()
                                                ).organisation("").build();

        Representative firstNameNullLastNameStrOrgStr = Representative.builder().name(
                                                    Name.builder().lastName("l").build()
                                                ).organisation("o").build();

        Representative namesStrOrgNull = Representative.builder().name(
                                                    Name.builder().firstName("f").lastName("l").build()
                                                ).build();

        Representative namesStrOrgBlank = Representative.builder().name(
                                                    Name.builder().firstName("f").lastName("l").build()
                                                ).organisation("").build();

        Representative namesStrOrgStr = Representative.builder().name(
                                                    Name.builder().firstName("f").lastName("l").build()
                                                ).organisation("o").build();

        return new Object[]{
            new Object[]{null, false},
            new Object[]{nameNullOrgNull, false},
            new Object[]{nameFieldsNullOrgNull, false},
            new Object[]{nameFieldsNullOrgBlank, false},
            new Object[]{nameFieldsNullOrgStr, true},
            new Object[]{nameFieldsBlankOrgNull, false},
            new Object[]{nameFieldsBlankOrgBlank, false},
            new Object[]{nameFieldsBlankOrgStr, true},
            new Object[]{firstNameBlankLastNameNullOrgNull, false},
            new Object[]{firstNameBlankLastNameNullOrgBlank, false},
            new Object[]{firstNameBlankLastNameNullOrgStr, true},
            new Object[]{firstNameStrLastNameNullOrgNull, false},
            new Object[]{firstNameStrLastNameNullOrgBlank, false},
            new Object[]{firstNameStrLastNameNullOrgStr, true},
            new Object[]{firstNameNullLastNameBlankOrgNull, false},
            new Object[]{firstNameNullLastNameBlankOrgBlank, false},
            new Object[]{firstNameNullLastNameBlankOrgStr, true},
            new Object[]{firstNameNullLastNameStrOrgNull, false},
            new Object[]{firstNameNullLastNameStrOrgBlank, false},
            new Object[]{firstNameNullLastNameStrOrgStr, true},
            new Object[]{namesStrOrgNull, true},
            new Object[]{namesStrOrgBlank, true},
            new Object[]{namesStrOrgStr, true},
        };
    }
}
