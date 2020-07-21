package com.make.equo.monaco.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4e.LaunchConfigurationStreamProvider;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;

public class IndividualLspProxy extends LspProxy {

	private StreamConnectionProvider streamConnectionProvider;
	private Process process;
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
			streamConnectionProvider.start();
			this.process = getProcessFromStreamConnectionProvider();

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
				} catch (IOException e) {
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
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			redirect2.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Process getProcessFromStreamConnectionProvider() {
		Field field;
		try {
			if (streamConnectionProvider instanceof ProcessStreamConnectionProvider)
				field = ProcessStreamConnectionProvider.class.getDeclaredField("process");
			else if (streamConnectionProvider instanceof LaunchConfigurationStreamProvider) {
				field = LaunchConfigurationStreamProvider.class.getDeclaredField("process");
				field.setAccessible(true);
				IProcess result = (IProcess) field.get(streamConnectionProvider);
				if (!(result instanceof RuntimeProcess))
					return null;
				Method systemProcessGetter = RuntimeProcess.class.getDeclaredMethod("getSystemProcess");
				systemProcessGetter.setAccessible(true);
				return (Process) systemProcessGetter.invoke(result);
			}else
				return null;
			field.setAccessible(true);
			return (Process) field.get(streamConnectionProvider);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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

	@SuppressWarnings("deprecation")
	@Override
	public void stopServer() {
		if (redirect1 != null)
			redirect1.stop();
		if (redirect2 != null) {
			redirect2.stop();
		}
		if (streamConnectionProvider != null) {
			if (this.process == getProcessFromStreamConnectionProvider())
				streamConnectionProvider.stop();
			else if (this.process != null)
				process.destroy();
		}
		super.stopServer();
	}

}