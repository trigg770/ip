package ted;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ted.task.Deadline;
import ted.task.Event;
import ted.task.Task;
import ted.task.TaskList;
import ted.task.Todo;

/**
 * Tests {@link Storage}, which writes the task list to disk and reads it back.
 * <p>
 * Every test writes inside a {@link TempDir} rather than the real save file, so
 * running the tests can never disturb the user's own tasks.
 */
public class StorageTest {
    /** A fixed date-time, so the tests do not depend on when they are run. */
    private static final LocalDateTime SECOND_OF_DECEMBER_6PM = LocalDateTime.of(2019, 12, 2, 18, 0);

    /** Two hours before {@link #SECOND_OF_DECEMBER_6PM}. */
    private static final LocalDateTime SECOND_OF_DECEMBER_4PM = LocalDateTime.of(2019, 12, 2, 16, 0);

    @Test
    public void load_fileDoesNotExist_returnsEmptyList(@TempDir Path tempDir) throws TedException {
        // The first run on a new computer has no save file, which must not be an error.
        Storage storage = new Storage(tempDir.resolve("ted.txt").toString());
        assertTrue(storage.load().isEmpty());
    }

    @Test
    public void save_folderDoesNotExist_createsIt(@TempDir Path tempDir) throws TedException {
        Path dataFile = tempDir.resolve("data").resolve("ted.txt");
        Storage storage = new Storage(dataFile.toString());
        storage.save(new TaskList(List.of(new Todo("borrow book"))));
        assertTrue(Files.exists(dataFile));
    }

    @Test
    public void saveThenLoad_everyTaskType_roundTripsUnchanged(@TempDir Path tempDir) throws TedException {
        Storage storage = new Storage(tempDir.resolve("ted.txt").toString());
        Todo todo = new Todo("borrow book");
        todo.markAsDone();
        TaskList saved = new TaskList(List.of(
                todo,
                new Deadline("return book", SECOND_OF_DECEMBER_6PM),
                new Event("meeting", SECOND_OF_DECEMBER_4PM, SECOND_OF_DECEMBER_6PM)));

        storage.save(saved);
        List<Task> loaded = storage.load();

        assertEquals(3, loaded.size());
        assertEquals(0, storage.getSkippedLineCount());
        assertInstanceOf(Todo.class, loaded.get(0));
        assertInstanceOf(Deadline.class, loaded.get(1));
        assertInstanceOf(Event.class, loaded.get(2));
        // Comparing the save format checks the description, the done flag and
        // the dates in one go.
        for (int i = 0; i < loaded.size(); i++) {
            assertEquals(saved.asList().get(i).toSaveFormat(), loaded.get(i).toSaveFormat());
        }
    }

    @Test
    public void saveThenLoad_descriptionContainingSeparator_roundTripsUnchanged(@TempDir Path tempDir)
            throws TedException {
        Storage storage = new Storage(tempDir.resolve("ted.txt").toString());
        storage.save(new TaskList(List.of(new Todo("rock | roll \\ blues"))));

        List<Task> loaded = storage.load();
        assertEquals(1, loaded.size());
        assertEquals("[T][ ] rock | roll \\ blues", loaded.get(0).toString());
    }

    @Test
    public void save_emptyList_forgetsPreviousTasks(@TempDir Path tempDir) throws TedException {
        // Deleting the last task must be remembered, not silently undone by an
        // untouched save file.
        Storage storage = new Storage(tempDir.resolve("ted.txt").toString());
        storage.save(new TaskList(List.of(new Todo("borrow book"))));
        storage.save(new TaskList());
        assertTrue(storage.load().isEmpty());
    }

    @Test
    public void load_blankLines_ignoresThem(@TempDir Path tempDir) throws TedException, IOException {
        Path dataFile = tempDir.resolve("ted.txt");
        Files.writeString(dataFile, "T | 0 | borrow book\n\n   \n");

        Storage storage = new Storage(dataFile.toString());
        assertEquals(1, storage.load().size());
        assertEquals(0, storage.getSkippedLineCount());
    }

    @Test
    public void load_corruptedLines_skipsThemAndKeepsTheRest(@TempDir Path tempDir)
            throws TedException, IOException {
        Path dataFile = tempDir.resolve("ted.txt");
        Files.writeString(dataFile, String.join("\n",
                "T | 0 | borrow book",
                "X | 0 | unknown task type",
                "T | 2 | done flag is not 0 or 1",
                "D | 0 | not-a-date | return book",
                "D | 0 | missing the description",
                "gibberish",
                "E | 1 | 2019-12-02T16:00 | 2019-12-02T18:00 | meeting"));

        Storage storage = new Storage(dataFile.toString());
        List<Task> loaded = storage.load();

        // The good lines survive; the five broken ones are counted, not fatal.
        assertEquals(2, loaded.size());
        assertEquals(5, storage.getSkippedLineCount());
        assertEquals("[T][ ] borrow book", loaded.get(0).toString());
        assertInstanceOf(Event.class, loaded.get(1));
    }

    @Test
    public void load_calledTwice_resetsTheSkippedCount(@TempDir Path tempDir)
            throws TedException, IOException {
        Path dataFile = tempDir.resolve("ted.txt");
        Files.writeString(dataFile, "gibberish\n");
        Storage storage = new Storage(dataFile.toString());
        storage.load();

        Files.writeString(dataFile, "T | 0 | borrow book\n");
        storage.load();
        assertEquals(0, storage.getSkippedLineCount());
    }

    @Test
    public void load_fileHandEditedByUser_isAccepted(@TempDir Path tempDir)
            throws TedException, IOException {
        // The save format is meant to be human-editable, so a line typed by hand
        // must load like one Ted wrote itself.
        Path dataFile = tempDir.resolve("ted.txt");
        Files.writeString(dataFile, "D | 1 | 2019-12-02T18:00 | return book\n");

        List<Task> loaded = new Storage(dataFile.toString()).load();
        assertEquals(1, loaded.size());
        assertEquals("D | 1 | 2019-12-02T18:00 | return book", loaded.get(0).toSaveFormat());
    }
}
