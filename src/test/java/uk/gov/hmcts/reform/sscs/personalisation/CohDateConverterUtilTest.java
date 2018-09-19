package uk.gov.hmcts.reform.sscs.personalisation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

public class CohDateConverterUtilTest {
    @Test
    public void doesNotAdd0ToStartOfDate() {
        String emailDate = new CohDateConverterUtil().toEmailDate("2018-09-01T23:59:59Z");
        assertThat(emailDate, is("1 September 2018"));
    }

    @Test
    public void dateInMonthIs2Digits() {
        String emailDate = new CohDateConverterUtil().toEmailDate("2018-09-12T23:59:59Z");
        assertThat(emailDate, is("12 September 2018"));
    }

}