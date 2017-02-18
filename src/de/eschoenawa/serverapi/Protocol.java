package de.eschoenawa.serverapi;

public interface Protocol<T> {
	public void connectionReady(Contacter<T> c);
	public void received(T msg);
	public void connectionTerminated();
}
