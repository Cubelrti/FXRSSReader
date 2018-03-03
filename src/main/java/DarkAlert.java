import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;

/**
 * The type Dark alert.
 */
public class DarkAlert extends Alert {
    private DarkAlert(AlertType alertType) {
        super(alertType);
        setStyle();
    }

    private void setStyle(){
        DialogPane dialogPane = this.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("Dialogs.css").toExternalForm());
        dialogPane.getStyleClass().add("myDialog");
    }

    /**
     * Make simple alert.
     *
     * @param title   the title
     * @param header  the header
     * @param content the content
     */
    static void makeSimpleAlert(String title, String header, String content){
        Alert alert = new DarkAlert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
