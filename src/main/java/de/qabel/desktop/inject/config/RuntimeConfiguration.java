package de.qabel.desktop.inject.config;

import de.qabel.desktop.repository.sqlite.ClientDatabase;
import javafx.stage.Stage;

import java.net.URI;
import java.nio.file.Path;

public interface RuntimeConfiguration {
    URI getDropUri();
    URI getAccountingUri();
    URI getBlockUri();
    Path getPersistenceDatabaseFile();
    Stage getPrimaryStage();

    ClientDatabase getConfigDatabase();
}
