package uk.gov.hmcts.sscs.domain.reminder;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Trigger {

    private String start_date_time;

    public Trigger(ZonedDateTime dateTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        this.start_date_time = dateTime.format(dateFormatter) + "T" + dateTime.format(timeFormatter) + dateTime.getOffset();
    }

    public String getStart_date_time() {
        return start_date_time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trigger trigger = (Trigger) o;
        return Objects.equals(start_date_time, trigger.start_date_time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start_date_time);
    }
}