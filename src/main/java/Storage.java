import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Persists the task list to a human-editable text file so that Ted's tasks
 * survive between runs.
 * <p>
 * The file lives at {@code data/ted.txt}, a relative path from wherever Ted is
 * run from, so it works the same on any computer and any OS. Each task is one
 * line, formatted by {@link Task#toSaveFormat()}:
 *
 * <pre>
 * T | 0 | borrow book
 * D | 0 | June 6th | return book
 * E | 0 | Aug 6th | 2-4pm | project meeting
 * </pre>
 */
public class Storage {
    /** Folder holding the data file, created on first save if missing. */
    private static final Path DATA_DIR = Path.of("data");

    /** File the task list is persisted to, relative to the project root. */
    private static final Path DATA_FILE = DATA_DIR.resolve("ted.txt");

    /**
     * The save-field separator, ready to use as a regex in {@link String#split}.
     * {@link Pattern#quote(String)} ensures the pipe is treated as text rather
     * than as the regex "or" operator.
     */
    private static final String FIELD_SEPARATOR_REGEX = Pattern.quote(Task.SAVE_FIELD_SEPARATOR);

    /**
     * Writes every task to the data file, replacing its previous content.
     * The file is always written even when the list is empty, so that deleting
     * all tasks is remembered.
     *
     * @param tasks the tasks to persist.
     */
    public static void save(List<Task> tasks) {
        try {
            Files.createDirectories(DATA_DIR);
            List<String> lines = new ArrayList<>();
            for (Task task : tasks) {
                lines.add(task.toSaveFormat());
            }
            Files.write(DATA_FILE, lines);
        } catch (IOException e) {
            System.out.println("Unable to save tasks to " + DATA_FILE + " (" + e.getMessage() + ").");
        }
    }

    /**
     * Reads the task list back from the data file.
     *
     * @return the tasks in the order they were saved, or an empty list if the
     *         file does not exist yet (first run).
     */
    public static List<Task> load() {
        List<Task> tasks = new ArrayList<>();

        if (Files.notExists(DATA_FILE)) {
            return tasks;
        }

        try {
            for (String line : Files.readAllLines(DATA_FILE)) {
                if (line.isBlank()) {
                    // A stray blank line (e.g. at the end of the file) is not a task.
                    continue;
                }
                Task task = parseLine(line);
                if (task == null) {
                    // A corrupted line must not take down startup; report it and keep
                    // loading the rest of the file.
                    System.out.println("Skipping an unreadable line in " + DATA_FILE + ": " + line);
                } else {
                    tasks.add(task);
                }
            }
        } catch (IOException e) {
            System.out.println("Unable to load tasks from " + DATA_FILE + " (" + e.getMessage() + ").");
        }
        return tasks;
    }

    /**
     * Rebuilds a single task from one line of the data file.
     * <p>
     * The line has the shape {@code <icon> | <done> | <date fields...> | <description>}.
     * Pipes and backslashes inside fields are escaped. The limited split also
     * preserves descriptions containing raw separators from save files created
     * before escaping was introduced.
     *
     * @param line one non-blank line of the data file.
     * @return the rebuilt task, or {@code null} if the line is not in the expected format.
     */
    private static Task parseLine(String line) {
        // A first, unlimited split just to read the type icon safely: even a
        // corrupted line must have at least its icon before anything can be parsed.
        String icon = line.split(FIELD_SEPARATOR_REGEX)[0];
        int fieldCount = fieldCountFor(icon);
        if (fieldCount == -1) {
            return null;
        }

        // A limited split keeps legacy raw separators inside the final description.
        String[] fields = line.split(FIELD_SEPARATOR_REGEX, fieldCount);
        if (fields.length != fieldCount) {
            return null;
        }
        if (!fields[1].equals("0") && !fields[1].equals("1")) {
            return null;
        }

        String description = decodeSaveField(fields[fieldCount - 1]);
        switch (icon) {
            case "T":
                return withDone(new Todo(description), fields[1].equals("1"));
            case "D":
                return withDone(new Deadline(
                        description, decodeSaveField(fields[2])), fields[1].equals("1"));
            case "E":
                return withDone(new Event(
                        description,
                        decodeSaveField(fields[2]),
                        decodeSaveField(fields[3])),
                        fields[1].equals("1"));
            default:
                return null; // Unreachable: fieldCountFor accepts only T, D, and E.
        }
    }

    /**
     * Number of fields a valid save line has for a given task type:
     * the icon and the done flag, plus the date fields, plus the description.
     *
     * @return 3 for a todo, 4 for a deadline, 5 for an event, or -1 if the icon is unknown.
     */
    private static int fieldCountFor(String icon) {
        return switch (icon) {
            case "T" -> 3;
            case "D" -> 4;
            case "E" -> 5;
            default -> -1;
        };
    }

    /**
     * Restores escaped backslashes and pipe characters in one saved field.
     * An unknown escape sequence is left unchanged so that older save files
     * containing ordinary backslashes remain readable.
     *
     * @param field one encoded field from the save file.
     * @return the field's original text.
     */
    private static String decodeSaveField(String field) {
        StringBuilder decodedField = new StringBuilder();
        for (int i = 0; i < field.length(); i++) {
            char currentCharacter = field.charAt(i);
            boolean hasEscapedCharacter = currentCharacter == '\\' && i + 1 < field.length();
            if (hasEscapedCharacter) {
                char nextCharacter = field.charAt(i + 1);
                if (nextCharacter == '\\' || nextCharacter == '|') {
                    decodedField.append(nextCharacter);
                    i++;
                    continue;
                }
            }
            decodedField.append(currentCharacter);
        }
        return decodedField.toString();
    }

    /**
     * Applies the saved done flag to a freshly created task, which always
     * starts out undone.
     */
    private static Task withDone(Task task, boolean isDone) {
        if (isDone) {
            task.markAsDone();
        }
        return task;
    }
}
