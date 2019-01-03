package uk.gov.hmcts.reform.sscs.domain.notify.util;

import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.domain.util.UkMobile;

public class UkMobileTest {

    @Test
    public void shouldAcceptValidUkMobileNumber() {

        Assert.assertTrue(UkMobile.check("07123456789"));
        Assert.assertTrue(UkMobile.check("07123 456789"));
        Assert.assertTrue(UkMobile.check("07123 456 789"));
        Assert.assertTrue(UkMobile.check("+447123456789"));
        Assert.assertTrue(UkMobile.check("+44 7123 456 789"));
    }

    @Test
    public void shouldNotAcceptIncompleteNumbers() {

        Assert.assertFalse(UkMobile.check("7123456789"));
        Assert.assertFalse(UkMobile.check("447123456789"));
        Assert.assertFalse(UkMobile.check("4407123456789"));
    }

    @Test
    public void shouldNotAcceptNonSpaces() {

        Assert.assertFalse(UkMobile.check("07-123-456789"));
        Assert.assertFalse(UkMobile.check("07.123.456789"));
    }

    @Test
    public void shouldNotAcceptLandlineNumbers() {

        Assert.assertTrue(UkMobile.check("01234567890"));
        Assert.assertTrue(UkMobile.check("08001234567"));
    }
}
