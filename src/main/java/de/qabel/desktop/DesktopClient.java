package de.qabel.desktop;

import com.airhacks.afterburner.injection.Injector;
import de.qabel.core.accounting.AccountingHTTP;
import de.qabel.core.accounting.AccountingProfile;
import de.qabel.core.config.*;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;
import de.qabel.desktop.config.ClientConfiguration;
import de.qabel.desktop.config.factory.ClientConfigurationFactory;
import de.qabel.desktop.config.factory.DropUrlGenerator;
import de.qabel.desktop.daemon.drop.DropDaemon;
import de.qabel.desktop.config.factory.*;
import de.qabel.desktop.daemon.management.DefaultTransferManager;
import de.qabel.desktop.daemon.management.MonitoredTransferManager;
import de.qabel.desktop.daemon.share.ShareNotificationHandler;
import de.qabel.desktop.daemon.sync.SyncDaemon;
import de.qabel.desktop.daemon.sync.worker.DefaultSyncerFactory;
import de.qabel.desktop.repository.AccountRepository;
import de.qabel.desktop.repository.ClientConfigurationRepository;
import de.qabel.desktop.repository.DropMessageRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.desktop.repository.persistence.*;
import de.qabel.desktop.ui.LayoutView;
import de.qabel.desktop.ui.accounting.login.LoginView;
import de.qabel.desktop.ui.actionlog.item.renderer.MessageRendererFactory;
import de.qabel.desktop.ui.actionlog.item.renderer.PlaintextMessageRenderer;
import de.qabel.desktop.ui.actionlog.item.renderer.ShareNotificationRenderer;
import de.qabel.desktop.ui.connector.HttpDropConnector;
import de.qabel.desktop.ui.inject.RecursiveInjectionInstanceSupplier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DesktopClient extends Application {
	private static final Logger logger = LoggerFactory.getLogger(DesktopClient.class.getSimpleName());
	private static final String TITLE = "Qabel Desktop Client";
	private static String DATABASE_FILE = "db.sqlite";
	private final Map<String, Object> customProperties = new HashMap<>();
	private LayoutView view;
	private HttpDropConnector dropConnector = new HttpDropConnector();
	private PersistenceDropMessageRepository dropMessageRepository;
	private PersistenceContactRepository contactRepository;
	private BoxVolumeFactory boxVolumeFactory;
	private Stage primaryStage;
	private MonitoredTransferManager transferManager;
	private MessageRendererFactory rendererFactory = new MessageRendererFactory();
	private BlockSharingService sharingService;
	private ExecutorService executorService = Executors.newCachedThreadPool();
	private ClientConfiguration config;

	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			DATABASE_FILE = args[0];
		}
		UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		primaryStage = stage;
		setUserAgentStylesheet(STYLESHEET_MODENA);

		config = initDiContainer();

		SceneAntialiasing aa = SceneAntialiasing.BALANCED;
		primaryStage.getIcons().setAll(new javafx.scene.image.Image(getClass().getResourceAsStream("/logo-invert_small.png")));
		Scene scene;


		Platform.setImplicitExit(false);
		primaryStage.setTitle(TITLE);
		scene = new Scene(new LoginView().getView(), 370, 550, true, aa);
		primaryStage.setScene(scene);

		config.addObserver((o, arg) -> {
			Platform.runLater(() -> {
				if (arg instanceof Account) {
					try {
						ClientConfiguration configuration = (ClientConfiguration) customProperties.get("clientConfiguration");
						Account acc = (Account) arg;
						AccountingServer server = new AccountingServer(new URI(acc.getProvider()), acc.getUser(), acc.getAuth());
						AccountingHTTP accountingHTTP = new AccountingHTTP(server, new AccountingProfile());

						BoxVolumeFactory factory = new BlockBoxVolumeFactory(configuration.getDeviceId().getBytes(), accountingHTTP);
						boxVolumeFactory = new CachedBoxVolumeFactory(factory);
						customProperties.put("boxVolumeFactory", boxVolumeFactory);
						sharingService = new BlockSharingService(dropMessageRepository, dropConnector);
						customProperties.put("sharingService", sharingService);

						new Thread(getSyncDaemon(config)).start();
						new Thread(getDropDaemon(config)).start();
						view = new LayoutView();
						Parent view = this.view.getView();
						Scene layoutScene = new Scene(view, 800, 600, true, aa);
						Platform.runLater(() -> primaryStage.setScene(layoutScene));

						if (config.getSelectedIdentity() != null) {
							addShareMessageRenderer(config.getSelectedIdentity());
						}
					} catch (Exception e) {
						logger.error("failed to init background services: " + e.getMessage(), e);
						//TODO to something with the fault
					}
				} else if (arg instanceof Identity) {
					addShareMessageRenderer((Identity) arg);
				}
			});
		});

		dropMessageRepository.addObserver(new ShareNotificationHandler(config));

		setTrayIcon(primaryStage);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				Platform.exit();
			}
		});
		primaryStage.show();
	}

	private void addShareMessageRenderer(Identity arg) {
		executorService.submit(() -> {
			ShareNotificationRenderer renderer = new ShareNotificationRenderer(((BoxVolumeFactory) customProperties.get("boxVolumeFactory")).getVolume(config.getAccount(), arg).getReadBackend(), sharingService);
			rendererFactory.addRenderer(DropMessageRepository.PAYLOAD_TYPE_SHARE_NOTIFICATION, renderer);
		});
	}

	protected SyncDaemon getSyncDaemon(ClientConfiguration config) {
		new Thread(transferManager, "TransactionManager").start();
		return new SyncDaemon(config.getBoxSyncConfigs(), new DefaultSyncerFactory(boxVolumeFactory, transferManager));
	}

	protected DropDaemon getDropDaemon(ClientConfiguration config) throws PersistenceException, EntityNotFoundExcepion {
		return  new DropDaemon(config, dropConnector,contactRepository, dropMessageRepository);
	}

	private ClientConfiguration initDiContainer() throws QblInvalidEncryptionKeyException, URISyntaxException {
		Persistence<String> persistence = new SQLitePersistence(DATABASE_FILE);
		transferManager = new MonitoredTransferManager(new DefaultTransferManager());
		customProperties.put("loadManager", transferManager);
		customProperties.put("transferManager", transferManager);
		customProperties.put("persistence", persistence);
		customProperties.put("dropUrlGenerator", new DropUrlGenerator("https://qdrop.prae.me"));
		PersistenceIdentityRepository identityRepository = new PersistenceIdentityRepository(persistence);
		customProperties.put("identityRepository", identityRepository);
		PersistenceAccountRepository accountRepository = new PersistenceAccountRepository(persistence);
		customProperties.put("accountRepository", accountRepository);
		contactRepository = new PersistenceContactRepository(persistence);
		customProperties.put("contactRepository", contactRepository);
		dropMessageRepository = new PersistenceDropMessageRepository(persistence);
		customProperties.put("dropMessageRepository", dropMessageRepository);
		customProperties.put("dropConnector", dropConnector);
		ClientConfiguration clientConfig = getClientConfiguration(
				persistence,
				identityRepository,
				accountRepository
		);
		if (!clientConfig.hasDeviceId()) {
			clientConfig.setDeviceId(generateDeviceId());
		}
		PersistenceContactRepository contactRepository = new PersistenceContactRepository(persistence);
		customProperties.put("contactRepository", contactRepository);
		customProperties.put("clientConfiguration", clientConfig);
		customProperties.put("primaryStage", primaryStage);

		rendererFactory.setFallbackRenderer(new PlaintextMessageRenderer());
		customProperties.put("messageRendererFactory", rendererFactory);

		Injector.setConfigurationSource(customProperties::get);
		Injector.setInstanceSupplier(new RecursiveInjectionInstanceSupplier(customProperties));
		return clientConfig;
	}

	private String generateDeviceId() {
		return UUID.randomUUID().toString();
	}

	private ClientConfiguration getClientConfiguration(
			Persistence<String> persistence,
			IdentityRepository identityRepository,
			AccountRepository accountRepository) {
		ClientConfigurationRepository repo = new PersistenceClientConfigurationRepository(
				persistence,
				new ClientConfigurationFactory(), identityRepository, accountRepository);
		final ClientConfiguration config = repo.load();
		config.addObserver((o, arg) -> repo.save(config));
		return config;
	}

	private void setTrayIcon(Stage primaryStage) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {

		SystemTray sTray = SystemTray.getSystemTray();
		primaryStage.setOnCloseRequest(arg0 -> primaryStage.hide());
		JPopupMenu popup = buildSystemTrayJPopupMenu(primaryStage);
		URL url = System.class.getResource("/logo-invert_small.png");
		Image img = Toolkit.getDefaultToolkit().getImage(url);
		TrayIcon icon = new TrayIcon(img, "Qabel");

		icon.setImageAutoSize(true);
		trayIconListener(popup, icon);

		try {
			sTray.add(icon);
		} catch (AWTException e) {
			logger.error("failed to add tray icon: " + e.getMessage(), e);
		}

	}

	private void trayIconListener(final JPopupMenu popup, TrayIcon icon) {
		icon.addMouseListener(new MouseAdapter() {
			boolean visible = false;

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() != MouseEvent.BUTTON1) {
					return;
				}
				popup.setLocation(e.getX(), e.getY());
				visible = !visible;
				popup.setVisible(visible);

			}
		});
	}

	protected JPopupMenu buildSystemTrayJPopupMenu(Stage primaryStage) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
		final JPopupMenu menu = new JPopupMenu();
		final JMenuItem showMenuItem = new JMenuItem("Show");
		final JMenuItem exitMenuItem = new JMenuItem("Exit");

		menu.add(showMenuItem);
		menu.addSeparator();
		menu.add(exitMenuItem);
		showMenuItem.addActionListener(ae -> Platform.runLater(primaryStage::show));
		exitMenuItem.addActionListener(ae -> System.exit(0));

		return menu;
	}
}
