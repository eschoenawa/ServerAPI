package de.eschoenawa.serverapi.test.daytimeserver;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.eschoenawa.serverapi.Contacter;
import de.eschoenawa.serverapi.Protocol;
import de.eschoenawa.serverapi.ProtocolFactory;
import de.eschoenawa.serverapi.server.Server;

public class DayTimeServer {

	private static Logger logger = LogManager.getLogger("DayTimeServer");

	public static void main(String[] args) {
		Server<String> s = new Server<>("DayTime_1_0", 5050, 10, new MyProtocolFactory());
		s.start();
	}

	private static class MyProtocolFactory implements ProtocolFactory<String> {

		@Override
		public Protocol<String> getProtocol() {
			return new MyProtocol();
		}

	}

	private static class MyProtocol implements Protocol<String> {

		private Contacter<String> c;

		@Override
		public void connectionReady(Contacter<String> c) {
			this.c = c;
		}

		@Override
		public void received(String msg) {
			if (msg.equals("date")) {
				c.send(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
			}
			else {
				c.send("I only know date! No idea what you are talking about!");
			}
		}

		@Override
		public void connectionTerminated() {
			logger.info("Oh no, the client is no longer there!");
		}

	}

}