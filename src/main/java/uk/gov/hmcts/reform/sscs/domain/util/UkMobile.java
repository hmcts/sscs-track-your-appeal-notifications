package uk.gov.hmcts.reform.sscs.domain.util;

import java.util.regex.Pattern;

public class UkMobile {

    private static final Pattern UK_MOBILE = Pattern.compile("(?:(?:\\+44[\\s]?)|(?:0[\\s]?))(?:7[\\s]?)(?:\\d[\\s]?){9}");

    private UkMobile() {
        // Void
    }

    public static boolean check(String value) {
        return value != null && UK_MOBILE.matcher(value).matches();
    }

}