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

	private generateTextModelService(language: string): any{
		let self = this;
		let ws = this.webSocket;
		let getModel = function(resource: monaco.Uri, modelContent: string){
			var model = null;
			if(resource !== null)
				model = monaco.editor.getModel(resource);
			if (model !== null && self.getEditor().getModel()?.uri.fsPath != resource.fsPath
				&& model.getValue() != modelContent){
				model.dispose();
				model = null;
			}
			if(model == null) {
				model = monaco.editor.createModel(
					modelContent,
					language,
					resource
				);
			}
			return model;
		}

		return {
			createModelReference: function(uri: monaco.Uri) {
				return new Promise(function (r, e) {
					if (self.getEditor().getModel()!.uri.fsPath == uri.fsPath){
						const textEditorModel = {
							load() {
							return Promise.resolve(textEditorModel)
							},
							dispose() {},
							textEditorModel: monaco.editor.getModel(uri)
						}
						r({
							object: textEditorModel,
							dispose() {}
						});
					}else{
						ws.on(self.namespace + "_modelResolved" + uri.fsPath, (content: string) => {
							let previewModel = getModel(uri, content);
							let textEditorModel = {
								load() {
								return Promise.resolve(textEditorModel)
								},
								dispose() {},
								textEditorModel: previewModel
							}
							r({
								object: textEditorModel,
								dispose() {}
							});
							let container = self.elemdiv.parentElement;
							let width = container?.clientWidth;
							if (width == null){
								width = 0;
							}
							let height = container?.clientHeight;
							if (height == null){
								height = 0;
							}
							self.editor.layout({height: height + 1, width: width + 1});
							self.editor.layout({height: height, width: width});
						});
						ws.send(self.namespace + "_getContentOf", {path: uri.fsPath});
					}
				 });
			},
			registerTextModelContentProvider: () => ({ dispose: () => {} })
		}
	}

	private editorTweaks(): void{
		let ws = this.webSocket;
		let namespace = this.namespace;
		this.editor.addAction({
			id: 'findAllReferences',
			label: 'Find All References',
			keybindings: [
				monaco.KeyMod.Alt | monaco.KeyMod.Shift | monaco.KeyCode.F12
			],
			precondition: "editorHasSelection",
			contextMenuGroupId: 'navigation',
			contextMenuOrder: 6.0,
			run: function(editor: monaco.editor.IStandaloneCodeEditor): void {
				ws.send(namespace + "_findAllReferences");
			}
		});

		StandaloneCodeEditorServiceImpl.prototype.doOpenEditor = function (editor: any, input: any) {
			ws.send("_openCodeEditor", { path: input.resource.path, selection: input.options.selection });
			return null;
		};
		RenameAction.prototype.runCommand = function (accessor: any, args: any) {
			ws.send(namespace + "_makeRename");
			return null;
		};
	}

	public create(element: HTMLElement, filePath?: string): void {
		this.webSocket.on("_doCreateEditor", (values: { text: string; name: string; namespace: string; lspPath?: string; rootUri?: string }) => {
			if (!this.wasCreated) {
				this.namespace = values.namespace;

				element.appendChild(this.elemdiv);

				let language = this.createModelAndGetLanguage(values.name, values.text);

				this.editor = monaco.editor.create(element, {
					model: this.model,
					lightbulb: {
						enabled: true
					},
					automaticLayout: true
				}, { textModelService: this.generateTextModelService(language)});

				this.editorTweaks();

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