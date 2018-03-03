import javafx.scene.control.DialogPane;
import javafx.scene.control.TextInputDialog;

/**
 * The type Dark question.
 */
public class DarkQuestion extends TextInputDialog {
    private DarkQuestion(String defaultValue) {
        super(defaultValue);
        setStyle();
    }

    private void setStyle(){
        DialogPane dialogPane = this.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("Dialogs.css").toExternalForm());
        dialogPane.getStyleClass().add("myDialog");
    }

    /**
     * Make simple question text input dialog.
     *
     * @param defaultValue the default value
     * @param title        the title
     * @param header       the header
     * @param prefix       the prefix
     * @return the text input dialog
     */
    static TextInputDialog makeSimpleQuestion(String defaultValue, String title, String header, String prefix){
        TextInputDialog dialog = new DarkQuestion(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(prefix);
        return dialog;

    }
}
