package ted.command;

import ted.Storage;
import ted.TedException;
import ted.Ui;
import ted.task.Task;
import ted.task.TaskList;

/**
 * Adds a task to the list.
 * <p>
 * One class covers todos, deadlines and events: by the time the command is
 * built, {@link ted.Parser} has already turned the user's words into the right
 * kind of {@link Task}, and adding it is the same work in every case.
 */
public class AddCommand extends Command {
    /** The task to remember. */
    private final Task task;

    /**
     * Creates a command that adds the given task.
     *
     * @param task the task to add.
     */
    public AddCommand(Task task) {
        this.task = task;
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws TedException {
        tasks.add(task);
        ui.showAdded(task, tasks.size());
        storage.save(tasks);
    }
}
