/**
 * A single task that Ted keeps track of.
 * Bundles a task's description with its done status, so that the two can no
 * longer drift apart the way two parallel arrays could.
 * <p>
 * This class is abstract because every task Ted stores is one of the specific
 * kinds ({@link Todo}, {@link Deadline}, {@link Event}); a plain "task" with no
 * kind is not something the user can create. Subclasses supply their own type
 * icon and add any extra detail to {@link #toString()}.
 */
public abstract class Task {
    /** What the user wants to get done. */
    protected String description;

    /** Whether the task has been completed. */
    protected boolean isDone;

    /**
     * Creates a task that is not done yet.
     *
     * @param description what the user wants to get done.
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    /**
     * Returns the icon shown in place of a tick box.
     *
     * @return {@code "X"} if the task is done, a single space otherwise.
     */
    public String getStatusIcon() {
        return isDone ? "X" : " "; // mark done task with X
    }

    /** Marks this task as completed. */
    public void markAsDone() {
        this.isDone = true;
    }

    /** Marks this task as not completed, reversing {@link #markAsDone()}. */
    public void markAsNotDone() {
        this.isDone = false;
    }

    /**
     * Returns the one-letter icon identifying this kind of task, e.g. {@code "T"}.
     *
     * @return the type icon shown before the status icon.
     */
    public abstract String getTypeIcon();

    /**
     * Returns this task as it should appear to the user, e.g. {@code [T][X] read book}.
     * Overriding {@code toString} rather than writing a separate format method
     * lets a task be printed directly wherever it is needed. Subclasses that
     * carry extra detail append it to this result.
     */
    @Override
    public String toString() {
        return "[" + getTypeIcon() + "][" + getStatusIcon() + "] " + description;
    }
}
