/**
 * Removes a task from the list.
 * The removed task is echoed back because once it is gone the user has no
 * other way to check that the number they typed was the one they meant.
 */
public class DeleteCommand extends Command {
    /** Zero-based position of the task to remove. */
    private final int index;

    /**
     * Creates a command that removes one task.
     *
     * @param index zero-based position of the task.
     */
    public DeleteCommand(int index) {
        this.index = index;
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws TedException {
        Task removed = tasks.remove(index);
        ui.showRemoved(removed, tasks.size());
        storage.save(tasks);
    }
}
