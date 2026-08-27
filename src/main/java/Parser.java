import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Turns a line typed by the user into the {@link Command} it stands for.
 * <p>
 * All of the fiddly work of reading user input lives here: splitting off the
 * command word, finding {@code /by}, {@code /from} and {@code /to}, and
 * reading dates. Because a command is only built once its details make sense,
 * the command classes themselves are free of input checking, and this class
 * can be tested without a keyboard or a save file.
 */
public class Parser {
    /** Format expected for date-times entered in deadline and event commands. */
    private static final DateTimeFormatter INPUT_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("d/M/uuuu HHmm");

    /** Separator introducing a deadline's due date and time. */
    private static final String OPTION_BY = "/by";

    /** Separator introducing an event's start time. */
    private static final String OPTION_FROM = "/from";

    /** Separator introducing an event's end time. */
    private static final String OPTION_TO = "/to";

    /**
     * Reads one line of user input as a command.
     *
     * @param input full line of text entered by the user, already trimmed.
     * @return the command the user asked for.
     * @throws TedException if the command is unknown or its details are unusable.
     */
    public static Command parse(String input) throws TedException {
        // Splitting into at most two parts keeps the command word exact, so that
        // "todos" is not mistaken for "todo", while leaving the rest untouched.
        String[] parts = input.split(" ", 2);
        String argument = parts.length > 1 ? parts[1].trim() : "";
        CommandType commandType = CommandType.fromKeyword(parts[0]);

        return switch (commandType) {
            case LIST -> new ListCommand();
            case MARK -> new MarkCommand(parseTaskIndex(argument, CommandType.MARK), true);
            case UNMARK -> new MarkCommand(parseTaskIndex(argument, CommandType.UNMARK), false);
            case DELETE -> new DeleteCommand(parseTaskIndex(argument, CommandType.DELETE));
            case TODO -> new AddCommand(parseTodo(argument));
            case DEADLINE -> new AddCommand(parseDeadline(argument));
            case EVENT -> new AddCommand(parseEvent(argument));
            case BYE -> new ExitCommand();
        };
    }

    /**
     * Reads a task with no date attached.
     *
     * @param description what the user wants to get done.
     * @return the new todo.
     * @throws TedException if the description is missing.
     */
    private static Todo parseTodo(String description) throws TedException {
        requireNotBlank(description, "A todo needs a description, for example: todo borrow book");
        return new Todo(description);
    }

    /**
     * Reads a task due by a given date and time, from an argument of the form
     * {@code <description> /by <d/M/uuuu HHmm>}.
     *
     * @param argument everything the user typed after the command word.
     * @return the new deadline.
     * @throws TedException if the description or due date-time is missing or unreadable.
     */
    private static Deadline parseDeadline(String argument) throws TedException {
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
        return new Deadline(description, parseDateTime(by, example));
    }

    /**
     * Reads a task spanning two date-times, from an argument of the form
     * {@code <description> /from <d/M/uuuu HHmm> /to <d/M/uuuu HHmm>}.
     *
     * @param argument everything the user typed after the command word.
     * @return the new event.
     * @throws TedException if the description, the start or the end is missing
     *                      or unreadable, or the event ends before it starts.
     */
    private static Event parseEvent(String argument) throws TedException {
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
        return new Event(description, start, end);
    }

    /**
     * Converts a task number typed by the user into an index into the task list.
     * Whether the number points at a real task is checked by {@link TaskList}
     * when the command runs, since only then is the list at hand.
     *
     * @param argument    task number as typed by the user, counting from 1.
     * @param commandType command the number was given to, used in the error message.
     * @return zero-based index of the task.
     * @throws TedException if the number is missing or is not a number.
     */
    private static int parseTaskIndex(String argument, CommandType commandType) throws TedException {
        String example = "for example: " + commandType.getKeyword() + " 2";
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
