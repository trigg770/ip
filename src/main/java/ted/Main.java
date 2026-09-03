package ted;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * The JavaFX application behind Ted's window.
 * <p>
 * Its only job is to load the window described in {@code MainWindow.fxml}, hand
 * the controller a {@link Ted} to talk to, and show the result. The window's
 * appearance lives in the FXML and its behaviour in {@link MainWindow}.
 */
public class Main extends Application {
    /** The chatbot answering in this window. */
    private final Ted ted = new Ted();

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/view/MainWindow.fxml"));
            AnchorPane root = fxmlLoader.load();
            stage.setScene(new Scene(root));
            stage.setTitle("Ted");
            // Below these the send button and text field start overlapping.
            stage.setMinHeight(220);
            stage.setMinWidth(417);
            fxmlLoader.<MainWindow>getController().setTed(ted);
            stage.show();
        } catch (IOException e) {
            // Without its window there is nothing left for the app to do, so the
            // failure is reported rather than swallowed.
            e.printStackTrace();
        }
    }
}
