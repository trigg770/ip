package ted;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ted.command.AddCommand;
import ted.command.Command;
import ted.command.DeleteCommand;
import ted.command.ExitCommand;
import ted.command.FindCommand;
import ted.command.ListCommand;
import ted.command.MarkCommand;
import ted.task.Task;
import ted.task.TaskList;
import ted.task.Todo;

/**
 * Tests {@link Parser#parse(String)}, which turns a line of user input into
 * the command it stands for.
 * <p>
 * Parser is worth testing closely because it is where malformed input is
 * caught: every check it skips becomes a crash or a wrong task later on.
 */
public class ParserTest {
    @Test
    public void parse_todoWithDescription_returnsAddCommand() throws TedException {
        assertInstanceOf(AddCommand.class, Parser.parse("todo borrow book"));
    }

    @Test
    public void parse_deadlineWithValidDateTime_returnsAddCommand() throws TedException {
        assertInstanceOf(AddCommand.class, Parser.parse("deadline return book /by 2/12/2019 1800"));
    }

    @Test
    public void parse_eventWithValidDateTimes_returnsAddCommand() throws TedException {
        assertInstanceOf(AddCommand.class,
                Parser.parse("event meeting /from 2/12/2019 1400 /to 2/12/2019 1600"));
    }

    @Test
    public void parse_listCommand_returnsListCommand() throws TedException {
        assertInstanceOf(ListCommand.class, Parser.parse("list"));
    }

    @Test
    public void parse_deleteCommand_returnsDeleteCommand() throws TedException {
        assertInstanceOf(DeleteCommand.class, Parser.parse("delete 2"));
    }

    @Test
    public void parse_markCommand_returnsMarkCommand() throws TedException {
        assertInstanceOf(MarkCommand.class, Parser.parse("mark 1"));
        assertInstanceOf(MarkCommand.class, Parser.parse("unmark 1"));
    }

    @Test
    public void parse_byeCommand_returnsExitingCommand() throws TedException {
        Command command = Parser.parse("bye");
        assertInstanceOf(ExitCommand.class, command);
        assertTrue(command.isExit());
    }

    @Test
    public void parse_otherCommand_doesNotExit() throws TedException {
        assertFalse(Parser.parse("list").isExit());
    }

    @Test
    public void parse_findWithKeyword_returnsFindCommand() throws TedException {
        assertInstanceOf(FindCommand.class, Parser.parse("find book"));
    }

    @Test
    public void parse_findWithMultiWordKeyword_returnsFindCommand() throws TedException {
        // The whole of the rest of the line is the keyword, spaces included.
        assertInstanceOf(FindCommand.class, Parser.parse("find return book"));
    }

    @Test
    public void parse_findWithoutKeyword_exceptionThrown() {
        assertThrows(TedException.class, () -> Parser.parse("find"));
        assertThrows(TedException.class, () -> Parser.parse("find    "));
    }

    @Test
    public void parse_unknownKeyword_exceptionThrown() {
        TedException e = assertThrows(TedException.class, () -> Parser.parse("blah"));
        // The message should name the offending word and list what Ted does understand.
        assertTrue(e.getMessage().contains("blah"));
        assertTrue(e.getMessage().contains("todo"));
    }

    @Test
    public void parse_keywordWithTrailingLetters_exceptionThrown() {
        // "todos" must not be accepted as "todo": the command word is matched whole.
        assertThrows(TedException.class, () -> Parser.parse("todos borrow book"));
    }

    @Test
    public void parse_todoWithoutDescription_exceptionThrown() {
        assertThrows(TedException.class, () -> Parser.parse("todo"));
        assertThrows(TedException.class, () -> Parser.parse("todo    "));
    }

    @Test
    public void parse_deadlineWithoutBy_exceptionThrown() {
        assertThrows(TedException.class, () -> Parser.parse("deadline return book"));
    }

    @Test
    public void parse_deadlineWithoutDescription_exceptionThrown() {
        assertThrows(TedException.class, () -> Parser.parse("deadline /by 2/12/2019 1800"));
    }

    @Test
    public void parse_deadlineWithoutDateTime_exceptionThrown() {
        assertThrows(TedException.class, () -> Parser.parse("deadline return book /by"));
    }

    @Test
    public void parse_deadlineWithUnreadableDateTime_exceptionThrown() {
        TedException e = assertThrows(TedException.class, () -> Parser.parse("deadline return book /by tomorrow"));
        assertTrue(e.getMessage().contains("tomorrow"));
    }

    @Test
    public void parse_deadlineWithDateButNoTime_exceptionThrown() {
        // The time is part of the expected format, so a bare date is rejected
        // rather than silently assumed to mean midnight.
        assertThrows(TedException.class, () -> Parser.parse("deadline return book /by 2/12/2019"));
    }

    @Test
    public void parse_deadlineWithImpossibleDate_exceptionThrown() {
        assertThrows(TedException.class, () -> Parser.parse("deadline return book /by 31/2/2019 1800"));
    }

    @Test
    public void parse_eventMissingOneSeparator_exceptionThrown() {
        assertThrows(TedException.class, () -> Parser.parse("event meeting /from 2/12/2019 1400"));
        assertThrows(TedException.class, () -> Parser.parse("event meeting /to 2/12/2019 1600"));
    }

    @Test
    public void parse_eventWithSeparatorsSwapped_exceptionThrown() {
        assertThrows(TedException.class, () -> Parser.parse("event meeting /to 2/12/2019 1600 /from 2/12/2019 1400"));
    }

    @Test
    public void parse_eventEndingBeforeStart_exceptionThrown() {
        assertThrows(TedException.class, () -> Parser.parse("event meeting /from 2/12/2019 1600 /to 2/12/2019 1400"));
    }

    @Test
    public void parse_eventEndingWhenItStarts_returnsAddCommand() throws TedException {
        // A zero-length event is odd but not impossible, so it is allowed.
        assertInstanceOf(AddCommand.class,
                Parser.parse("event meeting /from 2/12/2019 1400 /to 2/12/2019 1400"));
    }

    @Test
    public void parse_taskNumberMissing_exceptionThrown() {
        assertThrows(TedException.class, () -> Parser.parse("delete"));
        assertThrows(TedException.class, () -> Parser.parse("mark"));
    }

    @Test
    public void parse_taskNumberNotANumber_exceptionThrown() {
        TedException e = assertThrows(TedException.class, () -> Parser.parse("delete two"));
        assertTrue(e.getMessage().contains("two"));
    }

    @Test
    public void parse_extraSpacesAroundArgument_argumentTrimmed() throws TedException {
        // Whether the user pads their input should make no difference.
        assertInstanceOf(AddCommand.class, Parser.parse("todo    borrow book   "));
        assertInstanceOf(DeleteCommand.class, Parser.parse("delete   2  "));
    }

    @Test
    public void parse_commandWordAlone_isCaseSensitive() {
        // Ted's keywords are lower case; accepting "LIST" would need a decision
        // about the rest of the input too, so it is rejected for now.
        assertThrows(TedException.class, () -> Parser.parse("LIST"));
    }

    @Test
    public void parse_deleteWithNumber_indexIsZeroBased() throws TedException {
        // The user counts from 1; a command built from "delete 1" must therefore
        // point at index 0. Proven by deleting from a one-task list.
        TaskList tasks = new TaskList(List.of(new Todo("only task")));
        Parser.parse("delete 1").execute(tasks, new SilentUi(), new NoOpStorage());
        assertEquals(0, tasks.size());
    }

    /** A Ui that says nothing, so tests do not print over the test report. */
    private static class SilentUi extends Ui {
        @Override
        public void showRemoved(Task task, int taskCount) {
            // Deliberately silent.
        }
    }

    /** Storage that keeps nothing, so tests never touch a real save file. */
    private static class NoOpStorage extends Storage {
        NoOpStorage() {
            super("data/unused.txt");
        }

        @Override
        public void save(TaskList tasks) {
            // Deliberately does nothing.
        }
    }
}
