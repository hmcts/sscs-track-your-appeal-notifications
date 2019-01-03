package uk.gov.hmcts.reform.sscs.domain.util;

import java.util.regex.Pattern;

public class UkMobile {

    private static final Pattern UK_MOBILE = Pattern.compile("(?:(?:(\\+44[\\s]?)|(?:0[\\s]?))(?:\\d[\\s]?){10})");

    private UkMobile() {
        // Void
    }

    public static boolean check(String value) {
        return value != null && UK_MOBILE.matcher(value).matches();
    }

}