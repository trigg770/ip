/**
 * A single task that Ted keeps track of.
 * Bundles a task's description with its done status, so that the two can no
 * longer drift apart the way two parallel arrays could.
 */
public class Task {
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
     * Returns this task as it should appear to the user, e.g. {@code [X] read book}.
     * Overriding {@code toString} rather than writing a separate format method
     * lets a task be printed directly wherever it is needed.
     */
    @Override
    public String toString() {
        return "[" + getStatusIcon() + "] " + description;
    }
}
