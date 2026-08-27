package ted.command;

import ted.Storage;
import ted.Ui;
import ted.task.TaskList;

/** Shows every stored task as a numbered list. */
public class ListCommand extends Command {
    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) {
        ui.showTasks(tasks);
    }
}
