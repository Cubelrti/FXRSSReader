import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;
import org.apache.http.HttpResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;
import parser.RSSFeed;
import parser.RSSItem;
import util.network;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Optional;

/**
 * The type Ui controller.
 */
public class UIController {
    private ReaderApplication _mainApp;
    private ObservableList<RSSFeed> list = FXCollections.observableArrayList();
    private ObservableList<RSSItem> itemList = FXCollections.observableArrayList();
    private RSSFeed readLater = new RSSFeed("Read Later", null, new ArrayList<>());
    private Thread refreshThread;
    private int refreshingDelay = 60000;

    //region FXML Object
    @FXML
    private WebView webview;

    @FXML
    private ListView<RSSFeed> feeds;

    @FXML
    private ListView<RSSItem> items;

    @FXML
    private Label leftStatus;

    @FXML
    private SplitPane splitPane;

    @FXML
    private MenuItem isAutoRefresh;


    //endregion

    /**
     * setting reference for application.
     * this keeps control from application->ui.
     *
     * @param application the readerApplication
     */
    void setMainApp(ReaderApplication application) {
        this._mainApp = application;
        //load from io
        ArrayList<RSSFeed> items = _mainApp.getIO().getFeeds();
        ArrayList<RSSItem> favorites = _mainApp.getIO().getFavorites();
        if (items == null || readLater == null) leftStatus.setText("Cannot connect to db. Using locally.");
        else {
            list.addAll(items);
            readLater.rssItems.addAll(favorites);
        }
    }

    /**
     * Handle close button.
     *
     * @param ev the event
     */
    void handleCloseButton(WindowEvent ev) {
        isAutoRefresh.setText("Auto Refresh");
        if (refreshThread != null) refreshThread.interrupt();
        Platform.exit();
    }

    private void showRSSItem(RSSFeed feed) {
        if (feed != null) {

            ObservableList<RSSItem> list = FXCollections.observableArrayList(feed.rssItems);
            items.setItems(list);
        }
        items.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    WebEngine engine = webview.getEngine();
                    if (newValue != null) {
                        engine.loadContent(newValue.content);
                        webview.getEngine().getLoadWorker().stateProperty().addListener((ov) -> {
                            Document document = webview.getEngine().getDocument();
                            if (document != null) {
                            Node node = document.getElementById("external");
                                EventTarget eventTarget = (EventTarget) node;
                                if (eventTarget != null)
                                    eventTarget.addEventListener("click", (event) -> {
                                    HTMLAnchorElement anchorElement = (HTMLAnchorElement) event.getCurrentTarget();
                                    String href = anchorElement.getHref();
                                    URI uri;
                                    try {
                                        uri = new URI(href);
                                        Desktop.getDesktop().browse(uri);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    event.preventDefault();
                                }, false);
                            }
                        });

                    }
                    engine.setUserStyleSheetLocation(getClass().getResource("PageStyle.css").toString());
                }
        );
    }

    private void loadFeed(String url) {
        if (url == null) return;
        new Thread(() -> {
            Platform.runLater(() -> leftStatus.setText("Loading in background..."));
            try {
                HttpResponse response = network.getResponse(url);
                InputStream stream = response.getEntity().getContent();
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(stream));

                // if already added.
                Optional<RSSFeed> oldFeed = list.stream().filter(t -> t.name.equals(feed.getTitle())).findFirst();
                if (oldFeed.isPresent()) {
                    // already added.
                    for (SyndEntry ent : feed.getEntries()) {
                        if (oldFeed.get().rssItems.stream().noneMatch(t -> t.name.equals(ent.getTitle()))) {
                            RSSItem newItem = new RSSItem(ent);
                            oldFeed.get().rssItems.add(0, newItem);
                            // save to io
                            _mainApp.getIO().insertItem(newItem, oldFeed.get().name, false);
                        }
                    }
                    Platform.runLater(() -> {
                        // reload ui
                        int oldIndex = feeds.getSelectionModel().getSelectedIndex();
                        feeds.getSelectionModel().select(oldIndex);
                    });
                } else {
                    RSSFeed parsedFeed = new RSSFeed(feed, url);
                    _mainApp.getIO().insertFeed(parsedFeed);
                    Platform.runLater(() -> {
                        list.add(parsedFeed);
                        leftStatus.setText("Loaded " + feed.getEntries().size() + " entries from " + feed.getTitle());
                    });
                }
            } catch (IOException | FeedException e) {
                Platform.runLater(() -> leftStatus.setText("Error loading feed. " + e.getMessage()));
            }

        }).start();
    }

    private void searchItem(String queryString) {
        if (queryString.isEmpty() || feeds.getSelectionModel().getSelectedItem() == null) return;
        new Thread(() -> {
            Platform.runLater(() -> leftStatus.setText("Searching for " + queryString));
            for (RSSItem item : feeds.getSelectionModel().getSelectedItem().rssItems) {
                if (item.content.contains(queryString)) {
                    Platform.runLater(() -> items.getSelectionModel().select(item));
                }
            }
        }).start();
    }

    /**
     * Sets division.
     */
    void setDivision() {
        splitPane.setDividerPositions(0.1f, 0.3f);
    }

    @FXML
    private void initialize() {
        feeds.setItems(list);
        list.add(readLater);
        feeds.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showRSSItem(newValue)
        );
        items.setItems(itemList);
        WebEngine engine = webview.getEngine();
        engine.setUserStyleSheetLocation(getClass().getResource("PageStyle.css").toString());
        engine.loadContent(
                "<h2>Welcome to RSS Reader ver. ddf</h2>" +
                        "<hr/>" +
                        "<p>Select a feed to continue.</p>" +
                        "<p>Or you can import a feed from network. Just use " +
                        "File - Load Samples from Internet to import some feeds.</p>"
        );
    }

    /**
     * Handle open from web.
     */
    @FXML
    protected void handleOpenFromWeb() {
        TextInputDialog question = DarkQuestion.makeSimpleQuestion(
                "https://www.zhihu.com/rss",
                "Enter RSS Source",
                "Enter an RSS Source from Internet",
                "URL:"
        );
        Optional<String> result = question.showAndWait();
        result.ifPresent(this::loadFeed);

    }

    /**
     * Handle about menu.
     */
    @FXML
    protected void handleAboutMenu() {
        DarkAlert.makeSimpleAlert("About",
                "RSS Reader ver.Deep ♂ Dark ♂ Fantasy",
                "Original idea: Ruanko soft\nProgrammed and styled by Cubelrti\nCopyright 2017 Cubelrti"
        );
    }

    /**
     * Handle load sample from web.
     */
    @FXML
    protected void handleLoadSampleFromWeb() {
        loadFeed("https://www.huxiu.com/rss/0.xml");
        loadFeed("https://www.zhihu.com/rss");
        loadFeed("http://www.ifanr.com/feed");
        loadFeed("https://www.ithome.com/rss/");
    }

    /**
     * Handle save text file.
     */
    @FXML
    protected void handleSaveTextFile() {
        if(feeds.getSelectionModel().getSelectedItem() == null) {
            DarkAlert.makeSimpleAlert(
                    "Alert",
                    "Select an item first!",
                    "You should select an item from feed first."
            );
            return;
        }
        FileChooser fileChooser = new FileChooser();
        // Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Text files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setTitle("Save Text File");
        File file = fileChooser.showSaveDialog(_mainApp.getPrimaryStage());
        if (file != null) {
            try {
                PrintWriter writer = new PrintWriter(new FileWriter(file));
                for (RSSItem item : feeds.getSelectionModel().getSelectedItem().rssItems) {
                    writer.println("Title: " + item.name);
                    writer.println("Link: " + item.source);
                    writer.println("Author: " + item.author);
                    writer.println("Time Released: " + item.publishedDate);
                    writer.println("----------------------------------------------");
                    writer.println(item.content);
                    writer.println();
                }
                writer.close();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    /**
     * Handle close.
     */
    @FXML
    protected void handleClose() {
        if(feeds.getSelectionModel().getSelectedItem() == null) {
            DarkAlert.makeSimpleAlert(
                    "Alert",
                    "Select an item first!",
                    "You should select an item from feed first."
            );
            return;
        }
        RSSFeed toRemove = feeds.getSelectionModel().getSelectedItem();
        _mainApp.getIO().removeFeed(toRemove.name);
        list.remove(toRemove);
    }

    /**
     * Handle search.
     */
    @FXML
    protected void handleSearch() {
        TextInputDialog question = DarkQuestion.makeSimpleQuestion(
                "",
                "Search",
                "Enter what you want to search",
                ""
        );
        Optional<String> result = question.showAndWait();
        result.ifPresent(this::searchItem);

    }

    /**
     * Handle quit.
     */
    @FXML
    protected void handleQuit() {
        isAutoRefresh.setText("Auto Refresh");
        if (refreshThread != null) refreshThread.interrupt();
        Platform.exit();
    }

    /**
     * Handle open file.
     */
    @FXML
    protected void handleOpenFile() {
        FileChooser fileChooser = new FileChooser();

        // Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "XML files (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show save file dialog
        File file = fileChooser.showOpenDialog(_mainApp.getPrimaryStage());

        if (file != null) {
            try {
                Reader reader = new FileReader(file);
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(reader);
                RSSFeed parsedFeed = new RSSFeed(feed, null);
                list.add(parsedFeed);
            } catch (FileNotFoundException | FeedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle copy.
     */
    @FXML
    protected void handleCopy() {
        if(feeds.getSelectionModel().getSelectedItem() == null) {
            DarkAlert.makeSimpleAlert(
                    "Alert",
                    "Select an item first!",
                    "You should select an item from feed first."
            );
            return;
        }
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        // get current item
        RSSItem item = items.getSelectionModel().getSelectedItem();
        content.putString("Title: " + item.name + System.lineSeparator() +
                "Author: " + item.author + System.lineSeparator() +
                "Published Date" + item.publishedDate + System.lineSeparator() +
                "------------------------------" + System.lineSeparator() +
                item.rawContent);
        clipboard.setContent(content);
        leftStatus.setText("Copied entry.");
    }

    /**
     * Handle read later.
     */
    @FXML
    protected void handleReadLater() {
        RSSItem item = items.getSelectionModel().getSelectedItem();
        if (item == null) return;
        readLater.rssItems.add(items.getSelectionModel().getSelectedItem());
        // save to io.
        _mainApp.getIO().insertItem(item, "readLater", true);
        leftStatus.setText("Added to read later.");
    }

    /**
     * Handle refresh.
     */
    @FXML
    protected void handleRefresh() {
        if (isAutoRefresh.getText().equals("Auto Refresh")) {
            isAutoRefresh.setText("Auto Refresh √");
            refreshThread = new Thread(() -> {
                while (isAutoRefresh.getText().equals("Auto Refresh √")) {
                    list.forEach(t -> loadFeed(t.src));
                    try {
                        Thread.sleep(refreshingDelay);
                    } catch (InterruptedException ignored) {
                    }
                }
            });
            refreshThread.start();
        } else {
            isAutoRefresh.setText("Auto Refresh");
        }
    }

    /**
     * Handle preferences.
     */
    @FXML
    protected void handlePreferences() {
        TextInputDialog question = DarkQuestion.makeSimpleQuestion(
                "",
                "Refresh Timeout",
                "Enter delay between refreshing:",
                ""
        );
        Optional<String> result = question.showAndWait();
        try {
            result.ifPresent(delay -> refreshingDelay = Integer.parseInt(delay));
        } catch (NumberFormatException e) {
            DarkAlert.makeSimpleAlert(
                    "Error",
                    "You should enter an integer",
                    "Wrong input"
            );
        }
    }

    @FXML
    protected void handleRemoveDateBefore(){
        TextInputDialog question = DarkQuestion.makeSimpleQuestion(
                "",
                "Remove items before",
                "Enter how many days before",
                "Day:"
        );
        Optional<String> result = question.showAndWait();
        try {
            result.ifPresent(days -> _mainApp.getIO().removeDateBefore(Integer.parseInt(days)));
        } catch (NumberFormatException e) {
            DarkAlert.makeSimpleAlert(
                    "Error",
                    "You should enter an integer",
                    "Wrong input"
            );
        }
    }
}
