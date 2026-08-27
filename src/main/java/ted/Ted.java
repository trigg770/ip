package ted;

import ted.command.Command;
import ted.task.TaskList;

/**
 * Entry point of the Ted chatbot.
 * Ted stores todos, deadlines and events, lists the stored tasks on request,
 * and can mark, unmark and delete them, until the user enters {@code bye}.
 * <p>
 * This class only wires the parts together and runs the conversation loop: the
 * talking belongs to {@link Ui}, the tasks to {@link TaskList}, the save file
 * to {@link Storage}, reading the user's words to {@link Parser}, and the work
 * itself to the {@link Command} classes.
 */
public class Ted {
    /** Where the tasks are kept between runs, relative to where Ted is run from. */
    private static final String DATA_FILE_PATH = "data/ted.txt";

    /** Handles what Ted says and what the user types. */
    private final Ui ui;

    /** Loads the tasks at startup and saves them after every change. */
    private final Storage storage;

    /** The tasks entered so far. */
    private TaskList tasks;

    /**
     * Creates a Ted whose tasks are kept in the given file, restoring whatever
     * was saved there previously.
     *
     * @param filePath where to keep the tasks, e.g. {@code data/ted.txt}.
     */
    public Ted(String filePath) {
        ui = new Ui();
        storage = new Storage(filePath);
        try {
            // The list must be complete before the user is greeted, so that every
            // command that follows can rely on it.
            tasks = new TaskList(storage.load());
            if (storage.getSkippedLineCount() > 0) {
                ui.showSkippedLines(storage.getSkippedLineCount());
            }
        } catch (TedException e) {
            // An unreadable save file is not worth refusing to start over.
            ui.showLoadingError(e.getMessage());
            tasks = new TaskList();
        }
    }

    /** Greets the user, then handles commands until the conversation ends. */
    public void run() {
        ui.showWelcome();

        boolean isExit = false;
        // hasNextCommand() also stops the loop when the input stream ends,
        // e.g. on Ctrl-D or at the end of a piped file.
        while (!isExit && ui.hasNextCommand()) {
            String input = ui.readCommand();
            if (input.isBlank()) {
                // A stray blank line is not worth a reply.
                continue;
            }

            Command command;
            try {
                command = Parser.parse(input);
            } catch (TedException e) {
                // Every problem Ted can recognise is recoverable, so the message is
                // shown and the conversation continues with the next command.
                showFramed(() -> ui.showError(e.getMessage()));
                continue;
            }

            isExit = command.isExit();
            if (isExit) {
                // Quitting gets the goodbye below rather than an ordinary reply.
                break;
            }

            showFramed(() -> {
                try {
                    command.execute(tasks, ui, storage);
                } catch (TedException e) {
                    ui.showError(e.getMessage());
                }
            });
        }

        ui.showGoodbye();
    }

    /**
     * Starts Ted.
     *
     * @param args command-line arguments, which Ted does not use.
     */
    public static void main(String[] args) {
        new Ted(DATA_FILE_PATH).run();
    }

    /**
     * Runs a reply between the two divider lines, so that every reply is framed
     * the same way whether it succeeded or reported a problem.
     *
     * @param reply the printing to frame.
     */
    private void showFramed(Runnable reply) {
        ui.showLine();
        reply.run();
        ui.showLine();
    }
}
