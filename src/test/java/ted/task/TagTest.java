package ted.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link Tag}, which holds the one rule for what a tag may be.
 * <p>
 * The parser, the save file and the tasks all rely on this rule, so a mistake
 * here would show up in all three.
 */
public class TagTest {
    @Test
    public void constructor_mixedCaseName_storedInLowerCase() {
        assertEquals("fun", new Tag("Fun").name());
        assertEquals("#cs2103", new Tag("CS2103").toString());
    }

    @Test
    public void equals_sameNameInDifferentCase_sameTag() {
        // #Fun and #fun must be one tag, or a task could end up showing both.
        assertEquals(new Tag("fun"), new Tag("FUN"));
    }

    @Test
    public void constructor_nameWithOtherCharacters_exceptionThrown() {
        assertThrows(IllegalArgumentException.class, () -> new Tag(""));
        assertThrows(IllegalArgumentException.class, () -> new Tag("to-do"));
        assertThrows(IllegalArgumentException.class, () -> new Tag("fun school"));
        // A pipe inside a tag would break the save file's separators.
        assertThrows(IllegalArgumentException.class, () -> new Tag("a|b"));
    }

    @Test
    public void isValidText_hashThenLettersAndDigits_accepted() {
        assertTrue(Tag.isValidText("#fun"));
        assertTrue(Tag.isValidText("#CS2103"));
    }

    @Test
    public void isValidText_otherText_rejected() {
        assertFalse(Tag.isValidText("fun"));
        assertFalse(Tag.isValidText("#"));
        assertFalse(Tag.isValidText("##fun"));
        assertFalse(Tag.isValidText("#to-do"));
        assertFalse(Tag.isValidText("#fun #school"));
        assertFalse(Tag.isValidText(""));
    }

    @Test
    public void fromText_validText_returnsTagWithoutHash() {
        assertEquals(new Tag("fun"), Tag.fromText("#Fun"));
    }

    @Test
    public void fromText_invalidText_exceptionThrown() {
        assertThrows(IllegalArgumentException.class, () -> Tag.fromText("fun"));
    }
}
