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
    private static final String OPTION_BY = " /by ";

    /** Separator introducing an event's start time. */
    private static final String OPTION_FROM = " /from ";

    /** Separator introducing an event's end time. */
    private static final String OPTION_TO = " /to ";

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
            handleCommand(input);
            printLine();
        }

        printLine();
        System.out.println(exit);
        printLine();
    }

    /**
     * Carries out the command entered by the user.
     * Anything that is not a recognised command is stored as a new task.
     *
     * @param input full line of text entered by the user, already trimmed.
     */
    private static void handleCommand(String input) {
        if (input.equals(COMMAND_LIST)) {
            printTasks();
        } else if (input.startsWith(COMMAND_UNMARK + " ")) {
            // Checked before "mark" so that "unmark 2" is not mistaken for a mark command.
            setTaskDone(input.substring(COMMAND_UNMARK.length()).trim(), false);
        } else if (input.startsWith(COMMAND_MARK + " ")) {
            setTaskDone(input.substring(COMMAND_MARK.length()).trim(), true);
        } else if (input.startsWith(COMMAND_TODO)) {
            addTodo(input.substring(COMMAND_TODO.length()).trim());
        } else if (input.startsWith(COMMAND_DEADLINE)) {
            addDeadline(input.substring(COMMAND_DEADLINE.length()).trim());
        } else if (input.startsWith(COMMAND_EVENT)) {
            addEvent(input.substring(COMMAND_EVENT.length()).trim());
        } else {
            System.out.println("I don't know that command. Try: todo, deadline, event, list, mark, unmark or bye.");
        }
    }

    /**
     * Adds a task with no date attached.
     *
     * @param description what the user wants to get done.
     */
    private static void addTodo(String description) {
        addTask(new Todo(description));
    }

    /**
     * Adds a task due by a given time, from an argument of the form
     * {@code <description> /by <time>}.
     *
     * @param argument everything the user typed after the command word.
     */
    private static void addDeadline(String argument) {
        int separator = argument.indexOf(OPTION_BY);
        if (separator == -1) {
            System.out.println("A deadline needs a due time, for example: deadline return book /by Sunday");
            return;
        }

        String description = argument.substring(0, separator).trim();
        String by = argument.substring(separator + OPTION_BY.length()).trim();
        addTask(new Deadline(description, by));
    }

    /**
     * Adds a task spanning two times, from an argument of the form
     * {@code <description> /from <start> /to <end>}.
     *
     * @param argument everything the user typed after the command word.
     */
    private static void addEvent(String argument) {
        int fromSeparator = argument.indexOf(OPTION_FROM);
        int toSeparator = argument.indexOf(OPTION_TO);
        if (fromSeparator == -1 || toSeparator == -1 || toSeparator < fromSeparator) {
            System.out.println("An event needs a start and an end, for example: "
                    + "event project meeting /from Mon 2pm /to 4pm");
            return;
        }

        String description = argument.substring(0, fromSeparator).trim();
        String from = argument.substring(fromSeparator + OPTION_FROM.length(), toSeparator).trim();
        String to = argument.substring(toSeparator + OPTION_TO.length()).trim();
        addTask(new Event(description, from, to));
    }

    /**
     * Stores an already-built task and confirms it to the user.
     * Anything beyond {@link #MAX_TASKS} tasks is rejected rather than silently
     * dropped, so the user always knows whether Ted remembered the task.
     *
     * @param task the task to remember.
     */
    private static void addTask(Task task) {
        if (taskCount == MAX_TASKS) {
            System.out.println("I can only remember " + MAX_TASKS + " tasks, so I can't add that one.");
            return;
        }

        tasks[taskCount] = task;
        taskCount++;
        System.out.println("Got it. I've added this task:");
        System.out.println("  " + task);
        printTaskCount();
    }

    /** Tells the user how many tasks are now stored. */
    private static void printTaskCount() {
        String taskWord = taskCount == 1 ? "task" : "tasks";
        System.out.println("Now you have " + taskCount + " " + taskWord + " in the list.");
    }

    /**
     * Sets the done status of the task at the given position and shows the result.
     * Marking and unmarking differ only in the stored flag and the wording, so
     * they share one method rather than duplicating the lookup logic.
     *
     * @param argument task number as typed by the user, counting from 1.
     * @param isDone   {@code true} to mark the task done, {@code false} to reverse it.
     */
    private static void setTaskDone(String argument, boolean isDone) {
        int index = parseTaskIndex(argument);
        if (index == -1) {
            return;
        }

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
     * Converts a task number typed by the user into an index into the task arrays.
     * Reports the problem to the user and returns {@code -1} when the number is
     * missing, not a number, or outside the range of stored tasks, so that a
     * mistyped command never crashes the conversation.
     *
     * @param argument task number as typed by the user, counting from 1.
     * @return zero-based index of the task, or {@code -1} if the number is unusable.
     */
    private static int parseTaskIndex(String argument) {
        int taskNumber;
        try {
            taskNumber = Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            System.out.println("I need a task number, for example: mark 2");
            return -1;
        }

        if (taskNumber < 1 || taskNumber > taskCount) {
            System.out.println("You don't have a task numbered " + taskNumber + ".");
            return -1;
        }

        // The user counts from 1, the arrays count from 0.
        return taskNumber - 1;
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

    private static void printLine() {
        System.out.println("____________________________________________________________");
    }
}
