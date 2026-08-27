package ted;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import ted.task.Deadline;
import ted.task.Event;
import ted.task.Task;
import ted.task.TaskList;
import ted.task.Todo;

/**
 * Loads the task list from a human-editable text file and saves it back, so
 * that Ted's tasks survive between runs.
 * <p>
 * The file path is given to the constructor rather than hard-coded, which lets
 * tests point Ted at a scratch file instead of the real one. A relative path
 * such as {@code data/ted.txt} is built with {@link Path}, so it works the same
 * on any computer and any operating system. Each task is one line, formatted
 * by {@link Task#toSaveFormat()}:
 *
 * <pre>
 * T | 0 | borrow book
 * D | 0 | 2019-06-06T18:00 | return book
 * E | 0 | 2019-08-06T14:00 | 2019-08-06T16:00 | project meeting
 * </pre>
 */
public class Storage {
    /**
     * The save-field separator, ready to use as a regex in {@link String#split}.
     * {@link Pattern#quote(String)} ensures the pipe is treated as text rather
     * than as the regex "or" operator.
     */
    private static final String FIELD_SEPARATOR_REGEX = Pattern.quote(Task.SAVE_FIELD_SEPARATOR);

    /** File the task list is persisted to. */
    private final Path dataFile;

    /** How many lines the last {@link #load()} could not make sense of. */
    private int skippedLineCount = 0;

    /**
     * Creates storage backed by the given file.
     *
     * @param filePath where to keep the tasks, e.g. {@code data/ted.txt}.
     */
    public Storage(String filePath) {
        this.dataFile = Path.of(filePath);
    }

    /**
     * Writes every task to the data file, replacing its previous content.
     * The file is always written even when the list is empty, so that deleting
     * all tasks is remembered. Any missing parent folder is created first,
     * which is what makes the very first run work on a fresh computer.
     *
     * @param tasks the tasks to persist.
     * @throws TedException if the file cannot be written.
     */
    public void save(TaskList tasks) throws TedException {
        try {
            Path parentDir = dataFile.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            List<String> lines = new ArrayList<>();
            for (Task task : tasks.asList()) {
                lines.add(task.toSaveFormat());
            }
            Files.write(dataFile, lines);
        } catch (IOException e) {
            throw new TedException("Unable to save tasks to " + dataFile
                    + " (" + e.getMessage() + ").");
        }
    }

    /**
     * Reads the task list back from the data file.
     * A line that is not in the expected format is skipped rather than allowed
     * to take down startup; {@link #getSkippedLineCount()} reports how many.
     *
     * @return the tasks in the order they were saved, or an empty list if the
     *         file does not exist yet (first run).
     * @throws TedException if the file exists but cannot be read.
     */
    public List<Task> load() throws TedException {
        List<Task> tasks = new ArrayList<>();
        skippedLineCount = 0;

        if (Files.notExists(dataFile)) {
            return tasks;
        }

        try {
            for (String line : Files.readAllLines(dataFile)) {
                if (line.isBlank()) {
                    // A stray blank line (e.g. at the end of the file) is not a task.
                    continue;
                }

                Task task = parseLine(line);
                if (task == null) {
                    skippedLineCount++;
                } else {
                    tasks.add(task);
                }
            }
        } catch (IOException e) {
            throw new TedException("Unable to load tasks from " + dataFile
                    + " (" + e.getMessage() + ").");
        }
        return tasks;
    }

    /**
     * Returns how many lines the last {@link #load()} had to skip.
     *
     * @return the number of unreadable lines, or 0 if the file was clean.
     */
    public int getSkippedLineCount() {
        return skippedLineCount;
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
        boolean isDone = fields[1].equals("1");
        try {
            switch (icon) {
                case "T":
                    return withDone(new Todo(description), isDone);
                case "D":
                    return withDone(new Deadline(
                            description, parseSavedDateTime(fields[2])), isDone);
                case "E":
                    return withDone(new Event(
                            description,
                            parseSavedDateTime(fields[2]),
                            parseSavedDateTime(fields[3])),
                            isDone);
                default:
                    // Unreachable: fieldCountFor accepts only T, D and E.
                    return null;
            }
        } catch (DateTimeParseException e) {
            // A line whose date cannot be read is corrupted as far as Ted is
            // concerned; the caller counts it and carries on with the rest.
            return null;
        }
    }

    /**
     * Reads back a date and time written by {@link LocalDateTime#toString()}.
     * The saved form is ISO-8601 rather than the format shown to the user, so
     * that the file stays unambiguous and independent of Ted's display format.
     *
     * @param field one encoded date-time field from the save file.
     * @return the date and time it stands for.
     * @throws DateTimeParseException if the field is not a valid ISO date-time.
     */
    private static LocalDateTime parseSavedDateTime(String field) {
        return LocalDateTime.parse(decodeSaveField(field));
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
