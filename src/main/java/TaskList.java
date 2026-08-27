import java.util.ArrayList;
import java.util.List;

/**
 * The tasks Ted is keeping track of, in the order they were added.
 * <p>
 * Wrapping the list in its own class rather than passing an
 * {@code ArrayList<Task>} around means the rules about task numbers live in
 * one place: every lookup goes through {@link #requireValidIndex(int)}, so no
 * caller can reach past the end of the list.
 */
public class TaskList {
    /** The stored tasks. */
    private final ArrayList<Task> tasks;

    /** Creates an empty task list. */
    public TaskList() {
        this.tasks = new ArrayList<>();
    }

    /**
     * Creates a task list holding tasks that were already loaded, e.g. from disk.
     *
     * @param tasks the tasks to start with.
     */
    public TaskList(List<Task> tasks) {
        this.tasks = new ArrayList<>(tasks);
    }

    /**
     * Adds a task to the end of the list.
     *
     * @param task the task to remember.
     */
    public void add(Task task) {
        tasks.add(task);
    }

    /**
     * Removes the task at the given position.
     *
     * @param index zero-based position of the task.
     * @return the task that was removed.
     * @throws TedException if there is no task at that position.
     */
    public Task remove(int index) throws TedException {
        requireValidIndex(index);
        return tasks.remove(index);
    }

    /**
     * Returns the task at the given position.
     *
     * @param index zero-based position of the task.
     * @return the task at that position.
     * @throws TedException if there is no task at that position.
     */
    public Task get(int index) throws TedException {
        requireValidIndex(index);
        return tasks.get(index);
    }

    /**
     * Returns how many tasks are stored.
     *
     * @return the number of tasks.
     */
    public int size() {
        return tasks.size();
    }

    /**
     * Returns whether there are no tasks at all.
     *
     * @return {@code true} if the list is empty.
     */
    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    /**
     * Returns the tasks as a plain list, for code that only needs to read them.
     *
     * @return an unmodifiable view of the tasks, in order.
     */
    public List<Task> asList() {
        return List.copyOf(tasks);
    }

    /**
     * Rejects a task number that does not point at a stored task.
     * The message speaks in the 1-based numbers the user sees, not the 0-based
     * indices the list uses.
     *
     * @param index zero-based position the user asked for.
     * @throws TedException if there is no task at that position.
     */
    private void requireValidIndex(int index) throws TedException {
        int taskNumber = index + 1;
        if (tasks.isEmpty()) {
            throw new TedException("Your list is empty, so there is no task " + taskNumber + " yet.");
        }
        if (index < 0 || index >= tasks.size()) {
            throw new TedException("You don't have a task numbered " + taskNumber + ". "
                    + "Pick a number between 1 and " + tasks.size() + ".");
        }
    }
}
