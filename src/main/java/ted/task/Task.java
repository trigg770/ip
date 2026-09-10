package ted.task;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
    /** Separator between fields in the save file. */
    public static final String SAVE_FIELD_SEPARATOR = " | ";

    /** What the user wants to get done. */
    protected String description;

    /** Whether the task has been completed. */
    protected boolean isDone;

    /**
     * Labels the user has attached to this task, in the order they were added.
     * A set, so that attaching a tag the task already has changes nothing.
     */
    private final Set<Tag> tags = new LinkedHashSet<>();

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
        // A cross for a finished task, a blank for one still to do.
        return isDone ? "X" : " ";
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
     * Attaches tags to this task. Tags it already has are left where they are,
     * so no tag is ever shown twice.
     *
     * @param newTags the tags to attach.
     */
    public void addTags(Collection<Tag> newTags) {
        tags.addAll(newTags);
    }

    /**
     * Detaches tags from this task. Tags it does not have are ignored.
     *
     * @param oldTags the tags to detach.
     */
    public void removeTags(Collection<Tag> oldTags) {
        tags.removeAll(oldTags);
    }

    /**
     * Returns whether this task has the given tag.
     *
     * @param tag the tag to look for.
     * @return {@code true} if the tag is attached to this task.
     */
    public boolean hasTag(Tag tag) {
        return tags.contains(tag);
    }

    /**
     * Converts this task into a single line of the save file format.
     * <p>
     * The line carries everything needed to rebuild this task: the type icon
     * ({@link #getTypeIcon()}), the done flag, any tags, the description, and
     * any type-specific detail. Todo uses the default implementation; subclasses
     * override it to pass their extra fields to {@link #toSaveLine(String...)}.
     *
     * @return one line of the save file, using {@code " | "} as the separator.
     */
    public String toSaveFormat() {
        return toSaveLine();
    }

    /**
     * Builds one save line from the fields every task shares and the extra
     * fields this kind of task adds.
     * <p>
     * The number of extra fields differs per task type -- a todo has none, a
     * deadline one, an event two -- so they are taken as varargs. Each caller
     * then names its own fields in order and this method alone deals with
     * escaping them and placing the separators.
     * <p>
     * Tags, if there are any, form one field straight after the done flag, e.g.
     * {@code T | 0 | #fun #school | read book}. A task without tags leaves the
     * field out, so its line is exactly what Ted wrote before tags existed.
     *
     * @param extraFields type-specific fields, in the order they are saved,
     *                    stored between the done flag and the description.
     * @return one line of the save file, using {@code " | "} as the separator.
     */
    protected String toSaveLine(String... extraFields) {
        StringBuilder line = new StringBuilder();
        line.append(getTypeIcon()).append(SAVE_FIELD_SEPARATOR)
                .append(isDone ? "1" : "0").append(SAVE_FIELD_SEPARATOR);
        if (!tags.isEmpty()) {
            // Tags need no escaping: after the # they are only letters and digits.
            line.append(formatTags()).append(SAVE_FIELD_SEPARATOR);
        }
        for (String field : extraFields) {
            line.append(encodeSaveField(field)).append(SAVE_FIELD_SEPARATOR);
        }
        // The description goes last, where it can safely contain separators.
        return line.append(encodeSaveField(description)).toString();
    }

    /**
     * Escapes characters that have a structural meaning in a save line.
     * Backslashes are escaped first so that decoding can distinguish a literal
     * backslash from one that protects a pipe character.
     *
     * @param field one task field to store.
     * @return the field with backslashes and pipe characters escaped.
     */
    protected static String encodeSaveField(String field) {
        return field.replace("\\", "\\\\").replace("|", "\\|");
    }

    /**
     * Returns the one-letter icon identifying this kind of task, e.g. {@code "T"}.
     *
     * @return the type icon shown before the status icon.
     */
    public abstract String getTypeIcon();

    /**
     * Returns this task as it should appear to the user, e.g. {@code [T][X] read book #fun}.
     * Overriding {@code toString} rather than writing a separate format method
     * lets a task be printed directly wherever it is needed. Subclasses that
     * carry extra detail append it to this result, so a deadline's due date
     * comes after its tags.
     */
    @Override
    public String toString() {
        String shown = "[" + getTypeIcon() + "][" + getStatusIcon() + "] " + description;
        return tags.isEmpty() ? shown : shown + " " + formatTags();
    }

    /**
     * Returns the tags the way they are both shown and saved, e.g. {@code #fun #school}.
     */
    private String formatTags() {
        return tags.stream()
                .map(Tag::toString)
                .collect(Collectors.joining(" "));
    }
}
