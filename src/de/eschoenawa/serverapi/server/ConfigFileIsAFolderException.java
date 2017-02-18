package de.eschoenawa.serverapi.server;

public class ConfigFileIsAFolderException extends Exception {
	
	private static final long serialVersionUID = 6689242686199473075L;

	public ConfigFileIsAFolderException() {
		super("The Config-File is a Folder! Cannot create config file.");
	}

}
