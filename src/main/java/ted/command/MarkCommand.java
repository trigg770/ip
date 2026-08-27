package ted.command;

import ted.Storage;
import ted.TedException;
import ted.Ui;
import ted.task.Task;
import ted.task.TaskList;

/**
 * Marks a task as done, or reverses that.
 * Marking and unmarking differ only in the stored flag and the wording, so one
 * class covers both rather than duplicating the lookup.
 */
public class MarkCommand extends Command {
    /** Zero-based position of the task to change. */
    private final int index;

    /** {@code true} to mark the task done, {@code false} to reverse it. */
    private final boolean isDone;

    /**
     * Creates a command that changes one task's done status.
     *
     * @param index  zero-based position of the task.
     * @param isDone {@code true} to mark it done, {@code false} to reverse it.
     */
    public MarkCommand(int index, boolean isDone) {
        this.index = index;
        this.isDone = isDone;
    }

    /**
     * Changes the task's done status, shows the result, and saves the list.
     *
     * @throws TedException if no task has that number, or the list cannot be saved.
     */
    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws TedException {
        Task task = tasks.get(index);
        if (isDone) {
            task.markAsDone();
        } else {
            task.markAsNotDone();
        }

        ui.showMarked(task, isDone);
        storage.save(tasks);
    }
}
