package com.make.equo.monaco.lsp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CommonLspProxy extends LspProxy {
	private String serversFile = null;
	private boolean serverOn = false;
	private int instancesUsingServer = 0;
	private Map<String, List<String>> servers = new HashMap<>();

	public CommonLspProxy() {
		super();
		try {
			File fileForServer = File.createTempFile("serversLsp", ".yml");
			fileForServer.deleteOnExit();
			serversFile = fileForServer.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String formatServerName(String name) {
		return name.replace(" ", "");
	}

	public CommonLspProxy addServer(String name, List<String> excecutionParameters) {
		servers.put(formatServerName(name), excecutionParameters);
		saveServersInFile();
		return this;
	}

	public CommonLspProxy removeServer(Collection<String> names) {
		for (String name : names) {
			servers.remove(name);
		}
		saveServersInFile();
		return this;
	}

	
	@Override
	public synchronized void startServer() {
		if (!serverOn) {
			try {
				startServerWithParams("--languageServers", serversFile);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			serverOn = true;
		}
		instancesUsingServer++;
	}

	private void saveServersInFile() {
		StringBuilder content = new StringBuilder();
		content.append("langservers:\n");
		for (Entry<String, List<String>> server : servers.entrySet()) {
			content.append("    ");
			content.append(server.getKey());
			content.append(":\n");
			for (String parameter : server.getValue()) {
				content.append("        - ");
				content.append(parameter);
				content.append("\n");
			}
		}

		try {
			PrintWriter writer = new PrintWriter(serversFile, "UTF-8");
			writer.print(content);
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void stopServer() {
		if (serverOn && --instancesUsingServer == 0) {
			super.stopServer();
			serverOn = false;
		}
	}

	public boolean isRunning() {
		return serverOn;
	}
}
