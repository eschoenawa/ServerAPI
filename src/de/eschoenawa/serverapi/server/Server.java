package de.eschoenawa.serverapi.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.eschoenawa.serverapi.ProtocolFactory;

public class Server<T> extends Thread {
	
	static String CFG_PATH = "./server.properties";
	static String VERSION = "DEFAULT_SERVERAPI_1_0";
	private static Logger logger = LogManager.getLogger(Server.class);
	private static Config c = Config.getInstance();
	private int port;
	private int maxClients;
	private boolean acceptMore;
	private ProtocolFactory<T> protocolFactory;
	private ArrayList<ClientHandler<T>> clientHandlers;
	
	public static void main(String[] args) {
		new Server<String>(null).start();
	}
	
	public Server(ProtocolFactory<T> protocolFactory) {
		this(CFG_PATH, protocolFactory);
	}

	public Server(String configPath, ProtocolFactory<T> protocolFactory) {
		logger.info("NEW SERVER INITIALIZATION");
		logger.info("Config-file is '" + configPath + "'");
		CFG_PATH = configPath;
		try {
			c.generateDefaultConfigIfNotCreatedYet();
		} catch (ConfigFileIsAFolderException e) {
			logger.error("Unable to generate Config file since there is a folder with the file name ('" + CFG_PATH + "').");
		}
		VERSION = c.getProperty(Config.KEY_VERSION, VERSION);
		this.port = Integer.parseInt(c.getProperty(Config.KEY_PORT, Integer.toString(Config.DEFAULT_PORT)));
		this.maxClients = Integer.parseInt(c.getProperty(Config.KEY_MAX_CLIENTS, Integer.toString(Config.DEFAULT_MAX_CLIENTS)));
		acceptMore = true;
		this.protocolFactory = protocolFactory;
		this.clientHandlers = new ArrayList<>();
	}
	
	public Server(String version, int port, int maxClients, ProtocolFactory<T> protocolFactory) {
		this(CFG_PATH, version, port, maxClients, protocolFactory);
	}
	
	public Server(String configPath, String version, int port, int maxClients, ProtocolFactory<T> protocolFactory) {
		logger.info("NEW SERVER INITIALIZATION");
		logger.info("Config-file is '" + configPath + "'");
		CFG_PATH = configPath;
		try {
			c.generateDefaultConfigIfNotCreatedYet();
		} catch (ConfigFileIsAFolderException e) {
			logger.error("Unable to generate Config file since there is a folder with the file name ('" + CFG_PATH + "').");
		}
		VERSION = version;
		this.port = port;
		this.maxClients = maxClients;
		c.setProperty(Config.KEY_VERSION, VERSION);
		c.setProperty(Config.KEY_PORT, Integer.toString(this.port));
		c.setProperty(Config.KEY_MAX_CLIENTS, Integer.toString(this.maxClients));
		acceptMore = true;
		this.protocolFactory = protocolFactory;
		this.clientHandlers = new ArrayList<>();
	}
	
	public static String getConfigPath() {
		return CFG_PATH;
	}
	
	public static void setConfigPath(String path) {
		CFG_PATH = path;
	}
	
	@Override
	public void run() {
		logger.info("Starting Server on port " + this.port + "...");
		if (this.protocolFactory == null) {
			logger.fatal("ProtocolFactory is 'null'! You need to specify one to tell the Server what it should do!");
			return;
		}
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(this.port, this.maxClients);
		} catch (IOException e) {
			logger.fatal("Failed to bind new ServerSocket! Are you sure there isn't another program running on port '" + this.port + "'?", e);
		}
		if (serverSocket != null) {
			while (acceptMore) {
				try {
					Socket s = serverSocket.accept();
					if (clientHandlers.size() <= maxClients) {
						ClientHandler<T> ch = new ClientHandler<>(this, new APIProtocol<T>(), this.protocolFactory.getProtocol(), s);
						clientHandlers.add(ch);
						logger.debug("'" + ch.getAdress() + "' connected.");
						ch.start();
					}
					else {
						logger.warn("Rejecting '" + s.getInetAddress().toString() + "' due to maximum Client capacity reached.");
						s.close();
					}
				} catch (IOException e) {
					logger.fatal("IOException while trying to listen!", e);
					return;
				}
			}
		}
		else {
			logger.fatal("No ServerSocket available! Terminating Server!");
		}
	}
	
	void removeClientHandler(ClientHandler<T> ch) {
		if (clientHandlers.remove(ch))
			logger.debug("'" + ch.getAdress() + "' disconnected.");
	}
	
	public void stopServer(boolean immediatly) {
		if (immediatly) {
			for (ClientHandler<T> ch : clientHandlers) {
				ch.disconnect();
			}
		}
		else {
			for (ClientHandler<T> ch : clientHandlers) {
				ch.sayBye();
			}
		}
	}
}
