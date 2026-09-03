package ted;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for Ted's main window.
 * <p>
 * Holds the conversation together: it puts what the user typed and what Ted
 * replied into the dialog container, and closes the window once Ted has said
 * goodbye. The layout itself is described in {@code MainWindow.fxml}.
 */
public class MainWindow extends AnchorPane {
    /** How long Ted's goodbye stays on screen before the window closes. */
    private static final Duration GOODBYE_PAUSE = Duration.seconds(1.5);

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox dialogContainer;
    @FXML
    private TextField userInput;
    @FXML
    private Button sendButton;

    private final Image userImage = new Image(this.getClass().getResourceAsStream("/images/DaUser.png"));
    private final Image tedImage = new Image(this.getClass().getResourceAsStream("/images/DaTed.png"));

    /** The chatbot answering in this window, supplied by {@link #setTed(Ted)}. */
    private Ted ted;

    /** Keeps the newest message in view as the conversation grows. */
    @FXML
    public void initialize() {
        scrollPane.vvalueProperty().bind(dialogContainer.heightProperty());
    }

    /**
     * Gives this window the chatbot it should talk to, and shows its greeting.
     *
     * @param ted the chatbot answering in this window.
     */
    public void setTed(Ted ted) {
        this.ted = ted;
        dialogContainer.getChildren().add(DialogBox.getTedDialog(ted.getGreeting(), tedImage));
    }

    /**
     * Shows what the user typed and how Ted replied, then clears the text field.
     * Called by the FXML for both the Send button and the Enter key.
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        String response = ted.getResponse(input);
        if (response.isEmpty()) {
            // A blank line gets no reply, so nothing is worth showing.
            userInput.clear();
            return;
        }

        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input, userImage),
                DialogBox.getTedDialog(response, tedImage)
        );
        userInput.clear();

        if (ted.isExit()) {
            // The goodbye would flash past if the window closed immediately.
            userInput.setDisable(true);
            sendButton.setDisable(true);
            PauseTransition pause = new PauseTransition(GOODBYE_PAUSE);
            pause.setOnFinished(event -> Platform.exit());
            pause.play();
        }
    }
}
