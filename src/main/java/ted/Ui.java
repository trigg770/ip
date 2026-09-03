package ted;

import java.util.List;
import java.util.Scanner;

import ted.task.Task;
import ted.task.TaskList;

/**
 * Handles everything Ted says to the user and everything the user types back.
 * <p>
 * Keeping all of the wording in one class means the rest of Ted never calls
 * {@code System.out} directly: the wording of a message can change, or the
 * whole conversation can be moved to a window instead of a terminal, by
 * editing this class alone.
 * <p>
 * Each {@code show} method adds to the reply being built rather than printing
 * it. {@link #flush()} hands the finished reply back, so the terminal can print
 * it while the GUI puts it in a dialog box.
 */
public class Ui {
    /** Horizontal rule framing each of Ted's replies. */
    private static final String DIVIDER = "____________________________________________________________";

    /** Ted's name in ASCII art, shown once at startup. */
    private static final String BANNER = " _____ _____ ____  \n"
            + "|_   _| ____|  _ \\ \n"
            + "  | | |  _| | | | |\n"
            + "  | | | |___| |_| |\n"
            + "  |_| |_____|____/ \n";

    /** Reads the user's commands from standard input, one line at a time. */
    private final Scanner scanner = new Scanner(System.in);

    /** The reply being built, emptied by each call to {@link #flush()}. */
    private final StringBuilder reply = new StringBuilder();

    /** Creates a Ui with nothing said yet. */
    public Ui() {
    }

    /**
     * Returns whether the user has typed another line.
     * Guards against the input stream ending (e.g. Ctrl-D or a piped file),
     * which would otherwise make {@link #readCommand()} throw.
     *
     * @return {@code true} if another command can be read.
     */
    public boolean hasNextCommand() {
        return scanner.hasNextLine();
    }

    /**
     * Reads the next command typed by the user.
     *
     * @return the line the user typed, with surrounding spaces removed.
     */
    public String readCommand() {
        return scanner.nextLine().trim();
    }

    /**
     * Returns everything said since the previous call and starts a fresh reply.
     *
     * @return the finished reply, without a trailing newline.
     */
    public String flush() {
        String finishedReply = reply.toString().strip();
        reply.setLength(0);
        return finishedReply;
    }

    /**
     * Prints a finished reply to the terminal, framed by horizontal rules.
     * Used by the text interface only; the GUI shows the same text in a
     * dialog box, where the rules would be clutter.
     *
     * @param finishedReply the reply to print, as returned by {@link #flush()}.
     */
    public void printReply(String finishedReply) {
        System.out.println(DIVIDER);
        System.out.println(finishedReply);
        System.out.println(DIVIDER);
    }

    /** Greets the user at startup. */
    public void showWelcome() {
        show(BANNER + "Hello! I'm Ted.");
        show("What can I do for you?");
    }

    /** Says goodbye just before Ted stops. */
    public void showGoodbye() {
        show("Bye. Hope to see you again soon!");
    }

    /**
     * Reports a problem Ted can recover from.
     *
     * @param message explanation of what went wrong, phrased as Ted would say it.
     */
    public void showError(String message) {
        show(message);
    }

    /**
     * Warns that the saved tasks could not be read, so Ted starts empty.
     *
     * @param message explanation of what went wrong.
     */
    public void showLoadingError(String message) {
        show("I couldn't read your saved tasks (" + message + ").");
        show("Starting with an empty list.");
    }

    /**
     * Warns that some lines of the save file were not in the expected format.
     *
     * @param skippedLineCount how many lines were skipped.
     */
    public void showSkippedLines(int skippedLineCount) {
        String lineWord = skippedLineCount == 1 ? "line" : "lines";
        show("Skipped " + skippedLineCount + " unreadable " + lineWord
                + " in your save file.");
    }

    /**
     * Confirms a newly added task.
     *
     * @param task      the task that was added.
     * @param taskCount how many tasks are now stored.
     */
    public void showAdded(Task task, int taskCount) {
        show("Got it. I've added this task:");
        show("  " + task);
        showTaskCount(taskCount);
    }

    /**
     * Confirms a deleted task. The task is echoed because once it is gone the
     * user has no other way to check that the number they typed was the one
     * they meant.
     *
     * @param task      the task that was removed.
     * @param taskCount how many tasks are left.
     */
    public void showRemoved(Task task, int taskCount) {
        show("Noted. I've removed this task:");
        show("  " + task);
        showTaskCount(taskCount);
    }

    /**
     * Confirms a change to a task's done status.
     *
     * @param task   the task whose status changed.
     * @param isDone {@code true} if the task was marked done, {@code false} if reversed.
     */
    public void showMarked(Task task, boolean isDone) {
        show(isDone
                ? "Nice! I've marked this task as done:"
                : "OK, I've marked this task as not done yet:");
        show("  " + task);
    }

    /**
     * Prints every stored task as a numbered list, starting from 1.
     *
     * @param tasks the tasks to show.
     */
    public void showTasks(TaskList tasks) {
        showNumbered(tasks, "Here are the tasks in your list:", "You have no tasks yet.");
    }

    /**
     * Prints the tasks that matched a search, as a numbered list.
     * The numbers count the matches, not the positions in the full list, so
     * they are not the numbers to pass to mark or delete.
     *
     * @param matches the tasks that matched.
     * @param keyword what the user searched for, repeated back when nothing matched.
     */
    public void showMatchingTasks(TaskList matches, String keyword) {
        showNumbered(matches, "Here are the matching tasks in your list:",
                "No task matches \"" + keyword + "\".");
    }

    /**
     * Prints tasks as a numbered list, starting from 1.
     *
     * @param tasks        the tasks to show.
     * @param header       line introducing the list.
     * @param emptyMessage line to show instead when there are no tasks.
     */
    private void showNumbered(TaskList tasks, String header, String emptyMessage) {
        if (tasks.isEmpty()) {
            show(emptyMessage);
            return;
        }

        show(header);
        List<Task> taskList = tasks.asList();
        for (int i = 0; i < taskList.size(); i++) {
            // Displayed numbering is 1-based even though list indices are 0-based.
            show((i + 1) + "." + taskList.get(i));
        }
    }

    /** Adds one line to the reply being built. */
    private void show(String line) {
        reply.append(line).append(System.lineSeparator());
    }

    /** Tells the user how many tasks are now stored. */
    private void showTaskCount(int taskCount) {
        String taskWord = taskCount == 1 ? "task" : "tasks";
        show("Now you have " + taskCount + " " + taskWord + " in the list.");
    }
}
