package com.equo.eclipse.monaco.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4e.LaunchConfigurationStreamProvider;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.equo.monaco.lsp.LspProxy;

public class EclipseLspProxy extends LspProxy {

	private StreamConnectionProvider streamConnectionProvider;
	private Process process;

	protected static final Logger logger = LoggerFactory.getLogger(EclipseLspProxy.class);

	public EclipseLspProxy(LanguageServerWrapper lspServer) {
		try {
			Field field = lspServer.getClass().getDeclaredField("lspStreamProvider");
			field.setAccessible(true);
			this.streamConnectionProvider = (StreamConnectionProvider) field.get(lspServer);
		} catch (Exception e) {
			logger.error("Error obtaining LSP stream provider", e);
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
			logger.error("Error obtaining language server process", e);
		}
		return null;
	}

	@Override
	public void startServer() {
		if (streamConnectionProvider == null) {
			return;
		}
		try {
			synchronized (streamConnectionProvider) {
				streamConnectionProvider.start();
				this.process = getProcessFromStreamConnectionProvider();
	
				InputStream streamIn = streamConnectionProvider.getInputStream();
				OutputStream streamOut = streamConnectionProvider.getOutputStream();
				startProxy(streamIn, streamOut);
			}
		} catch (IOException e) {
			logger.error("Error starting language server", e);
		}
	}

	@Override
	public void stopServer() {
		super.stopServer();
		synchronized (streamConnectionProvider) {
			if (streamConnectionProvider != null) {
				if (this.process == getProcessFromStreamConnectionProvider())
					streamConnectionProvider.stop();
				else if (this.process != null)
					process.destroy();
			}
		}
	}

}