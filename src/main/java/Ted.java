import java.util.Scanner;

/**
 * Entry point of the Ted chatbot.
 * Ted stores todos, deadlines and events, lists the stored tasks on request,
 * and can mark a task as done or not done, until the user enters {@code bye}.
 */
public class Ted {
    /** Command that ends the conversation. */
    private static final String COMMAND_EXIT = "bye";

    /** Command that displays every stored task. */
    private static final String COMMAND_LIST = "list";

    /** Command that marks a task as done, e.g. {@code mark 2}. */
    private static final String COMMAND_MARK = "mark";

    /** Command that reverses the done status of a task, e.g. {@code unmark 2}. */
    private static final String COMMAND_UNMARK = "unmark";

    /** Command that adds a task with no date attached, e.g. {@code todo borrow book}. */
    private static final String COMMAND_TODO = "todo";

    /** Command that adds a task due by a given time, e.g. {@code deadline report /by Sunday}. */
    private static final String COMMAND_DEADLINE = "deadline";

    /** Command that adds a task spanning two times, e.g. {@code event meeting /from Mon 2pm /to 4pm}. */
    private static final String COMMAND_EVENT = "event";

    /** Separator introducing a deadline's due time. */
    private static final String OPTION_BY = "/by";

    /** Separator introducing an event's start time. */
    private static final String OPTION_FROM = "/from";

    /** Separator introducing an event's end time. */
    private static final String OPTION_TO = "/to";

    /** Maximum number of tasks Ted can remember, as allowed by the requirements. */
    private static final int MAX_TASKS = 100;

    /**
     * Tasks entered by the user so far.
     * A fixed-size array is sufficient here because the requirements cap the
     * number of tasks at {@value #MAX_TASKS}. A growable {@code ArrayList} would
     * be the more flexible alternative once that cap is lifted.
     */
    private static final Task[] tasks = new Task[MAX_TASKS];

    /** Number of slots of {@link #tasks} that are currently filled. */
    private static int taskCount = 0;

    public static void main(String[] args) {
        String name = "Ted";
        String banner = " _____ _____ ____  \n"
                + "|_   _| ____|  _ \\ \n"
                + "  | | |  _| | | | |\n"
                + "  | | | |___| |_| |\n"
                + "  |_| |_____|____/ \n";
        String greeting = "Hello! I'm " + name + ".\nWhat can I do for you?";
        String exit = "Bye. Hope to see you again soon!";

        printLine();
        System.out.println(banner + greeting);
        printLine();

        // Scanner reads the user's commands from standard input, one line at a time.
        Scanner scanner = new Scanner(System.in);
        while (true) {
            // hasNextLine() guards against the input stream ending (e.g. Ctrl-D or a piped file),
            // which would otherwise make nextLine() throw a NoSuchElementException.
            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine().trim();
            if (input.equals(COMMAND_EXIT)) {
                break;
            }

            printLine();
            try {
                handleCommand(input);
            } catch (TedException e) {
                // Every problem Ted can recognise is recoverable, so the message is
                // shown and the conversation continues with the next command.
                System.out.println(e.getMessage());
            }
            printLine();
        }

        printLine();
        System.out.println(exit);
        printLine();
    }

    /**
     * Carries out the command entered by the user.
     *
     * @param input full line of text entered by the user, already trimmed.
     * @throws TedException if the command is unknown or its details are unusable.
     */
    private static void handleCommand(String input) throws TedException {
        // Splitting into at most two parts keeps the command word exact, so that
        // "todos" is not mistaken for "todo", while leaving the rest untouched.
        String[] parts = input.split(" ", 2);
        String commandWord = parts[0];
        String argument = parts.length > 1 ? parts[1].trim() : "";

        switch (commandWord) {
        case COMMAND_LIST:
            printTasks();
            break;
        case COMMAND_MARK:
            setTaskDone(argument, true);
            break;
        case COMMAND_UNMARK:
            setTaskDone(argument, false);
            break;
        case COMMAND_TODO:
            addTodo(argument);
            break;
        case COMMAND_DEADLINE:
            addDeadline(argument);
            break;
        case COMMAND_EVENT:
            addEvent(argument);
            break;
        default:
            throw new TedException("I don't recognise \"" + commandWord + "\". "
                    + "I understand: todo, deadline, event, list, mark, unmark, bye.");
        }
    }

    /**
     * Adds a task with no date attached.
     *
     * @param description what the user wants to get done.
     * @throws TedException if the description is missing.
     */
    private static void addTodo(String description) throws TedException {
        requireNotBlank(description, "A todo needs a description, for example: todo borrow book");
        addTask(new Todo(description));
    }

    /**
     * Adds a task due by a given time, from an argument of the form
     * {@code <description> /by <time>}.
     *
     * @param argument everything the user typed after the command word.
     * @throws TedException if the description or the due time is missing.
     */
    private static void addDeadline(String argument) throws TedException {
        String example = "for example: deadline return book /by Sunday";
        requireNotBlank(argument, "A deadline needs a description and a due time, " + example);

        int separator = argument.indexOf(OPTION_BY);
        if (separator == -1) {
            throw new TedException("I need to know when this is due. Use /by, " + example);
        }

        String description = argument.substring(0, separator).trim();
        String by = argument.substring(separator + OPTION_BY.length()).trim();
        requireNotBlank(description, "A deadline needs a description before /by, " + example);
        requireNotBlank(by, "A deadline needs a due time after /by, " + example);
        addTask(new Deadline(description, by));
    }

    /**
     * Adds a task spanning two times, from an argument of the form
     * {@code <description> /from <start> /to <end>}.
     *
     * @param argument everything the user typed after the command word.
     * @throws TedException if the description, the start or the end is missing,
     *                      or the two times are given in the wrong order.
     */
    private static void addEvent(String argument) throws TedException {
        String example = "for example: event project meeting /from Mon 2pm /to 4pm";
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
        addTask(new Event(description, from, to));
    }

    /**
     * Stores an already-built task and confirms it to the user.
     *
     * @param task the task to remember.
     * @throws TedException if Ted is already holding {@value #MAX_TASKS} tasks.
     */
    private static void addTask(Task task) throws TedException {
        if (taskCount == MAX_TASKS) {
            throw new TedException("I can only remember " + MAX_TASKS + " tasks, so I can't add that one.");
        }

        tasks[taskCount] = task;
        taskCount++;
        System.out.println("Got it. I've added this task:");
        System.out.println("  " + task);
        printTaskCount();
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
    private static void setTaskDone(String argument, boolean isDone) throws TedException {
        int index = parseTaskIndex(argument, isDone ? COMMAND_MARK : COMMAND_UNMARK);

        if (isDone) {
            tasks[index].markAsDone();
        } else {
            tasks[index].markAsNotDone();
        }

        System.out.println(isDone
                ? "Nice! I've marked this task as done:"
                : "OK, I've marked this task as not done yet:");
        System.out.println("  " + tasks[index]);
    }

    /**
     * Converts a task number typed by the user into an index into {@link #tasks}.
     *
     * @param argument    task number as typed by the user, counting from 1.
     * @param commandWord command the number was given to, used in the error message.
     * @return zero-based index of the task.
     * @throws TedException if the number is missing, not a number, or out of range.
     */
    private static int parseTaskIndex(String argument, String commandWord) throws TedException {
        String example = "for example: " + commandWord + " 2";
        requireNotBlank(argument, "Which task? Give me its number, " + example);

        int taskNumber;
        try {
            taskNumber = Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            // Rethrown as a TedException so the main loop handles every failure the same way.
            throw new TedException("\"" + argument + "\" is not a task number, " + example);
        }

        if (taskCount == 0) {
            throw new TedException("Your list is empty, so there is no task " + taskNumber + " yet.");
        }
        if (taskNumber < 1 || taskNumber > taskCount) {
            throw new TedException("You don't have a task numbered " + taskNumber + ". "
                    + "Pick a number between 1 and " + taskCount + ".");
        }

        // The user counts from 1, the array counts from 0.
        return taskNumber - 1;
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

    /** Prints every stored task as a numbered list, starting from 1. */
    private static void printTasks() {
        if (taskCount == 0) {
            System.out.println("You have no tasks yet.");
            return;
        }

        System.out.println("Here are the tasks in your list:");
        for (int i = 0; i < taskCount; i++) {
            // Displayed numbering is 1-based even though array indices are 0-based.
            System.out.println((i + 1) + "." + tasks[i]);
        }
    }

    /** Tells the user how many tasks are now stored. */
    private static void printTaskCount() {
        String taskWord = taskCount == 1 ? "task" : "tasks";
        System.out.println("Now you have " + taskCount + " " + taskWord + " in the list.");
    }

    private static void printLine() {
        System.out.println("____________________________________________________________");
    }
}
