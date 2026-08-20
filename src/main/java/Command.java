/**
 * A command Ted understands, together with the keyword the user types for it.
 * <p>
 * An enum fits here because the set of commands is fixed and known when the
 * code is written. Compared with the loose {@code String} constants this
 * replaces, the compiler now checks every use, a switch over a command can be
 * seen to cover every case, and the list of keywords shown to a confused user
 * is derived from the commands themselves rather than kept in step by hand.
 */
public enum Command {
    /** Adds a task with no date attached. */
    TODO("todo"),

    /** Adds a task due by a given time. */
    DEADLINE("deadline"),

    /** Adds a task spanning two times. */
    EVENT("event"),

    /** Displays every stored task. */
    LIST("list"),

    /** Marks a task as done. */
    MARK("mark"),

    /** Reverses the done status of a task. */
    UNMARK("unmark"),

    /** Removes a task from the list. */
    DELETE("delete"),

    /** Ends the conversation. */
    BYE("bye");

    /** The word the user types to invoke this command. */
    private final String keyword;

    Command(String keyword) {
        this.keyword = keyword;
    }

    /**
     * Returns the word the user types to invoke this command.
     *
     * @return the command's keyword, e.g. {@code "delete"}.
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Finds the command a keyword refers to.
     *
     * @param keyword first word of the user's input.
     * @return the matching command.
     * @throws TedException if no command uses that keyword.
     */
    public static Command fromKeyword(String keyword) throws TedException {
        for (Command command : values()) {
            if (command.keyword.equals(keyword)) {
                return command;
            }
        }

        throw new TedException("I don't recognise \"" + keyword + "\". "
                + "I understand: " + listKeywords() + ".");
    }

    /**
     * Returns whether the given input ends the conversation.
     * Checked before the input is parsed, so that quitting never has to travel
     * through the error handling that the other commands share.
     *
     * @param input full line of text entered by the user, already trimmed.
     * @return {@code true} if the user asked to leave.
     */
    public static boolean isExit(String input) {
        return BYE.keyword.equals(input);
    }

    /**
     * Lists every keyword, for telling the user what Ted understands.
     * Built from {@link #values()} so that adding a command here is enough to
     * make it appear in the message.
     *
     * @return the keywords, separated by commas.
     */
    private static String listKeywords() {
        String[] keywords = new String[values().length];
        for (int i = 0; i < values().length; i++) {
            keywords[i] = values()[i].keyword;
        }
        return String.join(", ", keywords);
    }
}
