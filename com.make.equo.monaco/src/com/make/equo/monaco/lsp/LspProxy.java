package com.make.equo.monaco.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;

public abstract class LspProxy {
	private int proxyPort;
	private ServerSocket socketPortReserve;
	protected LspWsProxy proxy;
	private String rootPath = null;

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public LspProxy() {
		proxyPort = reservePortForProxy(0);
	}

	public abstract void startServer();

	public void stopServer() {
		if (proxy != null) {
			try {
				proxy.stop();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	protected void startProxy(InputStream streamIn, OutputStream streamOut) {
		closeSocketPortReserve();
		proxy = new LspWsProxy(getPort(), streamIn, streamOut, rootPath);
		proxy.start();
	}

	public int getPort() {
		return proxyPort;
	}

	protected void closeSocketPortReserve() {
		try {
			socketPortReserve.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reserves a port to be used later by the ws proxy. It mantains a socket listen
	 * on that port until the proxy is started
	 * 
	 * @param port The port to be reserved. Use 0 to reserve a random port
	 * 
	 * @return the port reserved
	 */
	protected int reservePortForProxy(int port) {
		try {
			socketPortReserve = new ServerSocket(port);
			socketPortReserve.setReuseAddress(true);
			return socketPortReserve.getLocalPort();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return (int) (Math.random() * 10000 + 40000);
	}

}
