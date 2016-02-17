package de.qabel.desktop.ui.remotefs.dialog;

import de.qabel.desktop.exceptions.QblStorageException;
import de.qabel.desktop.storage.*;
import de.qabel.desktop.storage.cache.CachedBoxNavigation;
import de.qabel.desktop.ui.remotefs.BoxObjectTreeCell;
import de.qabel.desktop.ui.remotefs.LazyBoxFolderTreeItem;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;

import java.nio.file.Path;
import java.util.ResourceBundle;

public class RemoteFSDirectoryChooser extends RemoteFSChooser {

	public RemoteFSDirectoryChooser(ResourceBundle resources, BoxVolume volume) throws QblStorageException {
		super(resources, volume);
	}

	@Override
	public void changed(ObservableValue<? extends TreeItem<BoxObject>> observable, TreeItem<BoxObject> oldValue, TreeItem<BoxObject> newValue) {
		if (!(newValue instanceof LazyBoxFolderTreeItem)) {
			selectedProperty.setValue(null);
			return;
		}
		LazyBoxFolderTreeItem folderItem = (LazyBoxFolderTreeItem)newValue;
		ReadOnlyBoxNavigation navigation = folderItem.getNavigation();
		if (!(navigation instanceof CachedBoxNavigation)) {
			selectedProperty.setValue(null);
			return;
		}
		Path result = ((CachedBoxNavigation) navigation).getPath();
		selectedProperty.setValue(result);
	}
}
