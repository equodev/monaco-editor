package com.make.equo.monaco;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.chromium.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.google.gson.JsonObject;
import com.make.equo.filesystem.api.IEquoFileSystem;
import com.make.equo.monaco.lsp.CommonLspProxy;
import com.make.equo.monaco.lsp.LspProxy;
import com.make.equo.ws.api.IEquoEventHandler;
import com.make.equo.ws.api.IEquoRunnable;
import com.make.equo.ws.api.IEquoWebSocketService;

public class EquoMonacoEditor {
	protected IEquoFileSystem equoFileSystem;

	private LspProxy lspProxy = null;
	private static Map<String, List<String>> lspServers = new HashMap<>();
	private static Map<String, String> lspWsServers = new HashMap<>();

	private volatile boolean loaded;

	private final Semaphore lock = new Semaphore(1);

	private Browser browser;
	private String namespace;
	private List<IEquoRunnable<Void>> onLoadListeners;
	protected String filePath = "";
	private boolean dispose = false;
	private String fileName = "";
	private WatchService watchService;
	private String rootPath = null;

	protected IEquoEventHandler equoEventHandler;

	private String initialContent;

	public String getFilePath() {
		return filePath;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
		this.fileName = new File(this.filePath).getName();
		listenChangesPath();
		notifyFilePathChanged();
	}

	public EquoMonacoEditor(Composite parent, int style, IEquoEventHandler handler,
			IEquoWebSocketService websocketService, IEquoFileSystem equoFileSystem, String editorUrl) {
		this(handler, equoFileSystem);
		browser = new Browser(parent, style);
		String wsPort = String.format("&equowsport=%s", String.valueOf(websocketService.getPort()));
		browser.setUrl("http://" + editorUrl + "?namespace=" + namespace + wsPort);
	}

	public EquoMonacoEditor(IEquoEventHandler handler, IEquoFileSystem equoFileSystem) {
		this.equoEventHandler = handler;
		this.equoFileSystem = equoFileSystem;
		namespace = "editor" + Double.toHexString(Math.random());
		onLoadListeners = new ArrayList<IEquoRunnable<Void>>();
		loaded = false;
		registerActions();
	}

	private void registerActions() {
		equoEventHandler.on(namespace + "_disposeEditor", (IEquoRunnable<Void>) runnable -> dispose());
		equoEventHandler.on(namespace + "_doSaveAs", (IEquoRunnable<Void>) runnable -> saveAs());
		equoEventHandler.on(namespace + "_doSave", (IEquoRunnable<Void>) runnable -> save());
		equoEventHandler.on(namespace + "_doReload", (IEquoRunnable<Void>) runnable -> reload());
	}

	public void configRename(IEquoRunnable<Void> runnable) {
		equoEventHandler.on(namespace + "_makeRename", runnable);
	}

	public void sendModel(String uri, String content) {
		if (content == null) {
			File file = new File(uri);
			if (!file.exists() || !file.isFile())
				return;
			content = equoFileSystem.readFile(file);
			if (content == null)
				return;
		}
		equoEventHandler.send(namespace + "_modelResolved" + uri, content);
	}

	public void configGetModel(IEquoRunnable<String> runnable) {
		equoEventHandler.on(namespace + "_getContentOf", (JsonObject changes) -> {
			runnable.run(changes.get("path").getAsString());
		});
	}

	public void reInitialize(String content, String filePath, String rootPath, LspProxy lsp) {
		if (filePath.startsWith("file:")) {
			setFilePath(filePath.substring(5));
		} else {
			setFilePath(filePath);
		}
		String lspPath = null;
		if (lsp != null) {
			this.lspProxy = lsp;
			lspPath = "ws://127.0.0.1:" + lsp.getPort();
			this.lspProxy.startServer();
		}
		setRootPath(rootPath);

		Map<String, String> editorData = new HashMap<String, String>();
		editorData.put("text", content);
		editorData.put("name", this.filePath);
		editorData.put("lspPath", lspPath);
		if (this.rootPath != null) {
			editorData.put("rootUri", "file://" + this.rootPath);
		}
		equoEventHandler.send(this.namespace + "_doReinitialization", editorData);
	}

	public void initialize(String contents, String fileName, String filePath) {
		this.filePath = filePath;
		this.fileName = fileName;
		handleCreateEditor(contents, null, false);
	}

	protected void createEditor(String content, String filePath, LspProxy lsp) {
		this.initialContent = content;
		if (filePath.startsWith("file:")) {
			setFilePath(filePath.substring(5));
		} else {
			setFilePath(filePath);
		}
		String lspPathAux = null;
		if (this.lspProxy != null)
			this.lspProxy.stopServer();
		final boolean thereIsLS = (lsp != null);
		if (thereIsLS) {
			this.lspProxy = lsp;
			lspPathAux = "ws://127.0.0.1:" + lsp.getPort();
		}
		final String lspPath = lspPathAux;
		equoEventHandler.on("_createEditor", (JsonObject payload) -> handleCreateEditor(content, lspPath, thereIsLS));
	}

	protected String getLspServerForFile(String fileName) {
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
			extension = fileName.substring(i + 1);
		}
		List<String> lspProgram = lspServers.getOrDefault(extension, null);
		if (lspProgram != null) {
			this.lspProxy = new CommonLspProxy(lspProgram);
			return "ws://127.0.0.1:" + this.lspProxy.getPort();
		}
		return lspWsServers.getOrDefault(extension, null);
	}

	protected void handleCreateEditor(String contents, String fixedLspPath, boolean bindEclipseLsp) {
		String lspPath = (fixedLspPath != null) ? fixedLspPath : getLspServerForFile(this.fileName);
		if (lspPath != null && this.lspProxy != null) {
			try {
				new Thread(() -> lspProxy.startServer()).start();
			} catch (Exception e) {
			}
		}
		Map<String, Object> editorData = new HashMap<>();
		editorData.put("text", contents);
		editorData.put("name", this.filePath);
		editorData.put("namespace", namespace);
		editorData.put("lspPath", lspPath);
		editorData.put("bindEclipseLsp", bindEclipseLsp);
		if (this.rootPath != null) {
			editorData.put("rootUri", "file://" + this.rootPath);
		}
		equoEventHandler.send("_doCreateEditor", editorData);
		loaded = true;
		for (IEquoRunnable<Void> onLoadListener : onLoadListeners) {
			onLoadListener.run(null);
		}
		onLoadListeners.clear();

		equoEventHandler.on(namespace + "_canPaste", (IEquoRunnable<Void>) runnable -> {
			try {
				Robot robot = new Robot();
				// Simulate a key press
				robot.keyPress(KeyEvent.VK_CONTROL);
				robot.keyPress(KeyEvent.VK_V);
				robot.keyRelease(KeyEvent.VK_V);
				robot.keyRelease(KeyEvent.VK_CONTROL);
			} catch (AWTException e) {
				e.printStackTrace();
			}
		});

		equoEventHandler.on(namespace + "_canSelectAll", (IEquoRunnable<Void>) runnable -> {
			try {
				Robot robot = new Robot();
				// Simulate a key press
				robot.keyPress(KeyEvent.VK_CONTROL);
				robot.keyPress(KeyEvent.VK_A);
				robot.keyRelease(KeyEvent.VK_A);
				robot.keyRelease(KeyEvent.VK_CONTROL);
			} catch (AWTException e) {
				e.printStackTrace();
			}
		});
		if (!filePath.equals("")) {
			listenChangesPath();
		}

	}

	public void addOnLoadListener(IEquoRunnable<Void> listener) {
		if (!loaded) {
			onLoadListeners.add(listener);
		} else {
			listener.run(null);
		}
	}

	public String getContentsSync() {
		if (!loaded) {
			return this.initialContent;
		}
		String[] result = { null };
		if (lock.tryAcquire()) {
			equoEventHandler.on(namespace + "_doGetContents", (JsonObject contents) -> {
				try {
					result[0] = contents.get("contents").getAsString();
				} finally {
					synchronized (lock) {
						lock.notify();
						lock.release();
					}
				}
			});

			equoEventHandler.send(namespace + "_getContents");
			synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return result[0];
	}

	public void getContentsAsync(IEquoRunnable<String> runnable) {
		equoEventHandler.on(namespace + "_doGetContents", (JsonObject contents) -> {
			runnable.run(contents.get("contents").getAsString());
		});
		equoEventHandler.send(namespace + "_getContents");
	}

	public void handleAfterSave() {
		equoEventHandler.send(namespace + "_didSave");
	}

	public void notifyFilePathChanged() {
		Map<String, String> payload = new HashMap<>();
		payload.put("filePath", filePath);
		payload.put("fileName", new File(filePath).getName());
		equoEventHandler.send(namespace + "_filePathChanged", payload);
	}

	public void undo() {
		equoEventHandler.send(namespace + "_undo");
	}

	public void redo() {
		equoEventHandler.send(namespace + "_redo");
	}

	public void copy() {
		equoEventHandler.send(namespace + "_doCopy");
	}

	public void cut() {
		equoEventHandler.send(namespace + "_doCut");
	}

	public void find() {
		equoEventHandler.send(namespace + "_doFind");
	}

	public void paste() {
		if (browser != null)
			browser.setFocus();
		equoEventHandler.send(namespace + "_doPaste");
	}

	public void selectAll() {
		if (browser != null)
			browser.setFocus();
		equoEventHandler.send(namespace + "_doSelectAll");
	}

	public void saveAs() {
		if (equoFileSystem != null) {
			getContentsAsync(content -> {
				Display.getDefault().asyncExec(() -> {
					File file = equoFileSystem.saveFileAs(content);
					if (file != null) {
						filePath = file.getAbsolutePath();
						notifyFilePathChanged();
						handleAfterSave();
						listenChangesPath();
					}
				});
			});
		}
	}

	public void save() {
		if (filePath == null || filePath.trim().equals("")) {
			saveAs();
		} else if (equoFileSystem != null) {
			getContentsAsync(content -> {
				Display.getDefault().asyncExec(() -> {
					if (equoFileSystem.saveFile(new File(filePath), content)) {
						handleAfterSave();
					}
				});
			});
		}
	}

	public boolean registerFileToListen() {
		fileName = Paths.get(filePath).getFileName().toString();
		Path parent = Paths.get(filePath).getParent();
		if (parent == null) {
			return false;
		}
		Path path = Paths.get(parent.toString());
		try {
			watchService = FileSystems.getDefault().newWatchService();
			path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
					StandardWatchEventKinds.ENTRY_DELETE);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void listenChangesPath() {
		if (filePath == null || filePath.equals("")) {
			return;
		}

		if (watchService != null) {
			try {
				watchService.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (!registerFileToListen()) {
			return;
		}

		new Thread() {
			public void run() {
				boolean poll = true;

				while (poll && !dispose) {
					WatchKey key = null;
					try {
						key = watchService.take();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ClosedWatchServiceException e) {
						break;
					}
					for (WatchEvent<?> event : key.pollEvents()) {
						if (event.context().toString().trim().equals(fileName)) {
							reportChanges();
						}
					}
					poll = key.reset();
				}
			}
		}.start();
	}

	public void reportChanges() {
		getContentsAsync(content -> {
			Display.getDefault().asyncExec(() -> {
				String fileContent = getFileContent();
				if (fileContent == null || !content.trim().equals(fileContent.trim())) {
					equoEventHandler.send(namespace + "_reportChanges");
				}
			});
		});
	}

	private String getFileContent() {
		if (filePath != null && !filePath.equals("") && equoFileSystem != null) {
			return equoFileSystem.readFile(new File(filePath));
		}
		return null;
	}

	public void reload() {
		String content = getFileContent();
		if (content != null) {
			equoEventHandler.send(namespace + "_doReload", content);
		}
	}

	public void selectAndReveal(int offset, int length) {
		Map<String, Integer> data = new HashMap<>();
		data.put("offset", offset);
		data.put("length", length);
		if (loaded) {
			equoEventHandler.send(namespace + "_selectAndReveal", data);
		} else {
			addOnLoadListener((IEquoRunnable<Void>) runnable -> {
				equoEventHandler.send(namespace + "_selectAndReveal", data);
			});
		}
	}

	public void configSelection(IEquoRunnable<TextSelection> selectionFunction) {
		equoEventHandler.on(namespace + "_selection", (JsonObject contents) -> {
			TextSelection textSelection = new TextSelection(contents.get("offset").getAsInt(),
					contents.get("length").getAsInt());
			selectionFunction.run(textSelection);
		});
	}

	public void configFindAllReferences(IEquoRunnable<Void> handler) {
		equoEventHandler.on(namespace + "_findAllReferences", handler);
	}

	public void subscribeChanges(IEquoRunnable<Boolean> dirtyListener, IEquoRunnable<Boolean> undoListener,
			IEquoRunnable<Boolean> redoListener, IEquoRunnable<String> contentChangeListener) {
		equoEventHandler.on(namespace + "_changesNotification", (JsonObject changes) -> {
			dirtyListener.run(changes.get("isDirty").getAsBoolean());
			undoListener.run(changes.get("canUndo").getAsBoolean());
			redoListener.run(changes.get("canRedo").getAsBoolean());
			contentChangeListener.run(changes.get("content").getAsString());
		});

		if (loaded) {
			equoEventHandler.send(namespace + "_subscribeModelChanges");
		} else {
			addOnLoadListener((IEquoRunnable<Void>) runnable -> {
				equoEventHandler.send(namespace + "_subscribeModelChanges");
			});
		}
	}

	/**
	 * Add a lsp websocket server to be used by the editors on the files with the
	 * given extensions
	 * 
	 * @param fullServerPath The full path to the lsp server. Example:
	 *                       ws://127.0.0.1:3000/lspServer
	 * @param extensions     A collection of extensions for what the editor will use
	 *                       the given lsp server. The extensions must not have the
	 *                       initial dot. Example: ["php", "php4"]
	 */
	public static void addLspWsServer(String fullServerPath, Collection<String> extensions) {
		for (String extension : extensions) {
			lspWsServers.put(extension, fullServerPath);
		}
	}

	/**
	 * Add a lsp server to be used by the editors on the files with the given
	 * extensions
	 * 
	 * @param executionParameters The parameters needed to start the lsp server
	 *                            through stdio. Example: ["html-languageserver",
	 *                            "--stdio"]
	 * @param extensions          A collection of extensions for what the editor
	 *                            will use the given lsp server. The extensions must
	 *                            not have the initial dot. Example: ["php", "php4"]
	 */
	public static void addLspServer(List<String> executionParameters, Collection<String> extensions) {
		for (String extension : extensions) {
			lspServers.put(extension, executionParameters);
		}
	}

	/**
	 * Remove a lsp server assigned to the given extensions
	 * 
	 * @param extensions A collection of the file extensions for which the
	 *                   previously assigned lsp will be removed The extensions must
	 *                   not have the initial dot. Example: ["php", "php4"]
	 */
	public static void removeLspServer(Collection<String> extensions) {
		for (String extension : extensions) {
			lspServers.remove(extension);
			lspWsServers.remove(extension);
		}
	}

	public void dispose() {
		if (lspProxy != null)
			lspProxy.stopServer();
		dispose = true;
	}

	/**
	 * Set new content into the editor
	 * 
	 * @param content The new content to be setted
	 * @param asEdit  If true, the content will be setted as an edition and an Undo
	 *                operation will be available. If false, the content will be
	 *                setted without Undo stack, as if it was the first content
	 *                loaded
	 */
	public void setContent(String content, boolean asEdit) {
		Map<String, Object> response = new HashMap<>();
		response.put("content", content);
		response.put("asEdit", asEdit);
		if (loaded) {
			equoEventHandler.send(namespace + "_setContent", response);
		} else {
			addOnLoadListener((IEquoRunnable<Void>) runnable -> {
				equoEventHandler.send(namespace + "_setContent", response);
			});
		}
	}

}
