package de.eschoenawa.serverapi;

public class Packet<T> {

	private String cmd;
	private String version;
	private T payload;

	public Packet() {
		cmd = null;
		payload = null;
	}

	public Packet(String version, String cmd, T payload) {
		super();
		this.cmd = cmd;
		this.version = version;
		this.payload = payload;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public T getPayload() {
		return payload;
	}

	public void setPayload(T payload) {
		this.payload = payload;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
