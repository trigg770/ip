package ted;

import javafx.application.Application;

/**
 * Starts the GUI.
 * <p>
 * JavaFX refuses to start when the class holding {@code main} extends
 * {@link Application} and the JavaFX libraries are on the classpath rather than
 * the module path, which is how this project depends on them. Launching from a
 * class that does not itself extend {@link Application} avoids that, so this
 * class exists only to call {@link Application#launch}.
 */
public class Launcher {
    /**
     * Starts Ted's GUI.
     *
     * @param args command-line arguments, which Ted does not use.
     */
    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
