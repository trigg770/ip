package ted.command;

import ted.Storage;
import ted.Ui;
import ted.task.TaskList;

/** Ends the conversation. */
public class ExitCommand extends Command {
    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) {
        // Nothing to do: the goodbye is shown by Ted once the loop has stopped,
        // so that it is not framed like an ordinary reply.
    }

    @Override
    public boolean isExit() {
        return true;
    }
}
