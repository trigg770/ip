package ted.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests how tasks present themselves, both to the user ({@code toString}) and
 * to the save file ({@link Task#toSaveFormat()}).
 * <p>
 * The save format matters most: anything it gets wrong is written to disk and
 * read back as a corrupted line on the next run.
 */
public class TaskTest {
    /** A fixed date-time, so the tests do not depend on when they are run. */
    private static final LocalDateTime SECOND_OF_DECEMBER_6PM = LocalDateTime.of(2019, 12, 2, 18, 0);

    /** Two hours before {@link #SECOND_OF_DECEMBER_6PM}. */
    private static final LocalDateTime SECOND_OF_DECEMBER_4PM = LocalDateTime.of(2019, 12, 2, 16, 0);

    @Test
    public void toString_newTodo_showsTypeAndEmptyStatus() {
        assertEquals("[T][ ] borrow book", new Todo("borrow book").toString());
    }

    @Test
    public void toString_doneTodo_showsCross() {
        Todo todo = new Todo("borrow book");
        todo.markAsDone();
        assertEquals("[T][X] borrow book", todo.toString());
    }

    @Test
    public void markAsNotDone_doneTask_reversesTheMark() {
        Todo todo = new Todo("borrow book");
        todo.markAsDone();
        todo.markAsNotDone();
        assertEquals("[T][ ] borrow book", todo.toString());
    }

    @Test
    public void toString_deadline_showsFriendlyDate() {
        String shown = new Deadline("return book", SECOND_OF_DECEMBER_6PM).toString();
        // The exact am/pm wording depends on the locale, so check the parts
        // that do not: the type, the description and the date itself.
        assertTrue(shown.startsWith("[D][ ] return book (by: "));
        assertTrue(shown.contains("2 Dec 2019"));
        assertTrue(shown.contains("6:00"));
    }

    @Test
    public void toString_event_showsBothEnds() {
        String shown = new Event("meeting", SECOND_OF_DECEMBER_4PM, SECOND_OF_DECEMBER_6PM).toString();
        assertTrue(shown.startsWith("[E][ ] meeting (from: "));
        assertTrue(shown.contains("4:00"));
        assertTrue(shown.contains("6:00"));
    }

    @Test
    public void toSaveFormat_todo_writesIconDoneFlagAndDescription() {
        assertEquals("T | 0 | borrow book", new Todo("borrow book").toSaveFormat());
    }

    @Test
    public void toSaveFormat_doneTodo_writesOneAsTheFlag() {
        Todo todo = new Todo("borrow book");
        todo.markAsDone();
        assertEquals("T | 1 | borrow book", todo.toSaveFormat());
    }

    @Test
    public void toSaveFormat_deadline_writesIsoDateTime() {
        // ISO-8601 rather than the display format, so the file does not change
        // meaning when the way Ted prints dates changes.
        assertEquals("D | 0 | 2019-12-02T18:00 | return book",
                new Deadline("return book", SECOND_OF_DECEMBER_6PM).toSaveFormat());
    }

    @Test
    public void toSaveFormat_event_writesBothIsoDateTimes() {
        assertEquals("E | 0 | 2019-12-02T16:00 | 2019-12-02T18:00 | meeting",
                new Event("meeting", SECOND_OF_DECEMBER_4PM, SECOND_OF_DECEMBER_6PM).toSaveFormat());
    }

    @Test
    public void toSaveFormat_descriptionContainingSeparator_escapesIt() {
        // An unescaped pipe would split the description into two fields when
        // the line is read back.
        assertEquals("T | 0 | rock \\| roll", new Todo("rock | roll").toSaveFormat());
    }

    @Test
    public void toSaveFormat_descriptionContainingBackslash_escapesIt() {
        assertEquals("T | 0 | back\\\\slash", new Todo("back\\slash").toSaveFormat());
    }

    @Test
    public void toString_taggedTodo_showsTagsAfterDescription() {
        Todo todo = new Todo("read book");
        todo.addTags(List.of(new Tag("fun"), new Tag("school")));
        assertEquals("[T][ ] read book #fun #school", todo.toString());
    }

    @Test
    public void toString_taggedDeadline_showsTagsBeforeDate() {
        Deadline deadline = new Deadline("return book", SECOND_OF_DECEMBER_6PM);
        deadline.addTags(List.of(new Tag("library")));
        assertTrue(deadline.toString().startsWith("[D][ ] return book #library (by: "));
    }

    @Test
    public void addTags_tagAlreadyPresent_notRepeated() {
        Todo todo = new Todo("read book");
        todo.addTags(List.of(new Tag("fun")));
        todo.addTags(List.of(new Tag("FUN"), new Tag("school")));
        // The repeated tag keeps its original place rather than moving to the end.
        assertEquals("[T][ ] read book #fun #school", todo.toString());
    }

    @Test
    public void removeTags_someOfTheTags_removesOnlyThose() {
        Todo todo = new Todo("read book");
        todo.addTags(List.of(new Tag("fun"), new Tag("school")));
        todo.removeTags(List.of(new Tag("school")));
        assertTrue(todo.hasTag(new Tag("fun")));
        assertFalse(todo.hasTag(new Tag("school")));
    }

    @Test
    public void toSaveFormat_taggedTodo_writesTagsAfterDoneFlag() {
        Todo todo = new Todo("read book");
        todo.addTags(List.of(new Tag("fun"), new Tag("school")));
        assertEquals("T | 0 | #fun #school | read book", todo.toSaveFormat());
    }

    @Test
    public void toSaveFormat_taggedEvent_writesTagsBeforeDates() {
        Event event = new Event("meeting", SECOND_OF_DECEMBER_4PM, SECOND_OF_DECEMBER_6PM);
        event.addTags(List.of(new Tag("cs2103")));
        assertEquals("E | 0 | #cs2103 | 2019-12-02T16:00 | 2019-12-02T18:00 | meeting",
                event.toSaveFormat());
    }

    @Test
    public void toSaveFormat_lastTagRemoved_writesUntaggedLine() {
        // Without tags the line must be exactly what Ted wrote before tags existed.
        Todo todo = new Todo("read book");
        todo.addTags(List.of(new Tag("fun")));
        todo.removeTags(List.of(new Tag("fun")));
        assertEquals("T | 0 | read book", todo.toSaveFormat());
    }
}
