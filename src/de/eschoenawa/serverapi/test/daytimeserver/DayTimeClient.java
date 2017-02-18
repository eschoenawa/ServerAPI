package de.eschoenawa.serverapi.test.daytimeserver;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.eschoenawa.serverapi.Contacter;
import de.eschoenawa.serverapi.Protocol;
import de.eschoenawa.serverapi.client.Client;

public class DayTimeClient {
	
	private static Logger logger = LogManager.getLogger("DayTimeClient");

	public static void main(String[] args) {
		try {
			new Client<String>("DayTime_1_0", "localhost", 5050, new MyProtocol()).start();
		} catch (UnknownHostException e) {
			logger.error("Don't know that host.", e);
		} catch (IOException e) {
			logger.error("IOException.", e);
		}
	}

	private static class MyProtocol implements Protocol<String> {

		private Contacter<String> c;
		private boolean firstContact;

		@Override
		public void connectionReady(Contacter<String> c) {
			this.c = c;
			firstContact = true;
			c.send("date");
		}

		@Override
		public void received(String msg) {
			if (firstContact) {
				logger.info("Recieved date: " + msg);
				firstContact = false;
				c.send("Can you talk?");
			}
			else {
				logger.info("Answer to 'Can you talk?' is: " + msg);
				c.sayBye();
			}
		}

		@Override
		public void connectionTerminated() {
			logger.info("Oh no, the server is no longer there!");
		}
	}
}