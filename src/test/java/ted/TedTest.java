package ted;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link Ted}'s one-message-at-a-time interface, which the GUI relies on.
 * <p>
 * Every test keeps its save file inside a {@link TempDir}, so running the tests
 * can never disturb the user's own tasks.
 */
public class TedTest {
    /**
     * Verifies that a command with leading spaces is recognized.
     *
     * @param tempDir The temporary directory for the save file.
     */
    @Test
    public void getResponse_leadingSpaces_commandRecognized(@TempDir Path tempDir) {
        // The GUI passes its text field on untrimmed, so Ted must cope with padding.
        Ted ted = new Ted(tempDir.resolve("ted.txt").toString());
        assertTrue(ted.getResponse("   list").contains("You have no tasks yet."));
    }

    /**
     * Verifies that a warning about unreadable save data follows the welcome message.
     *
     * @param tempDir The temporary directory for the save file.
     * @throws IOException If the test save file cannot be written.
     */
    @Test
    public void getGreeting_unreadableSaveLine_warningFollowsWelcome(@TempDir Path tempDir) throws IOException {
        Path dataFile = tempDir.resolve("ted.txt");
        Files.write(dataFile, List.of("not a task"));

        String greeting = new Ted(dataFile.toString()).getGreeting();
        int welcomeIndex = greeting.indexOf("Hello! I'm Ted.");
        int warningIndex = greeting.indexOf("Skipped 1 unreadable line");
        assertTrue(welcomeIndex >= 0 && warningIndex > welcomeIndex);
    }
}
