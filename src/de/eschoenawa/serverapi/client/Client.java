package de.eschoenawa.serverapi.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import de.eschoenawa.serverapi.Contacter;
import de.eschoenawa.serverapi.Packet;
import de.eschoenawa.serverapi.Protocol;

public class Client<T> extends Thread implements Contacter<T> {
	
	private static String VERSION = "DEFAULT_SERVERAPI_1_0";
	private String host;
	private int port;
	private ClientAPIProtocol<T> apiProtocol;
	private Protocol<T> usrProtocol;
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private boolean expectClose;
	private boolean disconnected;
	
	private Logger logger = LogManager.getLogger(Client.class);
	
	public Client(String host, int port, Protocol<T> protocol) throws IOException, UnknownHostException {
		this(VERSION, host, port, protocol);
	}

	public Client(String version, String host, int port, Protocol<T> protocol) throws IOException, UnknownHostException {
		this.host = host;
		this.port = port;
		apiProtocol = new ClientAPIProtocol<>();
		this.usrProtocol = protocol;
		VERSION = version;
		disconnected = false;
		expectClose = false;
		try {
			this.socket = new Socket(host, port);
			this.out = new PrintWriter(socket.getOutputStream(), true);
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (UnknownHostException e) {
			logger.fatal("Unknown host!", e);
			throw e;
		} catch (IOException e) {
			logger.fatal("IOException while connecting!", e);
			throw e;
		}
	}
	
	@Override
	public void run() {
		logger.info("Starting client for '" + host + ":" + port + "'...");
		String fromServer;
		Gson gson = new Gson();
		Packet<T> inputPacket;
		Type packetType = new TypeToken<Packet<T>>() {
		}.getType();
		apiProtocol.connectionReady(this);
		try {
			while ((fromServer = in.readLine()) != null) {
				logger.trace("Received: " + fromServer);
				inputPacket = gson.fromJson(fromServer, packetType);
				if (inputPacket.getVersion().equals(Client.VERSION)) {
					if (apiProtocol.received(inputPacket.getCmd())) {
						T payload = inputPacket.getPayload();
						if (payload != null)
							usrProtocol.received(payload);
						else
							logger.warn("Recieved null-Payload!");
					}
				}
				else {
					logger.warn("Server version ('" + inputPacket.getVersion() + "') is different from Client Version ('" + Client.VERSION + "')!");
					apiProtocol.versionMismatch();
				}
			}
		} catch (IOException e) {
			if (!expectClose)
				logger.warn("IOException while reading from Server!", e);
		} catch (JsonSyntaxException e) {
			logger.error("Received malformed JSON String! Terminating this channel!", e);
		}
		disconnect();
	}
	
	@Override
	public void disconnect() {
		if (!disconnected) {
			expectClose();
			out.close();
			try {
				in.close();
			} catch (IOException e) {
				logger.error("Failed to close BufferedReader (input)!", e);
			}
			try {
				socket.close();
			} catch (IOException e) {
				logger.error("Failed to close Socket!", e);
			}
			apiProtocol.connectionTerminated();
			usrProtocol.connectionTerminated();
			disconnected = true;
		}
	}
	
	@Override
	public void sayBye() {
		apiProtocol.sayBye();
	}
	
	@Override
	public void send(T msg) {
		Packet<T> outputPacket = new Packet<>(Client.VERSION, null, msg);
		Gson gson = new Gson();
		try {
			if (!socket.isClosed()) {
				String json = gson.toJson(outputPacket);
				out.println(json);
				logger.trace("Sent: " + json);
			}
			else {
				logger.warn("Tried to send Packet while the Connection is closed!");
			}
		} catch (Exception e) {
			logger.error("Failed to create JSON from message object!", e);
			disconnect();
		}
	}
	
	void apiSend(String s) {
		Packet<T> outputPacket = new Packet<>(Client.VERSION, s, null);
		Gson gson = new Gson();
		try {
			if (!socket.isClosed()) {
				String json = gson.toJson(outputPacket);
				out.println(json);
				logger.trace("Sent: " + json);
			}
			else {
				logger.warn("Tried to send API-Packet while the Connection is closed!");
			}
		} catch (Exception e) {
			logger.error("Failed to create JSON from API-sent Packet!", e);
			disconnect();
		}
	}
	
	void ready() {
		usrProtocol.connectionReady(this);
	}
	
	void expectClose() {
		expectClose = true;
	}
	
}
