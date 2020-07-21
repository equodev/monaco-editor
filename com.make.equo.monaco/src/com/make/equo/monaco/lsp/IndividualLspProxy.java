package com.make.equo.monaco.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4e.server.StreamConnectionProvider;

public class IndividualLspProxy extends LspProxy {

	private StreamConnectionProvider streamConnectionProvider;
	private Thread redirect1 = null;
	private Thread redirect2 = null;

	public IndividualLspProxy(LanguageServerWrapper lspServer) {
		super();
		try {
			Field field = lspServer.getClass().getDeclaredField("lspStreamProvider");
			field.setAccessible(true);
			this.streamConnectionProvider = (StreamConnectionProvider) field.get(lspServer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void connectLspWithProxy() {
		if (streamConnectionProvider == null) {
			return;
		}
		try {
			streamConnectionProvider.stop();
			streamConnectionProvider.start();
			InputStream streamIn = streamConnectionProvider.getInputStream();
			OutputStream streamOut = streamConnectionProvider.getOutputStream();
			Process process = getProcess();
			final InputStream inputProxy = process.getInputStream();
			final OutputStream outputProxy = process.getOutputStream();
			redirect1 = new Thread(() -> {
				try {
					while (true) {
						int read = inputProxy.read();
						streamOut.write(read);
						streamOut.flush();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			});
			redirect1.start();
			redirect2 = new Thread(() -> {
				try {
					while (true) {
						final int read = streamIn.read();
						outputProxy.write(read);
						outputProxy.flush();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			redirect2.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startServer() {
		try {
			startServerWithParams("--individual");
			connectLspWithProxy();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

}