package de.eschoenawa.serverapi;

public interface ProtocolFactory<T> {
	public Protocol<T> getProtocol();
}
