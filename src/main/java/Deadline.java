/**
 * A task that must be done before a given point in time,
 * e.g. {@code submit report (by: Sunday)}.
 */
public class Deadline extends Task {
    /**
     * When the task is due.
     * Stored as free text because this increment does not require Ted to
     * understand dates; "Sunday" and "no idea :-p" are both acceptable.
     */
    protected String by;

    /**
     * Creates a deadline that is not done yet.
     *
     * @param description what the task is.
     * @param by          when the task is due, as typed by the user.
     */
    public Deadline(String description, String by) {
        super(description);
        this.by = by;
    }

    @Override
    public String getTypeIcon() {
        return "D";
    }

    /**
     * Appends the due time to the shared save-line format, e.g.
     * {@code D | 0 | Sunday | return book}.
     */
    @Override
    public String toSaveFormat() {
        return toSavePrefix() + encodeSaveField(by) + SAVE_FIELD_SEPARATOR
                + encodeSaveField(description);
    }

    /** Appends the due time to the shared task format. */
    @Override
    public String toString() {
        return super.toString() + " (by: " + by + ")";
    }
}
