package de.eschoenawa.serverapi.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import de.eschoenawa.serverapi.Contacter;
import de.eschoenawa.serverapi.Packet;
import de.eschoenawa.serverapi.Protocol;

public class ClientHandler<T> extends Thread implements Contacter<T> {
	private APIProtocol<T> apiProtocol;
	private Protocol<T> usrProtocol;
	private Server<T> server;
	private Socket socket;
	private static Logger logger = LogManager.getLogger(ClientHandler.class);
	private String prefix;
	private boolean disconnected;
	private PrintWriter out;
	private BufferedReader in;
	private boolean expectClose;

	ClientHandler(Server<T> server, APIProtocol<T> apiProtocol, Protocol<T> usrProtocol, Socket socket) throws IOException {
		this.server = server;
		this.apiProtocol = apiProtocol;
		this.usrProtocol = usrProtocol;
		this.socket = socket;
		this.prefix = this.socket.getInetAddress().toString() + ": ";
		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			logger.error(prefix + "Unrecoverable Exception during initiation of communication! Terminating this channel!", e);
			throw e;
		}
		disconnected = false;
		expectClose = false;
	}
	
	@Override
	public void run() {
		String inputLine;
		Gson gson = new Gson();
		Packet<T> inputPacket;
		Type packetType = new TypeToken<Packet<T>>() {
		}.getType();
		apiProtocol.connectionReady(this);
		try {
			while ((inputLine = in.readLine()) != null) {
				logger.trace(prefix + "Received: " + inputLine);
				inputPacket = gson.fromJson(inputLine, packetType);
				if (inputPacket.getVersion().equals(Server.VERSION)) {
					if (apiProtocol.received(inputPacket.getCmd())) {
						T payload = inputPacket.getPayload();
						if (payload != null)
							usrProtocol.received(payload);
						else
							logger.warn(prefix + "Recieved null-Payload!");
					}
				}
				else {
					logger.warn(prefix + "Server version ('" + Server.VERSION + "') is different from Client Version ('" + inputPacket.getVersion() + "')!");
					apiProtocol.versionMismatch();
				}
			}
		} catch (IOException e) {
			if (!expectClose)
				logger.warn(prefix + "IOException stopped communications!", e);
		} catch (JsonSyntaxException e) {
			logger.error(prefix + "Received malformed JSON String! Terminating this channel!", e);
		}
		disconnect();
	}
	
	@Override
	public void send(T msg) {
		Packet<T> outputPacket = new Packet<>(Server.VERSION, null, msg);
		Gson gson = new Gson();
		try {
			if (!socket.isClosed()) {
				String json = gson.toJson(outputPacket);
				out.println(json);
				logger.trace(prefix + "Sent: " + json);
			}
			else {
				logger.warn(prefix + "Tried to send Packet while the Connection is closed!");
			}
		} catch (Exception e) {
			logger.error(prefix + "Failed to create JSON from message object!", e);
			disconnect();
		}
	}
	
	void apiSend(String s) {
		Packet<T> outputPacket = new Packet<>(Server.VERSION, s, null);
		Gson gson = new Gson();
		try {
			if (!socket.isClosed()) {
				String json = gson.toJson(outputPacket);
				out.println(json);
				logger.trace(prefix + "Sent: " + json);
			}
			else {
				logger.warn(prefix + "Tried to send API-Packet while the Connection is closed!");
			}
		} catch (Exception e) {
			logger.error(prefix + "Failed to create JSON from API-sent Packet!", e);
			disconnect();
		}
	}
	
	void ready() {
		usrProtocol.connectionReady(this);
	}
	
	@Override
	public void disconnect() {
		if (!disconnected) {
			expectClose();
			out.close();
			try {
				in.close();
			} catch (IOException e) {
				logger.error(prefix + "Failed to close BufferedReader (input)!", e);
			}
			try {
				socket.close();
			} catch (IOException e) {
				logger.error(prefix + "Failed to close Socket!", e);
			}
			server.removeClientHandler(this);
			apiProtocol.connectionTerminated();
			usrProtocol.connectionTerminated();
			disconnected = true;
		}
	}
	
	public String getAdress() {
		return socket.getInetAddress().toString();
	}
	
	@Override
	public void sayBye() {
		apiProtocol.sayBye();
	}
	
	void expectClose() {
		expectClose = true;
	}

}
