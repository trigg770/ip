package ted.task;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A task that runs from one point in time to another,
 * e.g. {@code project meeting (from: 2 Dec 2019, 2:00 PM to: 2 Dec 2019, 4:00 PM)}.
 */
public class Event extends Task {
    /** Format used to present the start and end date-times to the user. */
    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMM uuuu, h:mm a");

    /** When the event starts. */
    protected LocalDateTime from;

    /** When the event ends. */
    protected LocalDateTime to;

    /**
     * Creates an event that is not done yet.
     *
     * @param description what the event is.
     * @param from        when the event starts.
     * @param to          when the event ends.
     */
    public Event(String description, LocalDateTime from, LocalDateTime to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    /** {@inheritDoc} An event is shown as {@code E}. */
    @Override
    public String getTypeIcon() {
        return "E";
    }

    /**
     * Appends the start and end date-times in ISO format to the shared save-line
     * format, e.g. {@code E | 0 | 2019-12-02T14:00 | 2019-12-02T16:00 | project meeting}.
     */
    @Override
    public String toSaveFormat() {
        return toSavePrefix() + encodeSaveField(from.toString()) + SAVE_FIELD_SEPARATOR
                + encodeSaveField(to.toString()) + SAVE_FIELD_SEPARATOR
                + encodeSaveField(description);
    }

    /** Appends user-friendly start and end date-times to the shared task format. */
    @Override
    public String toString() {
        return super.toString() + " (from: " + from.format(DISPLAY_DATE_TIME_FORMAT) + " to: "
                + to.format(DISPLAY_DATE_TIME_FORMAT) + ")";
    }
}
