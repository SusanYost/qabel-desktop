package de.qabel.desktop.inject;

import de.qabel.box.storage.factory.BoxVolumeFactory;
import de.qabel.chat.repository.ChatDropMessageRepository;
import de.qabel.chat.service.ChatService;
import de.qabel.core.accounting.BoxClient;
import de.qabel.core.config.factory.DropUrlGenerator;
import de.qabel.core.config.factory.IdentityBuilderFactory;
import de.qabel.core.event.EventDispatcher;
import de.qabel.core.index.IndexService;
import de.qabel.core.repository.AccountRepository;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.core.repository.TransactionManager;
import de.qabel.desktop.SharingService;
import de.qabel.desktop.config.ClientConfig;
import de.qabel.desktop.config.FilesAbout;
import de.qabel.desktop.config.factory.BoxSyncConfigFactory;
import de.qabel.desktop.crashReports.CrashReportHandler;
import de.qabel.desktop.daemon.NetworkStatus;
import de.qabel.desktop.daemon.drop.DropDaemon;
import de.qabel.desktop.daemon.management.TransferManager;
import de.qabel.desktop.daemon.sync.SyncDaemon;
import de.qabel.desktop.repository.BoxSyncRepository;
import de.qabel.desktop.repository.DropMessageRepository;
import de.qabel.desktop.repository.ShareNotificationRepository;
import de.qabel.desktop.ui.actionlog.item.renderer.FXMessageRendererFactory;
import de.qabel.desktop.ui.connector.DropConnector;
import de.qabel.desktop.ui.util.FileChooserFactory;
import de.qabel.desktop.util.Translator;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import rx.Scheduler;

import java.io.IOException;
import java.net.URI;
import java.util.ResourceBundle;

public interface DesktopServices {
    @Create(name="indexService")
    IndexService getIndexService();

    @Create(name="chatService")
    ChatService getChatService();

    @Create(name = "sharingService")
    SharingService getSharingService();

    @Create(name = "loadManager")
    @Create(name = "transferManager")
    TransferManager getTransferManager();

    @Create(name = "identityRepository")
    IdentityRepository getIdentityRepository();

    @Create(name = "identityBuilderFactory")
    IdentityBuilderFactory getIdentityBuilderFactory();

    @Create(name = "accountingRepository")
    AccountRepository getAccountRepository();

    @Create(name = "dropUrlGenerator")
    DropUrlGenerator getDropUrlGenerator();

    @Create(name = "accountingUri")
    URI getAccountingUri();

    @Create(name = "blockUri")
    URI getBlockUri();

    @Create(name = "contactRepository")
    ContactRepository getContactRepository();

    @Create(name = "dropMessageRepository")
    DropMessageRepository getDropMessageRepository();

    @Create(name="chatDropMessageRepository")
    ChatDropMessageRepository getChatDropMessageRepository();

    @Create(name = "clientConfiguration")
    @Create(name = "config")
    ClientConfig getClientConfiguration();

    @Create(name = "networkStatus")
    NetworkStatus getNetworkStatus();

    @Create(name = "dropConnector")
    DropConnector getDropConnector();

    @Create(name = "reportHandler")
    CrashReportHandler getCrashReportHandler();

    @Create(name = "messageRendererFactory")
    FXMessageRendererFactory getDropMessageRendererFactory();

    @Create(name = "boxVolumeFactory")
    BoxVolumeFactory getBoxVolumeFactory() throws IOException;

    @Create(name = "boxClient")
    BoxClient getBoxClient();

    @Create(name = "shareNotificationRepository")
    ShareNotificationRepository getShareNotificationRepository();

    @Create(name = "boxSyncConfigRepository")
    @Create(name = "boxSyncRepository")
    BoxSyncRepository getBoxSyncConfigRepository();

    @Create(name = "primaryStage")
    Stage getPrimaryStage();

    @Create(name = "transactionManager")
    TransactionManager getTransactionManager();

    @Create(name = "resourceBundle")
    ResourceBundle getResourceBundle();

    @Create(name = "translator")
    Translator getTranslator();

    @Create(name = "syncDaemon")
    SyncDaemon getSyncDaemon();

    @Create(name = "dropDaemon")
    DropDaemon getDropDaemon();

    @Create(name = "boxSyncConfigFactory")
    BoxSyncConfigFactory getBoxSyncConfigFactory();

    @Create(name="layoutWindow")
    Pane getLayoutWindow();

    @Create(name="aboutFilesContent")
    FilesAbout getAboutFilesContent();

    @Create(name="currentVersion")
    String getCurrentVersion();

    @Create(name="remoteDebounceTimeout")
    default int getRemoteDebounceTimeout() {
        return 500;
    }

    @Create(name="debounceTimeout")
    default int getDebounceTimeout() {
        return 250;
    }

    @Create(name="eventDispatcher")
    @Create(name="eventSource")
    @Create(name="eventSink")
    EventDispatcher getEventDispatcher();

    @Create(name="fileChooserFactory")
    FileChooserFactory getFileChooserFactory();

    @Create(name="fxScheduler")
    Scheduler getFxScheduler();

    @Create(name="ioScheduler")
    Scheduler getIoScheduler();

    @Create(name="computationScheduler")
    Scheduler getComputationScheduler();
}
