import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import util.io;

/**
 * The type Reader application.
 */
public class ReaderApplication extends Application {
    private UIController controller;
    private Stage _primaryStage;

    /**
     * Gets io.
     *
     * @return the io
     */
    io getIO() {
        return _io;
    }

    private io _io;

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) throws Exception {

        //handle db
        _io = new io();
        _io.connect("jdbc:postgresql://localhost:5432/rssreader");
        _io.createTable();
        primaryStage.setTitle("RSS Reader");
        primaryStage.getIcons().add(new Image(getClass().getResource("icon/app_icon.png").toString()));
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("Main.fxml"));
        VBox rootLayout = loader.load();
        Scene scene = new Scene(rootLayout,1100,768);
        primaryStage.setScene(scene);
        primaryStage.show();
        _primaryStage = primaryStage;
        primaryStage.maximizedProperty().addListener((ov, t, t1) -> controller.setDivision());
        controller = loader.getController();
        controller.setMainApp(this);
        primaryStage.setOnCloseRequest(controller::handleCloseButton);

    }

    /**
     * Gets primary stage.
     *
     * @return the primary stage
     */
    Window getPrimaryStage() {
        return _primaryStage;
    }


}
