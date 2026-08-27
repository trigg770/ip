import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Entry point of the Ted chatbot.
 * Ted stores todos, deadlines and events, lists the stored tasks on request,
 * and can mark, unmark and delete them, until the user enters {@code bye}.
 * <p>
 * Ted itself now only wires the parts together and routes commands: the
 * conversation belongs to {@link Ui}, the tasks to {@link TaskList}, and the
 * save file to {@link Storage}.
 */
public class Ted {
    /** Where the tasks are kept between runs, relative to where Ted is run from. */
    private static final String DATA_FILE_PATH = "data/ted.txt";

    /** Format expected for date-times entered in deadline and event commands. */
    private static final DateTimeFormatter INPUT_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("d/M/uuuu HHmm");

    /** Separator introducing a deadline's due date and time. */
    private static final String OPTION_BY = "/by";

    /** Separator introducing an event's start time. */
    private static final String OPTION_FROM = "/from";

    /** Separator introducing an event's end time. */
    private static final String OPTION_TO = "/to";

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

        while (ui.hasNextCommand()) {
            String input = ui.readCommand();
            if (input.isBlank()) {
                // A stray blank line is not worth a reply.
                continue;
            }
            if (Command.isExit(input)) {
                break;
            }

            ui.showLine();
            try {
                handleCommand(input);
            } catch (TedException e) {
                // Every problem Ted can recognise is recoverable, so the message is
                // shown and the conversation continues with the next command.
                ui.showError(e.getMessage());
            }
            ui.showLine();
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
     * Carries out the command entered by the user.
     *
     * @param input full line of text entered by the user, already trimmed.
     * @throws TedException if the command is unknown or its details are unusable.
     */
    private void handleCommand(String input) throws TedException {
        // Splitting into at most two parts keeps the command word exact, so that
        // "todos" is not mistaken for "todo", while leaving the rest untouched.
        String[] parts = input.split(" ", 2);
        String argument = parts.length > 1 ? parts[1].trim() : "";
        Command command = Command.fromKeyword(parts[0]);

        switch (command) {
            case LIST:
                ui.showTasks(tasks);
                break;
            case MARK:
                setTaskDone(argument, true);
                break;
            case UNMARK:
                setTaskDone(argument, false);
                break;
            case TODO:
                addTodo(argument);
                break;
            case DEADLINE:
                addDeadline(argument);
                break;
            case EVENT:
                addEvent(argument);
                break;
            case DELETE:
                deleteTask(argument);
                break;
            default:
                // BYE is handled before parsing, so no other command can reach here.
                throw new TedException("I know \"" + command.getKeyword()
                        + "\", but I can't do it here.");
        }
    }

    /**
     * Adds a task with no date attached.
     *
     * @param description what the user wants to get done.
     * @throws TedException if the description is missing.
     */
    private void addTodo(String description) throws TedException {
        requireNotBlank(description, "A todo needs a description, for example: todo borrow book");
        addTask(new Todo(description));
    }

    /**
     * Adds a task due by a given date and time, from an argument of the form
     * {@code <description> /by <d/M/uuuu HHmm>}.
     *
     * @param argument everything the user typed after the command word.
     * @throws TedException if the description or due date-time is missing or unreadable.
     */
    private void addDeadline(String argument) throws TedException {
        String example = "for example: deadline return book /by 2/12/2019 1800";
        requireNotBlank(argument, "A deadline needs a description and a due time, " + example);

        int separator = argument.indexOf(OPTION_BY);
        if (separator == -1) {
            throw new TedException("I need to know when this is due. Use /by, " + example);
        }

        String description = argument.substring(0, separator).trim();
        String by = argument.substring(separator + OPTION_BY.length()).trim();
        requireNotBlank(description, "A deadline needs a description before /by, " + example);
        requireNotBlank(by, "A deadline needs a due time after /by, " + example);
        addTask(new Deadline(description, parseDateTime(by, example)));
    }

    /**
     * Adds a task spanning two date-times, from an argument of the form
     * {@code <description> /from <d/M/uuuu HHmm> /to <d/M/uuuu HHmm>}.
     *
     * @param argument everything the user typed after the command word.
     * @throws TedException if the description, the start or the end is missing
     *                      or unreadable, or the event ends before it starts.
     */
    private void addEvent(String argument) throws TedException {
        String example = "for example: event project meeting /from 2/12/2019 1400 /to 2/12/2019 1600";
        requireNotBlank(argument, "An event needs a description, a start and an end, " + example);

        int fromSeparator = argument.indexOf(OPTION_FROM);
        int toSeparator = argument.indexOf(OPTION_TO);
        if (fromSeparator == -1 || toSeparator == -1) {
            throw new TedException("An event needs both /from and /to, " + example);
        }
        if (toSeparator < fromSeparator) {
            throw new TedException("Please put /from before /to, " + example);
        }

        String description = argument.substring(0, fromSeparator).trim();
        String from = argument.substring(fromSeparator + OPTION_FROM.length(), toSeparator).trim();
        String to = argument.substring(toSeparator + OPTION_TO.length()).trim();
        requireNotBlank(description, "An event needs a description before /from, " + example);
        requireNotBlank(from, "An event needs a start time after /from, " + example);
        requireNotBlank(to, "An event needs an end time after /to, " + example);

        LocalDateTime start = parseDateTime(from, example);
        LocalDateTime end = parseDateTime(to, example);
        if (end.isBefore(start)) {
            // Now that the times are real date-times rather than free text, Ted can
            // spot an impossible event before it is stored.
            throw new TedException("An event cannot end before it starts, " + example);
        }
        addTask(new Event(description, start, end));
    }

    /**
     * Stores an already-built task and confirms it to the user.
     *
     * @param task the task to remember.
     * @throws TedException if the updated list cannot be saved.
     */
    private void addTask(Task task) throws TedException {
        tasks.add(task);
        ui.showAdded(task, tasks.size());
        storage.save(tasks);
    }

    /**
     * Removes the task at the given position and shows what was removed.
     *
     * @param argument task number as typed by the user, counting from 1.
     * @throws TedException if the task number is missing, not a number, or out of range.
     */
    private void deleteTask(String argument) throws TedException {
        Task removed = tasks.remove(parseTaskIndex(argument, Command.DELETE));
        ui.showRemoved(removed, tasks.size());
        storage.save(tasks);
    }

    /**
     * Sets the done status of the task at the given position and shows the result.
     * Marking and unmarking differ only in the stored flag and the wording, so
     * they share one method rather than duplicating the lookup logic.
     *
     * @param argument task number as typed by the user, counting from 1.
     * @param isDone   {@code true} to mark the task done, {@code false} to reverse it.
     * @throws TedException if the task number is missing, not a number, or out of range.
     */
    private void setTaskDone(String argument, boolean isDone) throws TedException {
        Task task = tasks.get(parseTaskIndex(argument, isDone ? Command.MARK : Command.UNMARK));
        if (isDone) {
            task.markAsDone();
        } else {
            task.markAsNotDone();
        }

        ui.showMarked(task, isDone);
        storage.save(tasks);
    }

    /**
     * Converts a task number typed by the user into an index into the task list.
     * Whether the number points at a real task is checked by {@link TaskList}.
     *
     * @param argument task number as typed by the user, counting from 1.
     * @param command  command the number was given to, used in the error message.
     * @return zero-based index of the task.
     * @throws TedException if the number is missing or is not a number.
     */
    private static int parseTaskIndex(String argument, Command command) throws TedException {
        String example = "for example: " + command.getKeyword() + " 2";
        requireNotBlank(argument, "Which task? Give me its number, " + example);

        try {
            // The user counts from 1, the list counts from 0.
            return Integer.parseInt(argument) - 1;
        } catch (NumberFormatException e) {
            // Rethrown as a TedException so the main loop handles every failure the same way.
            throw new TedException("\"" + argument + "\" is not a task number, " + example);
        }
    }

    /**
     * Turns a date and time typed by the user into a {@link LocalDateTime}.
     *
     * @param text    date and time as typed, e.g. {@code 2/12/2019 1800}.
     * @param example wording showing the expected format, used in the error message.
     * @return the date and time the text stands for.
     * @throws TedException if the text is not in the expected format.
     */
    private static LocalDateTime parseDateTime(String text, String example) throws TedException {
        try {
            return LocalDateTime.parse(text, INPUT_DATE_TIME_FORMAT);
        } catch (DateTimeParseException e) {
            // Rethrown as a TedException so the main loop reports it like any other
            // problem with the user's input, instead of crashing.
            throw new TedException("I can't read \"" + text + "\" as a date and time. "
                    + "Please use d/M/yyyy HHmm, " + example);
        }
    }

    /**
     * Rejects input the user left out.
     *
     * @param value   text entered by the user.
     * @param message explanation to show if the text is missing.
     * @throws TedException if {@code value} is empty or only whitespace.
     */
    private static void requireNotBlank(String value, String message) throws TedException {
        if (value.isBlank()) {
            throw new TedException(message);
        }
    }
}
