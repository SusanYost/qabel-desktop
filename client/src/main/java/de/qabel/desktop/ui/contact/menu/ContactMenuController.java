package de.qabel.desktop.ui.contact.menu;


import de.qabel.core.config.Contact;
import de.qabel.core.config.ContactExportImport;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.core.exceptions.QblDropInvalidURL;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.TransactionManager;
import de.qabel.core.repository.exception.EntityNotFoundException;
import de.qabel.core.repository.exception.PersistenceException;
import de.qabel.desktop.config.ClientConfig;
import de.qabel.desktop.ui.AbstractController;
import de.qabel.desktop.ui.contact.index.IndexSearchView;
import de.qabel.desktop.ui.util.Popup;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.json.JSONException;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class ContactMenuController extends AbstractController implements Initializable {

    private ResourceBundle resourceBundle;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private ClientConfig clientConfiguration;

    @Inject
    Pane layoutWindow;

    @Inject
    private TransactionManager transactionManager;

    @FXML
    public AnchorPane menuContact;

    @FXML
    VBox vboxMenu;

    @FXML
    Button importFromFile;
    @FXML
    Button importFromQR;
    @FXML
    Button searchButton;
    @FXML
    Button enterContact;
    @FXML
    Button exportContactsToFile;
    @FXML
    Button exportContactsToQR;

    private Identity identity;

    private static ImageView importFromFileImageView = setImageView(loadImage("/img/import_black.png"));
    private static ImageView importFromQRImageView = setImageView(loadImage("/img/qrcode.png"));
    private static ImageView searchButtonImageView = setImageView(loadImage("/img/cloud_search.png"));
    private static ImageView enterContactImageView = setImageView(loadImage("/img/pencil.png"));
    private static ImageView exportContactsToFileImageView = setImageView(loadImage("/img/export.png"));
    private static ImageView exportContactsToQRImageView = setImageView(loadImage("/img/qrcode.png"));
    private FileChooser.ExtensionFilter qcoExtensionFilter;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        resourceBundle = resources;
        identity = clientConfiguration.getSelectedIdentity();
        qcoExtensionFilter = new FileChooser.ExtensionFilter(resourceBundle.getString("qcoExtensionFilterLabel"), "*.qco");
        createButtonsGraphics();
        createButtonsTooltip();
    }

    private void createButtonsGraphics() {
        importFromFile.setGraphic(importFromFileImageView);
        importFromQR.setGraphic(importFromQRImageView);
        searchButton.setGraphic(searchButtonImageView);
        enterContact.setGraphic(enterContactImageView);
        exportContactsToFile.setGraphic(exportContactsToFileImageView);
        exportContactsToQR.setGraphic(exportContactsToQRImageView);
    }

    private void createButtonsTooltip() {
        Tooltip.install(importFromFile, new Tooltip(resourceBundle.getString("contactImport")));
        Tooltip.install(searchButton, new Tooltip(resourceBundle.getString("searchContact")));
        Tooltip.install(enterContact, new Tooltip(resourceBundle.getString("enterContactManually")));
        Tooltip.install(exportContactsToFile, new Tooltip(resourceBundle.getString("contactExport")));
        Tooltip.install(exportContactsToQR, new Tooltip(resourceBundle.getString("contactExport")));
    }

    private static Image loadImage(String resourcePath) {
        return new Image(ContactMenuController.class.getResourceAsStream(resourcePath), 32, 32, true, true);
    }

    private static ImageView setImageView(Image image) {
        return new ImageView(image);
    }

    public void handleExportContactsButtonAction() throws EntityNotFoundException, IOException, JSONException {
        tryOrAlert(() -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(resourceBundle.getString("exportContactsToFile"));
            chooser.setSelectedExtensionFilter(qcoExtensionFilter);
            chooser.setInitialFileName("Contacts.qco");
            File file = chooser.showSaveDialog(layoutWindow.getScene().getWindow());
            if (file != null) {
                exportContacts(file);
            }
        });
    }

    public void exportContacts(File file) throws EntityNotFoundException, IOException, JSONException, PersistenceException {
        Contacts contacts = contactRepository.find(identity);
        String jsonContacts = ContactExportImport.exportContacts(contacts);
        writeStringInFile(jsonContacts, file);
    }

    public void handleImportContactsButtonAction() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(resourceBundle.getString("importContactFromFile"));
        chooser.getExtensionFilters().add(qcoExtensionFilter);
        File file = chooser.showOpenDialog(layoutWindow.getScene().getWindow());
        try {
            if (file != null) {
                importContacts(file);
            }
        } catch (IOException | PersistenceException | JSONException | QblDropInvalidURL | URISyntaxException e) {
            alert(resourceBundle.getString("alertImportContactFail"), e);
        }
    }

    public void importContacts(File file) throws IOException, URISyntaxException, QblDropInvalidURL, PersistenceException, JSONException {
        String content = readFile(file);
        identity = clientConfiguration.getSelectedIdentity();
        transactionManager.transactional(() -> {
            try {
                Contacts contacts = ContactExportImport.parseContactsForIdentity(identity, content);
                for (Contact c : contacts.getContacts()) {
                    contactRepository.save(c, identity);
                }
            } catch (Exception ignore) {
                Contact c = ContactExportImport.parseContactForIdentity(content);
                contactRepository.save(c, identity);
            }
        });
    }

    Popup searchPopup;
    @FXML
    public void search() {
        new IndexSearchView().getViewAsync(searchView -> {
            searchPopup = new Popup(layoutWindow, searchView);
            close();
            searchPopup.show();
        });
    }

    public void close() {
        closeHandler.run();
    }

    private Runnable closeHandler = () -> {};
    public void onClose(Runnable closeHandler) {
        this.closeHandler = closeHandler;
    }
}
