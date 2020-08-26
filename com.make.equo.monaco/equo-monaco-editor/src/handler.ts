import { listen, MessageConnection } from 'vscode-ws-jsonrpc';
import {
	MonacoLanguageClient, CloseAction, ErrorAction,
	MonacoServices, createConnection
} from 'monaco-languageclient';
const normalizeUrl = require('normalize-url');
const ReconnectingWebSocket = require('reconnecting-websocket');
import * as monaco from 'monaco-editor';
import { EquoWebSocketService, EquoWebSocket } from '@equo/websocket'
// @ts-ignore
import { StandaloneCodeEditorServiceImpl } from 'monaco-editor/esm/vs/editor/standalone/browser/standaloneCodeServiceImpl.js'
// @ts-ignore
import { RenameAction } from 'monaco-editor/esm/vs/editor/contrib/rename/rename.js'

export class EquoMonacoEditor {

	private lastSavedVersionId!: number;
	private editor!: monaco.editor.IStandaloneCodeEditor;
	private model!: monaco.editor.ITextModel;
	private namespace!: string;
	private wasCreated: boolean = false;
	private webSocket: EquoWebSocket;
	private languageClient!: MonacoLanguageClient;
	private lspws!: WebSocket;
	private filePath!: string;
	private fileName!: string;
	private filePathChangedCallback!: Function;
	private notifyChangeCallback!: Function;
	private elemdiv: HTMLElement;
	private sendChangesToJavaSide: boolean = false;
	private shortcutsAdded: boolean = false;

	constructor(websocket: EquoWebSocket) {
		this.webSocket = websocket;
		this.elemdiv = document.createElement('div');
		this.elemdiv.addEventListener("click", (e: Event) => this.reload());
		this.elemdiv.style.background = "#DD944F";
		this.elemdiv.style.textAlign = "center";
		this.filePathChangedCallback = this.actionForFileChange;
		this.notifyChangeCallback = () => { };
	}


	public getEditor(): monaco.editor.IStandaloneCodeEditor {
		return this.editor;
	}

	public getFilePath(): string {
		return this.filePath;
	}

	public getFileName(): string {
		return this.fileName;
	}

	public dispose(): void {
		if (this.lspws) {
			//@ts-ignore
			this.lspws.close(1000, '', { keepClosed: true, fastClose: true, delay: 0 });
		}
		if (this.languageClient)
			this.languageClient.stop();
		this.model.dispose();
		this.editor.dispose();
		this.webSocket.send(this.namespace + "_disposeEditor");
	}

	public saveAs(): void {
		this.webSocket.send(this.namespace + "_doSaveAs");
	}

	public save(): void {
		this.webSocket.send(this.namespace + "_doSave");
	}

	public reload(): void {
		this.webSocket.send(this.namespace + "_doReload");
	}

	public setFilePathChangedListener(callback: Function) {
		this.filePathChangedCallback = callback;
	}

	public setActionDirtyState(callback: Function) {
		this.notifyChangeCallback = callback;
	}

	private createModelAndGetLanguage(file: string, content: string): string{
		let l = this.getLanguageOfFile(file);
		let language = '';

		if (l) {
			monaco.languages.register(l);
			language = l.id;
		} else {
			language = 'userdefinedlanguage'
			monaco.languages.register({
				id: language
			});
		}
		this.model = monaco.editor.createModel(
			content,
			language,
			monaco.Uri.file(file) // uri
		);
		return language;
	}

	private connectLsp(lspPath: string | undefined, rootUri: string | undefined, language: string): void{
		if (lspPath) {
			MonacoServices.install(this.editor, {rootUri: rootUri});

			// create the web socket
			var url = normalizeUrl(lspPath)
			this.lspws = createWebSocket(url);
			var webSocket = this.lspws;
			// listen when the web socket is opened
			listen({
				webSocket,
				onConnection: connection => {
					// create and start the language client
					this.languageClient = createLanguageClient(connection);
					var disposable = this.languageClient.start();
					connection.onClose(() => disposable.dispose());
				}
			});
		}

		function createLanguageClient(connection: MessageConnection): MonacoLanguageClient {
			return new MonacoLanguageClient({
				name: "Sample Language Client",
				clientOptions: {
					// use a language id as a document selector
					documentSelector: [language],
					// disable the default error handler
					errorHandler: {
						error: () => ErrorAction.Continue,
						closed: () => CloseAction.DoNotRestart
					}
				},
				// create a language client connection from the JSON RPC connection on demand
				connectionProvider: {
					get: (errorHandler, closeHandler) => {
						return Promise.resolve(createConnection(connection, errorHandler, closeHandler));
					}
				}
			});
		}

		function createWebSocket(url: string): WebSocket {
			const socketOptions = {
				maxReconnectionDelay: 10000,
				minReconnectionDelay: 1000,
				reconnectionDelayGrowFactor: 1.3,
				connectionTimeout: 10000,
				maxRetries: Infinity,
				debug: false
			};
			return new ReconnectingWebSocket(url, [], socketOptions);
		}
	}

	public create(element: HTMLElement, filePath?: string): void {
		this.webSocket.on("_doCreateEditor", (values: { text: string; name: string; namespace: string; lspPath?: string; rootUri?: string }) => {
			if (!this.wasCreated) {
				this.namespace = values.namespace;

				element.appendChild(this.elemdiv);

				let language = this.createModelAndGetLanguage(values.name, values.text);

				let ws = this.webSocket;
				let self = this;

				let getModel = function(resource: monaco.Uri){
					var model = null;
					if(resource !== null)
						model = monaco.editor.getModel(resource);
					if(model == null) {
						model = monaco.editor.createModel(
							"package com.make.equo.application.model;\n\nimport java.io.IOException;\nimport java.net.URISyntaxException;\n\nimport org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;\nimport org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;\nimport org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;\nimport org.eclipse.e4.ui.model.application.ui.menu.MHandledToolItem;\nimport org.eclipse.e4.ui.model.application.ui.menu.MMenu;\nimport org.eclipse.e4.ui.model.application.ui.menu.MToolBar;\nimport org.eclipse.e4.ui.model.application.ui.menu.impl.MenuFactoryImpl;\n\nimport com.make.equo.analytics.internal.api.AnalyticsService;\nimport com.make.equo.application.api.IEquoApplication;\nimport com.make.equo.application.impl.EnterFullScreenModeRunnable;\nimport com.make.equo.contribution.api.EquoContributionBuilder;\nimport com.make.equo.server.api.IEquoServer;\nimport com.make.equo.server.offline.api.filters.IHttpRequestFilter;\n\npublic class OptionalViewBuilder {\n\n	private IEquoServer equoServer;\n	private ViewBuilder viewBuilder;\n	private EquoApplicationBuilder equoApplicationBuilder;\n\n	private AnalyticsService analyticsService;\n	private MMenu mainMenu;\n	private EquoContributionBuilder mainAppBuilder;\n	private EquoContributionBuilder offlineSupportBuilder;\n\n	OptionalViewBuilder(ViewBuilder viewBuilder, IEquoServer equoServer, AnalyticsService analyticsService,\n			EquoContributionBuilder mainAppBuilder, EquoContributionBuilder offlineSupportBuilder, IEquoApplication equoApp) {\n		this.viewBuilder = viewBuilder;\n		this.equoServer = equoServer;\n		this.analyticsService = analyticsService;\n		this.mainAppBuilder = mainAppBuilder;\n		this.offlineSupportBuilder = offlineSupportBuilder;\n		this.equoApplicationBuilder = viewBuilder.getEquoApplicationBuilder();\n	}\n\n	public OptionalViewBuilder addShortcut(String keySequence, Runnable runnable) {\n		return addShortcut(keySequence, runnable, null);\n	}\n\n	public OptionalViewBuilder addShortcut(String keySequence, Runnable runnable, String userEvent) {\n		EquoApplicationBuilder equoAppBuilder = this.viewBuilder.getEquoApplicationBuilder();\n		new GlobalShortcutBuilder(equoAppBuilder, this.viewBuilder.getPart().getElementId(), runnable, userEvent)\n				.addGlobalShortcut(keySequence);\n		return this;\n	}\n\n	public OptionalViewBuilder addShortcut(String keySequence, String userEvent) {\n		return addShortcut(keySequence, null, userEvent);\n	}\n\n	/**\n	 * Add a Javascript script that can modify the html content of the application.\n	 * This script is added to the main page of the Equo application.\n	 * \n	 * Uses cases a custom Javascript script include the removal, addition, and\n	 * modification of existing HTML elements. An already existing application can\n	 * work perfectly on the web, but it will need some changes to be adapted to the\n	 * Desktop. Adding a custom js script allows to perform this kind of task.\n	 * \n	 * @param scriptPath the path to the Javascript script or a URL. Note that this\n	 *                   argument can be either a path which is relative to the\n	 *                   source folder where the script is defined or a well formed\n	 *                   URL. For example, if a script 'x.js' is defined inside a\n	 *                   folder 'y' which is defined inside a source folder\n	 *                   'resources', the path to the script will be 'y/x.js'.\n	 * \n	 * @return this builder\n	 * @throws IOException\n	 * @throws URISyntaxException\n	 * \n	 */\n	public OptionalViewBuilder withCustomScript(String scriptPath) throws IOException, URISyntaxException {\n		mainAppBuilder.withScriptFile(scriptPath);\n		return this;\n	}\n\n\n	public OptionalViewBuilder withCustomStyle(String stylePath) throws IOException, URISyntaxException {\n		mainAppBuilder.withStyleFile(stylePath);\n		return this;\n	}\n\n	/**\n	 * Enable an offline cache which will be used when there is no internet\n	 * connection or a limited one. This functionality will only work if and only if\n	 * the application was run at least once with a working internet connection.\n	 * \n	 * @return this optional builder\n	 */\n	public OptionalViewBuilder enableOfflineSupport() {\n		equoServer.enableOfflineCache();\n		return this;\n	}\n\n	public OptionalViewBuilder addOfflineSupportFilter(IHttpRequestFilter httpRequestFilter) {\n		equoServer.addOfflineSupportFilter(httpRequestFilter);\n		return this;\n	}\n\n	/**\n	 * Add a limited or no connection custom page for the case that there is no\n	 * internet connection or a limited one. If an offline cache is enabled, see\n	 * {@link #enableOfflineSupport()}, then this method has no effect.\n	 * \n	 * @param limitedConnectionPagePath\n	 * @return this optional builder\n	 * @throws URISyntaxException\n	 */\n	public OptionalViewBuilder addLimitedConnectionPage(String limitedConnectionPagePath) throws URISyntaxException {\n		offlineSupportBuilder.withBaseHtmlResource(limitedConnectionPagePath);\n		return this;\n	}\n\n	public EquoApplicationBuilder start() {\n		return this.viewBuilder.start();\n	}\n\n	public MenuBuilder withMainMenu(String menuLabel) {\n		mainMenu = equoApplicationBuilder.getmWindow().getMainMenu();\n		return new MenuBuilder(this).addMenu(menuLabel);\n	}\n\n	EquoApplicationBuilder getEquoApplicationBuilder() {\n		return equoApplicationBuilder;\n	}\n\n	MMenu getMainMenu() {\n		return mainMenu;\n	}\n\n	public OptionalViewBuilder addFullScreenModeShortcut(String keySequence) {\n		return addShortcut(keySequence, EnterFullScreenModeRunnable.instance);\n	}\n\n	public OptionalViewBuilder enableAnalytics() {\n		analyticsService.enableAnalytics();\n		return this;\n	}\n\n	public OptionalViewBuilder withBaseHtml(String baseHtmlFile) throws URISyntaxException {\n		mainAppBuilder.withBaseHtmlResource(baseHtmlFile);\n		return this;\n	}\n\n	public ToolbarBuilder withToolbar() {\n		return new ToolbarBuilder(this,equoApplicationBuilder.getmWindow()).addToolbar();\n	}	\n\n}\n",
							language,
							resource
						);
					}
					return model;
				}

				this.editor = monaco.editor.create(element, {
					model: this.model,
					lightbulb: {
						enabled: true
					},
					automaticLayout: true
				}, { textModelService: {
						createModelReference: function(uri: monaco.Uri) {
							// console.log(JSON.stringify(uri.fsPath));
							// var modelContent: string = "";
							// ws.on(values.namespace + "_modelResolved", (content: string) => {
							// 	modelContent = content;
							// });
							const textEditorModel = {
								load() {
								let result = Promise.resolve(textEditorModel);
								self.editor.layout(undefined);
								return result;
								},
								dispose() {},
								textEditorModel: getModel(uri)
							}
							// modelContent = "package com.make.equo.application.model;\n\nimport java.io.IOException;\nimport java.net.URISyntaxException;\n\nimport org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;\nimport org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;\nimport org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;\nimport org.eclipse.e4.ui.model.application.ui.menu.MHandledToolItem;\nimport org.eclipse.e4.ui.model.application.ui.menu.MMenu;\nimport org.eclipse.e4.ui.model.application.ui.menu.MToolBar;\nimport org.eclipse.e4.ui.model.application.ui.menu.impl.MenuFactoryImpl;\n\nimport com.make.equo.analytics.internal.api.AnalyticsService;\nimport com.make.equo.application.api.IEquoApplication;\nimport com.make.equo.application.impl.EnterFullScreenModeRunnable;\nimport com.make.equo.contribution.api.EquoContributionBuilder;\nimport com.make.equo.server.api.IEquoServer;\nimport com.make.equo.server.offline.api.filters.IHttpRequestFilter;\n\npublic class OptionalViewBuilder {\n\n	private IEquoServer equoServer;\n	private ViewBuilder viewBuilder;\n	private EquoApplicationBuilder equoApplicationBuilder;\n\n	private AnalyticsService analyticsService;\n	private MMenu mainMenu;\n	private EquoContributionBuilder mainAppBuilder;\n	private EquoContributionBuilder offlineSupportBuilder;\n\n	OptionalViewBuilder(ViewBuilder viewBuilder, IEquoServer equoServer, AnalyticsService analyticsService,\n			EquoContributionBuilder mainAppBuilder, EquoContributionBuilder offlineSupportBuilder, IEquoApplication equoApp) {\n		this.viewBuilder = viewBuilder;\n		this.equoServer = equoServer;\n		this.analyticsService = analyticsService;\n		this.mainAppBuilder = mainAppBuilder;\n		this.offlineSupportBuilder = offlineSupportBuilder;\n		this.equoApplicationBuilder = viewBuilder.getEquoApplicationBuilder();\n	}\n\n	public OptionalViewBuilder addShortcut(String keySequence, Runnable runnable) {\n		return addShortcut(keySequence, runnable, null);\n	}\n\n	public OptionalViewBuilder addShortcut(String keySequence, Runnable runnable, String userEvent) {\n		EquoApplicationBuilder equoAppBuilder = this.viewBuilder.getEquoApplicationBuilder();\n		new GlobalShortcutBuilder(equoAppBuilder, this.viewBuilder.getPart().getElementId(), runnable, userEvent)\n				.addGlobalShortcut(keySequence);\n		return this;\n	}\n\n	public OptionalViewBuilder addShortcut(String keySequence, String userEvent) {\n		return addShortcut(keySequence, null, userEvent);\n	}\n\n	/**\n	 * Add a Javascript script that can modify the html content of the application.\n	 * This script is added to the main page of the Equo application.\n	 * \n	 * Uses cases a custom Javascript script include the removal, addition, and\n	 * modification of existing HTML elements. An already existing application can\n	 * work perfectly on the web, but it will need some changes to be adapted to the\n	 * Desktop. Adding a custom js script allows to perform this kind of task.\n	 * \n	 * @param scriptPath the path to the Javascript script or a URL. Note that this\n	 *                   argument can be either a path which is relative to the\n	 *                   source folder where the script is defined or a well formed\n	 *                   URL. For example, if a script 'x.js' is defined inside a\n	 *                   folder 'y' which is defined inside a source folder\n	 *                   'resources', the path to the script will be 'y/x.js'.\n	 * \n	 * @return this builder\n	 * @throws IOException\n	 * @throws URISyntaxException\n	 * \n	 */\n	public OptionalViewBuilder withCustomScript(String scriptPath) throws IOException, URISyntaxException {\n		mainAppBuilder.withScriptFile(scriptPath);\n		return this;\n	}\n\n\n	public OptionalViewBuilder withCustomStyle(String stylePath) throws IOException, URISyntaxException {\n		mainAppBuilder.withStyleFile(stylePath);\n		return this;\n	}\n\n	/**\n	 * Enable an offline cache which will be used when there is no internet\n	 * connection or a limited one. This functionality will only work if and only if\n	 * the application was run at least once with a working internet connection.\n	 * \n	 * @return this optional builder\n	 */\n	public OptionalViewBuilder enableOfflineSupport() {\n		equoServer.enableOfflineCache();\n		return this;\n	}\n\n	public OptionalViewBuilder addOfflineSupportFilter(IHttpRequestFilter httpRequestFilter) {\n		equoServer.addOfflineSupportFilter(httpRequestFilter);\n		return this;\n	}\n\n	/**\n	 * Add a limited or no connection custom page for the case that there is no\n	 * internet connection or a limited one. If an offline cache is enabled, see\n	 * {@link #enableOfflineSupport()}, then this method has no effect.\n	 * \n	 * @param limitedConnectionPagePath\n	 * @return this optional builder\n	 * @throws URISyntaxException\n	 */\n	public OptionalViewBuilder addLimitedConnectionPage(String limitedConnectionPagePath) throws URISyntaxException {\n		offlineSupportBuilder.withBaseHtmlResource(limitedConnectionPagePath);\n		return this;\n	}\n\n	public EquoApplicationBuilder start() {\n		return this.viewBuilder.start();\n	}\n\n	public MenuBuilder withMainMenu(String menuLabel) {\n		mainMenu = equoApplicationBuilder.getmWindow().getMainMenu();\n		return new MenuBuilder(this).addMenu(menuLabel);\n	}\n\n	EquoApplicationBuilder getEquoApplicationBuilder() {\n		return equoApplicationBuilder;\n	}\n\n	MMenu getMainMenu() {\n		return mainMenu;\n	}\n\n	public OptionalViewBuilder addFullScreenModeShortcut(String keySequence) {\n		return addShortcut(keySequence, EnterFullScreenModeRunnable.instance);\n	}\n\n	public OptionalViewBuilder enableAnalytics() {\n		analyticsService.enableAnalytics();\n		return this;\n	}\n\n	public OptionalViewBuilder withBaseHtml(String baseHtmlFile) throws URISyntaxException {\n		mainAppBuilder.withBaseHtmlResource(baseHtmlFile);\n		return this;\n	}\n\n	public ToolbarBuilder withToolbar() {\n		return new ToolbarBuilder(this,equoApplicationBuilder.getmWindow()).addToolbar();\n	}	\n\n}\n";
							return Promise.resolve({
								object: textEditorModel,
								dispose() {}
							})
						},
						registerTextModelContentProvider: () => ({ dispose: () => {} })
					}
				});

				StandaloneCodeEditorServiceImpl.prototype.doOpenEditor = function (editor: any, input: any) {
					ws.send("_openCodeEditor", { path: input.resource.path, selection: input.options.selection });
					return null;
				};
				let namespace = values.namespace;
				RenameAction.prototype.runCommand = function (accessor: any, args: any) {
					ws.send(namespace + "_makeRename");
					return null;
				};

				if (this.shortcutsAdded) {
					this.activateShortcuts();
				}

				this.clearDirtyState();
				this.bindEquoFunctions();

				this.connectLsp(values.lspPath, values.rootUri, language);

				this.wasCreated = true;
				this.editor.onDidChangeModelContent(() => {
					this.notifyChanges();
				});
			}
		});

		if (filePath)
			this.filePath = filePath;
		this.webSocket.send("_createEditor", { filePath: filePath });
	}

	public isDirty(): boolean {
		return this.lastSavedVersionId !== this.model.getAlternativeVersionId();
	}

	public clearDirtyState() {
		this.lastSavedVersionId = this.model.getAlternativeVersionId();
	}

	public setTextLabel(text: string): void {
		this.elemdiv.innerText = text;
	}

	public setLabelChanges(element: HTMLElement): void {
		this.elemdiv = element;
	}

	public activateShortcuts(): void {
		this.shortcutsAdded = true;
		let thisEditor = this;
		this.editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KEY_S, function () {
			thisEditor.save();
		});
	}

	private actionForFileChange(): void {
		if (!this.isDirty()) {
			this.reload();
			return;
		}
		this.setTextLabel("New changes in the document. Click here to reaload");
	}

	private getLanguageOfFile(name: string): monaco.languages.ILanguageExtensionPoint | undefined {
		let ext = '.' + name.split('.').pop();
		let languages = monaco.languages.getLanguages();
		for (let l of languages) {
			if (l.extensions) {
				for (let e of l.extensions) {
					if (e === ext) {
						return l;
					}
				}
			}
		}
		return undefined;
	}

	private bindEquoFunctions(): void {
		this.editor.onDidChangeCursorSelection((e: any) => {
			let selection = e.selection;
			let offsetStart = this.model.getOffsetAt({lineNumber: selection.startLineNumber, column: selection.startColumn});
			let offsetEnd = this.model.getOffsetAt({lineNumber: selection.endLineNumber, column: selection.endColumn});
			let length = offsetEnd - offsetStart;
			this.webSocket.send(this.namespace + "_selection", {offset: offsetStart, length: length});
		});

		this.webSocket.on(this.namespace + "_doReinitialization", (values: { text: string; name: string; lspPath?: string; rootUri?: string }) => {
			this.fileName = name;

			this.model.dispose();
			let language = this.createModelAndGetLanguage(values.name, values.text);

			this.editor.setModel(this.model);
			this.setTextLabel("");

			if (this.lspws) {
				//@ts-ignore
				this.lspws.close(1000, '', { keepClosed: true, fastClose: true, delay: 0 });
			}
			if (this.languageClient)
				this.languageClient.stop();

			this.connectLsp(values.lspPath, values.rootUri, language);
		});

		this.webSocket.on(this.namespace + "_filePathChanged", (values: { path: string; name: string }) => {
			this.filePath = values.path;
			this.fileName = values.name;
			if (this.filePathChangedCallback)
				this.filePathChangedCallback(this.filePath, this.fileName);
		});

		this.webSocket.on(this.namespace + "_doFind", () => {
			this.editor.getAction("actions.find").run();
		});


		this.webSocket.on(this.namespace + "_getContents", () => {
			this.webSocket.send(this.namespace + "_doGetContents", { contents: this.editor.getValue() });
		});

		this.webSocket.on(this.namespace + "_undo", () => {
			(this.model as any).undo();
		});

		this.webSocket.on(this.namespace + "_redo", () => {
			(this.model as any).redo();
		});

		this.webSocket.on(this.namespace + "_didSave", () => {
			this.clearDirtyState();
			this.notifyChanges();
		});


		this.webSocket.on(this.namespace + "_subscribeModelChanges", () => {
			this.sendChangesToJavaSide = true;
		});


		this.webSocket.on(this.namespace + "_doCopy", () => {
			this.editor.getAction("editor.action.clipboardCopyAction").run();
		});


		this.webSocket.on(this.namespace + "_doCut", () => {
			this.editor.getAction("editor.action.clipboardCutAction").run();
		});


		this.webSocket.on(this.namespace + "_doPaste", () => {
			this.editor.focus();
			this.webSocket.send(this.namespace + "_canPaste");
		});


		this.webSocket.on(this.namespace + "_doSelectAll", () => {
			this.editor.focus();
			this.webSocket.send(this.namespace + "_canSelectAll");
		});

		this.webSocket.on(this.namespace + "_reportChanges", () => {
			this.filePathChangedCallback();
		});

		this.webSocket.on(this.namespace + "_doReload", (content: string) => {
			this.editor.setValue(content);
			this.clearDirtyState();
			this.setTextLabel("");
			this.notifyChanges();
		});

		this.webSocket.on(this.namespace + "_setContent", (content: string) => {
			this.editor.setValue(content);
			if (!this.isDirty()){
				this.lastSavedVersionId = this.lastSavedVersionId - 1;
			}
			this.setTextLabel("");
			this.notifyChanges();
		});

		this.webSocket.on(this.namespace + "_selectAndReveal", (values: { offset: number; length: number }) => {
			let position = this.model.getPositionAt(values.offset);
			let positionEnd = this.model.getPositionAt(values.offset + values.length);
			let range = {startLineNumber: position.lineNumber, startColumn: position.column,
				endLineNumber: positionEnd.lineNumber, endColumn: positionEnd.column};
			this.editor.setPosition(position);
			this.editor.revealRangeInCenter(range);
			this.editor.setSelection(range);
		});

	}

	private notifyChanges(): void {
		if (this.sendChangesToJavaSide) {
			this.webSocket.send(this.namespace + "_changesNotification",
				{ isDirty: this.lastSavedVersionId !== this.model.getAlternativeVersionId(), canRedo: (this.model as any).canRedo(),
					canUndo: (this.model as any).canUndo(), content: this.editor.getValue() });
		}
		this.notifyChangeCallback();
	}
}

export namespace EquoMonaco {

	var websocket: EquoWebSocket = EquoWebSocketService.get()

	export function create(element: HTMLElement, filePath?: string): EquoMonacoEditor {
		let monacoEditor = new EquoMonacoEditor(websocket);
		monacoEditor.create(element, filePath);
		return monacoEditor;
	}

	export function addLspServer(executionParameters: Array<string>, extensions: Array<string>): void {
		websocket.send("_addLspServer", { executionParameters: executionParameters, extensions: extensions });
	}

	export function removeLspServer(extensions: Array<string>): void {
		websocket.send("_removeLspServer", { extensions: extensions });
	}

	export function addLspWsServer(fullServerPath: string, extensions: Array<string>): void {
		websocket.send("_addLspWsServer", { fullServerPath: fullServerPath, extensions: extensions });
	}
}