package ted;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import ted.task.TaskList;
import ted.task.Todo;

/**
 * Tests {@link Ui}, which words everything Ted says and reads what the user types.
 * <p>
 * Most of Ui only builds up a reply, so its tests read the reply back with
 * {@link Ui#flush()}. The two methods that touch the terminal are tested by
 * swapping {@code System.in} and {@code System.out} for in-memory streams.
 */
public class UiTest {
    /**
     * Verifies that the welcome banner keeps the leading space that lines up
     * the first row of its ASCII art.
     */
    @Test
    public void flush_welcome_keepsBannerAligned() {
        Ui ui = new Ui();
        ui.showWelcome();
        String greeting = ui.flush();

        assertTrue(greeting.startsWith(" _____ _____ ____"));
        assertTrue(greeting.contains("Hello! I'm Ted."));
    }

    /**
     * Verifies that a flushed reply has no trailing line break, and that the
     * next reply starts empty.
     */
    @Test
    public void flush_calledTwice_secondReplyIsEmpty() {
        Ui ui = new Ui();
        ui.showGoodbye();

        assertEquals("Bye. Hope to see you again soon!", ui.flush());
        assertEquals("", ui.flush());
    }

    /**
     * Verifies that the task count reads naturally for one task and for several.
     */
    @Test
    public void showAdded_taskCount_singularOrPlural() {
        Ui ui = new Ui();
        ui.showAdded(new Todo("borrow book"), 1);
        assertTrue(ui.flush().contains("Now you have 1 task in the list."));

        ui.showRemoved(new Todo("borrow book"), 2);
        assertTrue(ui.flush().contains("Now you have 2 tasks in the list."));
    }

    /**
     * Verifies that the skipped-line warning reads naturally for one line and for several.
     */
    @Test
    public void showSkippedLines_count_singularOrPlural() {
        Ui ui = new Ui();
        ui.showSkippedLines(1);
        assertEquals("Skipped 1 unreadable line in your save file.", ui.flush());

        ui.showSkippedLines(3);
        assertEquals("Skipped 3 unreadable lines in your save file.", ui.flush());
    }

    /**
     * Verifies that an empty list gets a message rather than a bare header.
     */
    @Test
    public void showTasks_emptyList_saysSo() {
        Ui ui = new Ui();
        ui.showTasks(new TaskList());
        assertEquals("You have no tasks yet.", ui.flush());
    }

    /**
     * Verifies that matching tasks keep their numbers from the full list,
     * and that a search with no matches repeats the keyword back.
     */
    @Test
    public void showMatchingTasks_matchesAndNoMatches_numberedFromFullList() {
        TaskList tasks = new TaskList(List.of(new Todo("buy milk"), new Todo("read book")));
        Ui ui = new Ui();

        ui.showMatchingTasks(tasks, tasks.find("book"), "book");
        assertTrue(ui.flush().contains("2.[T][ ] read book"));

        ui.showMatchingTasks(tasks, tasks.find("bicycle"), "bicycle");
        assertEquals("No task matches \"bicycle\".", ui.flush());
    }

    /**
     * Verifies that a loading problem is followed by what Ted does about it,
     * and that the backup's location is given.
     */
    @Test
    public void showLoadingErrorAndBackup_messages_bothShown() {
        Ui ui = new Ui();
        ui.showLoadingError("I couldn't read your saved tasks.");
        ui.showBackup(Path.of("data", "ted.txt.bak"));
        String reply = ui.flush();

        assertTrue(reply.startsWith("I couldn't read your saved tasks."));
        assertTrue(reply.contains("Starting with an empty list for now."));
        assertTrue(reply.contains(Path.of("data", "ted.txt.bak").toString()));
    }

    /**
     * Verifies that a reply printed to the terminal sits between two dividers.
     */
    @Test
    public void printReply_reply_framedByDividers() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(printed, true, StandardCharsets.UTF_8));
            new Ui().printReply("Hello");
        } finally {
            System.setOut(originalOut);
        }

        String[] lines = printed.toString(StandardCharsets.UTF_8).split("\\R");
        assertEquals(3, lines.length);
        assertTrue(lines[0].matches("_+"));
        assertEquals("Hello", lines[1]);
        assertEquals(lines[0], lines[2]);
    }

    /**
     * Verifies that typed lines are read in order with surrounding spaces
     * removed, and that the end of the input is noticed.
     */
    @Test
    public void readCommand_typedLines_readInOrderUntilInputEnds() {
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream("  list  \nbye\n".getBytes(StandardCharsets.UTF_8)));
            // The Ui reads from whatever System.in is when the Ui is created.
            Ui ui = new Ui();

            assertTrue(ui.hasNextCommand());
            assertEquals("list", ui.readCommand());
            assertEquals("bye", ui.readCommand());
            assertFalse(ui.hasNextCommand());
        } finally {
            System.setIn(originalIn);
        }
    }
}
