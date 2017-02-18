package de.eschoenawa.serverapi.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Config {
	
	public static final String KEY_VERSION = "version";
	public static final String KEY_PORT = "port";
	public static final String KEY_MAX_CLIENTS = "max_clients";

	static final int DEFAULT_MAX_CLIENTS = 500;
	static final int DEFAULT_PORT = 5050;

	private static Logger logger = LogManager.getLogger(Config.class);
	private static Config config;
	private String path;
	private Config(String path) {
		this.path = path;
	}
	
	public static Config getInstance() {
		if (config == null)
			config = new Config(Server.CFG_PATH);
		return config;
	}
	
	public void createDefaultConfig() {
		logger.info("Creating default configuration file...");
		Properties prop = new Properties();
		try (OutputStream out = new FileOutputStream(path)) {
			prop.setProperty(KEY_VERSION, Server.VERSION);
			prop.setProperty(KEY_PORT, Integer.toString(DEFAULT_PORT));
			prop.setProperty(KEY_MAX_CLIENTS, Integer.toString(DEFAULT_MAX_CLIENTS));
			prop.store(out, "Default server configuration");
			logger.info("Created default configuration file!");
		} catch (FileNotFoundException e) {
			logger.error("Failed to create default configuration file!", e);
		} catch (IOException e) {
			logger.error("Failed to create default configuration file!", e);
		}
	}
	
	public void generateDefaultConfigIfNotCreatedYet() throws ConfigFileIsAFolderException {
		File f = new File(this.path);
		if (!f.exists()) {
			logger.info("No config file found!");
			createDefaultConfig();
		}
		if (f.isDirectory())
			throw new ConfigFileIsAFolderException();
	}
	
	public String getProperty(String key, String defaultValue) {
		logger.trace("Trying to load '" + key + "' value.");
		Properties prop = new Properties();
		try (InputStream in = new FileInputStream(path)) {
			prop.load(in);
			String result = prop.getProperty(key, defaultValue);
			logger.trace("Successfully loaded '" + key + "' value as '" + result + "'.");
			return result;
		} catch (FileNotFoundException e) {
			logger.warn("Unable to load '" + key + "' value from file! Using default (" + defaultValue + ").", e);
			return defaultValue;
		} catch (IOException e) {
			logger.warn("Unable to load '" + key + "' value from file! Using default (" + defaultValue + ").", e);
			return defaultValue;
		}
	}
	
	public void setProperty(String key, String value) {
		logger.debug("Trying to set '" + key + "' value to '" + value + "'.");
		Properties prop = new Properties();
		try (OutputStream out = new FileOutputStream(path)) {
			prop.setProperty(key, value);
			prop.store(out, null);
		} catch (FileNotFoundException e) {
			logger.error("Unable to store '" + value + "' into '" + key + "'!", e);
		} catch (IOException e) {
			logger.error("Unable to store '" + value + "' into '" + key + "'!", e);
		}
	}
}
