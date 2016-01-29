package de.qabel.desktop.storage;

import de.qabel.desktop.exceptions.QblStorageException;

import java.util.List;

public interface ReadOnlyBoxNavigation {
	/**
	 * Create a new navigation object that starts at another {@link BoxFolder}
	 *
	 * @param target Target folder that is a direct subfolder
	 * @return {@link BoxNavigation} for the subfolder
	 * @throws QblStorageException
	 */
	BoxNavigation navigate(BoxFolder target) throws QblStorageException;

	/**
	 * Create a new navigation object that starts at another {@link BoxExternal}
	 *
	 * @param target Target shared folder that is mounted in the current folder
	 * @return {@link BoxNavigation} for the external share
	 * @throws QblStorageException
	 */
	BoxNavigation navigate(BoxExternal target);

	/**
	 * Create a list of all files in the current folder
	 *
	 * @return list of files
	 * @throws QblStorageException
	 */
	List<BoxFile> listFiles() throws QblStorageException;

	/**
	 * Create a list of all folders in the current folder
	 *
	 * @return list of folders
	 * @throws QblStorageException
	 */
	List<BoxFolder> listFolders() throws QblStorageException;

	/**
	 * Create a list of external shares in the current folder
	 *
	 * @return list of external shares
	 * @throws QblStorageException
	 */
	List<BoxExternal> listExternals() throws QblStorageException;

	/**
	 * Navigate to subfolder by name
	 */
	BoxNavigation navigate(String folderName) throws QblStorageException;

	BoxFolder getFolder(String name) throws QblStorageException;

	boolean hasFolder(String name) throws QblStorageException;

	BoxFile getFile(String name) throws QblStorageException;

	boolean hasFile(String name) throws QblStorageException;
}
