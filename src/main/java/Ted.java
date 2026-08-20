import java.util.Scanner;

/**
 * Entry point of the Ted chatbot.
 * At this stage Ted simply echoes back whatever the user types,
 * until the user enters the {@code bye} command.
 */
public class Ted {
    /** Command that ends the conversation. */
    private static final String COMMAND_EXIT = "bye";

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
            System.out.println(input);
            printLine();
        }

        System.out.println(exit);
        printLine();
    }

    private static void printLine() {
        System.out.println("____________________________________________________________");
    }
}
