package de.eschoenawa.serverapi.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;




class ClientAPIProtocol<T> {
	
	private Client<T> c;
	private State s;
	private Logger logger = LogManager.getLogger(ClientAPIProtocol.class);

	ClientAPIProtocol() {
		
	}
	
	void connectionReady(Client<T> c) {
		this.c = c;
		this.s = State.READY;
	}

	boolean received(String msg) {
		if (msg == null) {
			if (s == State.READY) {
				logger.warn("Protocol mismatch! Received 'null' while expecting 'Hello'");
				c.disconnect();
				return false;
			}
			else if (s == State.BYE_SENT) {
				logger.warn("Protocol mismatch! Received 'null' while expecting 'Bye back'");
				c.disconnect();
				return false;
			}
			else if (s == State.TERMINATED) {
				logger.error("Invalid call! The call 'recieved(null);' cannot be executed since this ClientAPIProtocol is terminated!");
				return false;
			}
			return true;
		}
		else if (s == State.READY) {
			if (msg.equals("Hello")) {
				c.apiSend("Hello back");
				this.s = State.HELLO_BACK_SENT;
				c.ready();
			}
			else {
				logger.warn("Protocol mismatch! Received '" + msg + "' while expecting 'Hello'");
				c.disconnect();
			}
		}
		else if (s == State.HELLO_BACK_SENT) {
			if (msg.equals("Bye")) {
				c.apiSend("Bye back");
				c.disconnect();
				return false;
			}
			//Further in-comm commands go here
			return true;
		}
		else if (s == State.BYE_SENT) {
			if (msg.equals("Bye back")) {
				c.disconnect();
				this.s = State.TERMINATED;
			}
			else {
				logger.warn("Protocol mismatch! Received '" + msg + "' while expecting 'Bye back'!");
				c.disconnect();
				this.s = State.TERMINATED;
			}
		}
		else if (s == State.TERMINATED) {
			logger.error("Invalid call! The call 'recieved(" + msg + ");' cannot be executed since this ClientAPIProtocol is terminated!");
		}
		return false;
	}

	void connectionTerminated() {
		this.s = State.TERMINATED;
	}
	
	void sayBye() {
		if (s == State.HELLO_BACK_SENT) {
			c.apiSend("Bye");
			this.s = State.BYE_SENT;
		}
		else {
			c.disconnect();
			this.s = State.TERMINATED;
		}
	}
	
	void versionMismatch() {
		c.apiSend("Version mismatch");
		c.disconnect();
	}

	private enum State {
		UNREADY,READY,HELLO_BACK_SENT,BYE_SENT,TERMINATED
	}
}
