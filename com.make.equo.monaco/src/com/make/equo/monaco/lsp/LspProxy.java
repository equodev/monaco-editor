package com.make.equo.monaco.lsp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.FrameworkUtil;

public abstract class LspProxy {
	private static final String SERVER_FILE = "server.js";
	private int proxyPort;
	private ServerSocket socketPortReserve;
	private String proxyFile = null;
	private Process process = null;

	public LspProxy() {
		File bundle;
		try {
			bundle = FileLocator.getBundleFile(FrameworkUtil.getBundle(this.getClass()));
			if (bundle.isDirectory()) {
				proxyFile = new File(bundle, SERVER_FILE).toString();
			} else {
				proxyFile = extractServerFile(bundle.toString());
			}
			proxyPort = reservePortForProxy(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public abstract void startServer();

	protected void startServerWithParams(String... params) throws IOException {
		try {
			closeSocketPortReserve();
			List<String> args = new ArrayList<>();
			args.addAll(Arrays.asList("node", getProxyFile(), "--port", Integer.valueOf(getPort()).toString()));
			args.addAll(Arrays.asList(params));
			ProcessBuilder processBuilder = new ProcessBuilder(args);
			setProcess(processBuilder.start());
		} catch (IOException e) {
			setProcess(null);
			throw e;
		}
	}

	public void stopServer() {
		if (getProcess() != null) {
			getProcess().destroy();
			try {
				getProcess().waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			setProcess(null);
		}
		reservePortForProxy(getPort());
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
	 * Extracts the ws proxy file along with the node modules from the jar into a
	 * temp directory
	 * 
	 * @param jarFile The jar file from which to extract the ws proxy
	 * 
	 * @return the String for the path of the ws proxy file
	 */
	public static String extractServerFile(String jarFile) throws IOException {
		Path tempDir = Files.createTempDirectory("lspserver");
		JarFile jar = new JarFile(jarFile);
		Enumeration<JarEntry> enumEntries = jar.entries();
		while (enumEntries.hasMoreElements()) {
			JarEntry file = (JarEntry) enumEntries.nextElement();
			String name = file.getName();
			if (!name.contains("node_modules") && !name.equals(SERVER_FILE)) { // Only unpack the needed files
				continue;
			}
			File f = new java.io.File(tempDir + java.io.File.separator + name);
			if (file.isDirectory()) { // if it's a directory, create it
				f.mkdir();
				continue;
			}
			f.getParentFile().mkdirs();
			f.createNewFile();
			InputStream in = new BufferedInputStream(jar.getInputStream(file));
			@SuppressWarnings("resource")
			OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
			byte[] buffer = new byte[2048];
			for (;;) {
				int nBytes = in.read(buffer);
				if (nBytes <= 0) {
					break;
				}
				out.write(buffer, 0, nBytes);
			}
			out.flush();
			out.close();
			in.close();
		}
		jar.close();
		markForDelete(tempDir.toFile());
		return new File(tempDir.toString(), SERVER_FILE).toString();
	}

	private static void markForDelete(File directoryToBeDeleted) {
		directoryToBeDeleted.deleteOnExit();
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				markForDelete(file);
			}
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

	public Process getProcess() {
		return process;
	}

	protected void setProcess(Process process) {
		this.process = process;
	}

	protected String getProxyFile() {
		return proxyFile;
	}
}
