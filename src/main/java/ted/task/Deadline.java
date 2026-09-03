package ted.task;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A task that must be done before a given point in time,
 * e.g. {@code submit report (by: 2 Dec 2019, 6:00 PM)}.
 */
public class Deadline extends Task {
    /** Format used to present the due date and time to the user. */
    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMM uuuu, h:mm a");

    /** When the task is due, represented as a date and time. */
    protected LocalDateTime by;

    /**
     * Creates a deadline that is not done yet.
     *
     * @param description what the task is.
     * @param by          when the task is due.
     */
    public Deadline(String description, LocalDateTime by) {
        super(description);
        this.by = by;
    }

    /** {@inheritDoc} A deadline is shown as {@code D}. */
    @Override
    public String getTypeIcon() {
        return "D";
    }

    /**
     * Appends the due date and time in ISO format to the shared save-line
     * format, e.g. {@code D | 0 | 2019-12-02T18:00 | return book}.
     */
    @Override
    public String toSaveFormat() {
        return toSaveLine(by.toString());
    }

    /** Appends a user-friendly due date and time to the shared task format. */
    @Override
    public String toString() {
        return super.toString() + " (by: " + by.format(DISPLAY_DATE_TIME_FORMAT) + ")";
    }
}
