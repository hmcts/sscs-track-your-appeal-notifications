package helper;

import java.time.Instant;

public class FunctionalTestHelper {

    private FunctionalTestHelper() {

    }

    public static String generateRandomCaseReference() {
        String epoch = String.valueOf(Instant.now().toEpochMilli());
        return "SC" + epoch.substring(3, 6)
                    + "/"
                    + epoch.substring(6, 8)
                    + "/"
                    + epoch.substring(8, 13);
    }
}
