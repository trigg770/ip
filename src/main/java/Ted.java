import java.util.Scanner;

/**
 * Entry point of the Ted chatbot.
 * At this stage Ted stores whatever the user types as a task and can list
 * the stored tasks back on request, until the user enters the {@code bye} command.
 */
public class Ted {
    /** Command that ends the conversation. */
    private static final String COMMAND_EXIT = "bye";

    /** Command that displays every stored task. */
    private static final String COMMAND_LIST = "list";

    /** Maximum number of tasks Ted can remember, as allowed by the requirements. */
    private static final int MAX_TASKS = 100;

    /**
     * Tasks entered by the user so far.
     * A fixed-size array is sufficient here because the requirements cap the
     * number of tasks at {@value #MAX_TASKS}. A growable {@code ArrayList} would
     * be the more flexible alternative once that cap is lifted.
     */
    private static final String[] tasks = new String[MAX_TASKS];

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
            if (input.equals(COMMAND_LIST)) {
                printTasks();
            } else {
                addTask(input);
            }
            printLine();
        }

        printLine();
        System.out.println(exit);
        printLine();
    }

    /**
     * Stores the given task and confirms it to the user.
     * Anything beyond {@link #MAX_TASKS} tasks is rejected rather than silently
     * dropped, so the user always knows whether Ted remembered the task.
     *
     * @param task text entered by the user.
     */
    private static void addTask(String task) {
        if (taskCount == MAX_TASKS) {
            System.out.println("I can only remember " + MAX_TASKS + " tasks, so I can't add that one.");
            return;
        }

        tasks[taskCount] = task;
        taskCount++;
        System.out.println("added: " + task);
    }

    /** Prints every stored task as a numbered list, starting from 1. */
    private static void printTasks() {
        if (taskCount == 0) {
            System.out.println("You have no tasks yet.");
            return;
        }

        for (int i = 0; i < taskCount; i++) {
            // Displayed numbering is 1-based even though array indices are 0-based.
            System.out.println((i + 1) + ". " + tasks[i]);
        }
    }

    private static void printLine() {
        System.out.println("____________________________________________________________");
    }
}
