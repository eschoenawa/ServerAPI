package de.eschoenawa.serverapi.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class APIProtocol<T> {

	private ClientHandler<T> ch;
	private State s = State.UNREADY;
	private Logger logger = LogManager.getLogger(APIProtocol.class);

	APIProtocol() {
		
	}

	void connectionReady(ClientHandler<T> ch) {
		if (s == State.UNREADY) {
			this.ch = ch;
			ch.apiSend("Hello");
			s = State.HELLO_SENT;
		}
	}

	boolean received(String msg) {
		if (msg == null) {
			if (s == State.HELLO_SENT) {
				logger.warn("Protocol mismatch! Received 'null' while expecting 'Hello back'!");
				ch.disconnect();
				this.s = State.TERMINATED;
				return false;
			}
			else if (s == State.BYE_SENT) {
				logger.warn("Protocol mismatch! Received 'null' while expecting 'Bye back'!");
				ch.disconnect();
				this.s = State.TERMINATED;
				return false;
			}
			else if (s == State.TERMINATED) {
				logger.error("Invalid call! The call 'recieved(null);' cannot be executed since this APIProtocol is terminated!");
				return false;
			}
			return true;
		}
		else if (s == State.HELLO_SENT) {
			if (msg.equals("Hello back")) {
				ch.ready();
				this.s = State.HELLO_RECIEVED;
			}
			else {
				ch.apiSend("Bye");
				logger.warn("Protocol mismatch! Received '" + msg + "' while expecting 'Hello back'!");
				this.s = State.BYE_SENT;
			}
		}
		else if (s == State.HELLO_RECIEVED) {
			if (msg.equals("Bye")) {
				ch.apiSend("Bye back");
				ch.disconnect();
				this.s = State.TERMINATED;
				return false;
			}
			//Further in-comm Commands go here
			return true;
		}
		else if (s == State.BYE_SENT) {
			if (msg.equals("Bye back")) {
				ch.disconnect();
				this.s = State.TERMINATED;
			}
			else {
				logger.warn("Protocol mismatch! Received '" + msg + "' while expecting 'Bye back'!");
				ch.disconnect();
				this.s = State.TERMINATED;
			}
		}
		else if (s == State.TERMINATED) {
			logger.error("Invalid call! The call 'recieved(" + msg + ");' cannot be executed since this APIProtocol is terminated!");
		}
		return false;
	}

	void connectionTerminated() {
		this.s = State.TERMINATED;
	}
	
	void sayBye() {
		if (s == State.HELLO_RECIEVED) {
			ch.apiSend("Bye");
			this.s = State.BYE_SENT;
		}
		else {
			ch.disconnect();
			this.s = State.TERMINATED;
		}
	}
	
	void versionMismatch() {
		ch.apiSend("Version mismatch");
		ch.disconnect();
	}
	
	private enum State {
		UNREADY,HELLO_SENT,HELLO_RECIEVED,BYE_SENT,TERMINATED
	}

}
