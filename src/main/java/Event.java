/**
 * A task that runs from one point in time to another,
 * e.g. {@code project meeting (from: Mon 2pm to: 4pm)}.
 */
public class Event extends Task {
    /** When the event starts, as typed by the user. */
    protected String from;

    /** When the event ends, as typed by the user. */
    protected String to;

    /**
     * Creates an event that is not done yet.
     *
     * @param description what the event is.
     * @param from        when the event starts, as typed by the user.
     * @param to          when the event ends, as typed by the user.
     */
    public Event(String description, String from, String to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    @Override
    public String getTypeIcon() {
        return "E";
    }

    /**
     * Appends the start and end times to the shared save-line format, e.g.
     * {@code E | 0 | Mon 2pm | 4pm | project meeting}.
     */
    @Override
    public String toSaveFormat() {
        return toSavePrefix() + encodeSaveField(from) + SAVE_FIELD_SEPARATOR
                + encodeSaveField(to) + SAVE_FIELD_SEPARATOR
                + encodeSaveField(description);
    }

    /** Appends the start and end times to the shared task format. */
    @Override
    public String toString() {
        return super.toString() + " (from: " + from + " to: " + to + ")";
    }
}
