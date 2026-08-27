package ted.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ted.TedException;

/**
 * Tests {@link TaskList}, which owns the tasks and the rule that a task number
 * must point at a real one.
 * <p>
 * The bounds checking is the part worth testing: it is what stands between a
 * mistyped task number and a crash.
 */
public class TaskListTest {
    @Test
    public void size_newList_isEmpty() {
        TaskList tasks = new TaskList();
        assertEquals(0, tasks.size());
        assertTrue(tasks.isEmpty());
    }

    @Test
    public void add_severalTasks_keepsInsertionOrder() throws TedException {
        TaskList tasks = new TaskList();
        Todo first = new Todo("first");
        Todo second = new Todo("second");
        tasks.add(first);
        tasks.add(second);

        assertEquals(2, tasks.size());
        assertFalse(tasks.isEmpty());
        assertSame(first, tasks.get(0));
        assertSame(second, tasks.get(1));
    }

    @Test
    public void get_indexWithinList_returnsTask() throws TedException {
        TaskList tasks = new TaskList(List.of(new Todo("only task")));
        assertEquals("only task", tasks.get(0).toString().substring("[T][ ] ".length()));
    }

    @Test
    public void get_indexPastEnd_exceptionThrown() {
        TaskList tasks = new TaskList(List.of(new Todo("only task")));
        TedException e = assertThrows(TedException.class, () -> tasks.get(1));
        // The message speaks in the 1-based numbers the user sees.
        assertTrue(e.getMessage().contains("task numbered 2"));
        assertTrue(e.getMessage().contains("between 1 and 1"));
    }

    @Test
    public void get_negativeIndex_exceptionThrown() {
        TaskList tasks = new TaskList(List.of(new Todo("only task")));
        assertThrows(TedException.class, () -> tasks.get(-1));
    }

    @Test
    public void get_emptyList_exceptionThrown() {
        TaskList tasks = new TaskList();
        TedException e = assertThrows(TedException.class, () -> tasks.get(0));
        assertTrue(e.getMessage().contains("empty"));
    }

    @Test
    public void remove_indexWithinList_removesAndReturnsTask() throws TedException {
        Todo first = new Todo("first");
        Todo second = new Todo("second");
        TaskList tasks = new TaskList(List.of(first, second));

        assertSame(first, tasks.remove(0));
        assertEquals(1, tasks.size());
        // The gap left behind must close, so the survivor becomes task 1.
        assertSame(second, tasks.get(0));
    }

    @Test
    public void remove_indexPastEnd_exceptionThrown() {
        TaskList tasks = new TaskList(List.of(new Todo("only task")));
        assertThrows(TedException.class, () -> tasks.remove(1));
        // A rejected removal must leave the list untouched.
        assertEquals(1, tasks.size());
    }

    @Test
    public void remove_emptyList_exceptionThrown() {
        assertThrows(TedException.class, () -> new TaskList().remove(0));
    }

    @Test
    public void constructor_givenList_copiesIt() throws TedException {
        ArrayList<Task> source = new ArrayList<>();
        source.add(new Todo("first"));
        TaskList tasks = new TaskList(source);

        // Later changes to the caller's list must not leak into the task list.
        source.add(new Todo("second"));
        assertEquals(1, tasks.size());
    }

    @Test
    public void asList_returnedList_cannotBeModified() {
        TaskList tasks = new TaskList(List.of(new Todo("only task")));
        List<Task> view = tasks.asList();
        assertThrows(UnsupportedOperationException.class, () -> view.add(new Todo("sneaky")));
    }
}
