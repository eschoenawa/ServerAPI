package de.eschoenawa.serverapi;

public interface Contacter<T> {
	public void send(T msg);
	public void disconnect();
	public void sayBye();
}
