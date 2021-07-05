/****************************************************************************
**
** Copyright (C) 2021 Equo
**
** This file is part of Equo Framework.
**
** Commercial License Usage
** Licensees holding valid commercial Equo licenses may use this file in
** accordance with the commercial license agreement provided with the
** Software or, alternatively, in accordance with the terms contained in
** a written agreement between you and Equo. For licensing terms
** and conditions see https://www.equoplatform.com/terms.
**
** GNU General Public License Usage
** Alternatively, this file may be used under the terms of the GNU
** General Public License version 3 as published by the Free Software
** Foundation. Please review the following
** information to ensure the GNU General Public License requirements will
** be met: https://www.gnu.org/licenses/gpl-3.0.html.
**
****************************************************************************/

import { listen } from "vscode-ws-jsonrpc";
import {
  MonacoLanguageClient,
  CloseAction,
  ErrorAction,
  MonacoServices,
  createConnection,
} from "monaco-languageclient";
const normalizeUrl = require("normalize-url");
const ReconnectingWebSocket = require("reconnecting-websocket");
import * as monaco from "monaco-editor";
import { EquoCommService, EquoComm } from "@equo/comm";
// @ts-ignore
import { StandaloneCodeEditorServiceImpl } from "monaco-editor/esm/vs/editor/standalone/browser/standaloneCodeServiceImpl.js";
// @ts-ignore
import { RenameAction } from "monaco-editor/esm/vs/editor/contrib/rename/rename.js";

export class EquoMonacoEditor {
  private lastSavedVersionId!: number;
  private editor!: monaco.editor.IStandaloneCodeEditor;
  private model!: monaco.editor.ITextModel;
  private namespace!: string;
  private wasCreated: boolean = false;
  private comm: EquoComm;
  private languageClient!: MonacoLanguageClient;
  private lspws!: WebSocket;
  private filePath!: string;
  private fileName!: string;
  private filePathChangedCallback!: Function;
  private notifyChangeCallback!: Function;
  private elemdiv: HTMLElement;
  private sendChangesToJavaSide: boolean = false;
  private shortcutsAdded: boolean = false;

  /**
   * @name EquoMonacoEditor
   * @class
   */
  constructor(comm: EquoComm) {
    this.comm = comm;
    this.elemdiv = document.createElement("div");
    this.elemdiv.addEventListener("click", (e: Event) => this.reload());
    this.elemdiv.style.background = "#DD944F";
    this.elemdiv.style.textAlign = "center";
    this.filePathChangedCallback = this.actionForFileChange;
    this.notifyChangeCallback = () => {};
  }
  /**
   * Gets the Monaco editor.
   * @returns {IStandaloneCodeEditor}
   */
  public getEditor(): monaco.editor.IStandaloneCodeEditor {
    return this.editor;
  }
  /**
   * Gets the file path
   * @returns {string}
   */
  public getFilePath(): string {
    return this.filePath;
  }
  /**
   * Gets the file name.
   * @returns {string}
   */
  public getFileName(): string {
    return this.fileName;
  }
  /**
   * Destroy this model.
   * @returns {void}
   */
  public dispose(): void {
    if (this.lspws) {
      //@ts-ignore
      this.lspws.close(1000, "", {
        keepClosed: true,
        fastClose: true,
        delay: 0,
      });
    }
    if (this.languageClient) this.languageClient.stop();
    this.model.dispose();
    this.editor.dispose();
    this.comm.send(this.namespace + "_disposeEditor");
  }
  /**
   * Saves the file content in custom path.
   * @returns {void}
   */
  public saveAs(): void {
    this.comm.send(this.namespace + "_doSaveAs");
  }
  /**
   * Saves the file content in default path.
   * @returns {void}
   */
  public save(): void {
    this.comm.send(this.namespace + "_doSave");
  }
  /**
   * Reloads the document content.
   * @returns {void}
   */
  public reload(): void {
    this.comm.send(this.namespace + "_doReload");
  }
  /**
   * @callback listenerCallback
   * @param {string} filePath - Path file.
   * @param {string} fileName - File name.
   */
  /**
   * Sets the custom callback when file path changed.
   * @param {listenerCallback} callback 
   * @returns {void}
   */
  public setFilePathChangedListener(callback: (filePath: string, fileName: string) => {}) {
    this.filePathChangedCallback = callback;
  }
  /**
   * Sets a custom action when file changes are reported.
   * @param {funtion} callback
   * @returns {void}
   */
  public setActionOnNotifyChanges(callback: Function) {
    this.notifyChangeCallback = callback;
  }

  private createModelAndGetLanguage(file: string, content: string): string {
    let l = this.getLanguageOfFile(file);
    let language = "";

    if (l) {
      monaco.languages.register(l);
      language = l.id;
    } else {
      language = "userdefinedlanguage";
      monaco.languages.register({
        id: language,
      });
    }
    this.model = monaco.editor.createModel(
      content,
      language,
      monaco.Uri.file(file) // uri
    );
    return language;
  }

  private connectLsp(
    lspPath: string | undefined,
    rootUri: string | undefined,
    language: string
  ): void {
    if (lspPath) {
      MonacoServices.install(monaco, { rootUri: rootUri });

      // create the web socket
      var url = normalizeUrl(lspPath);
      this.lspws = createWebSocket(url);
      var webSocket = this.lspws;
      // listen when the web socket is opened
      listen({
        webSocket,
        onConnection: (connection) => {
          // create and start the language client
          this.languageClient = createLanguageClient(connection);
          var disposable = this.languageClient.start();
          connection.onClose(() => disposable.dispose());
        },
      });
    }

    function createLanguageClient(
      connection: any
    ): MonacoLanguageClient {
      return new MonacoLanguageClient({
        name: "Sample Language Client",
        clientOptions: {
          // use a language id as a document selector
          documentSelector: [language],
          // disable the default error handler
          errorHandler: {
            error: () => ErrorAction.Continue,
            closed: () => CloseAction.DoNotRestart,
          },
        },
        // create a language client connection from the JSON RPC connection on demand
        connectionProvider: {
          get: (errorHandler, closeHandler) => {
            return Promise.resolve(
              createConnection(connection, errorHandler, closeHandler)
            );
          },
        },
      });
    }

    function createWebSocket(url: string): WebSocket {
      const socketOptions = {
        maxReconnectionDelay: 10000,
        minReconnectionDelay: 1000,
        reconnectionDelayGrowFactor: 1.3,
        connectionTimeout: 10000,
        maxRetries: Infinity,
        debug: false,
      };
      return new ReconnectingWebSocket(url, [], socketOptions);
    }
  }

  private generateTextModelService(language: string): any {
    let self = this;
    let comm = this.comm;
    let getModel = function (resource: monaco.Uri, modelContent: string) {
      var model = null;
      if (resource !== null) model = monaco.editor.getModel(resource);
      if (
        model !== null &&
        self.getEditor().getModel()?.uri.fsPath != resource.fsPath &&
        model.getValue() != modelContent
      ) {
        model.dispose();
        model = null;
      }
      if (model == null) {
        model = monaco.editor.createModel(modelContent, language, resource);
      }
      return model;
    };

    return {
      createModelReference: function (uri: monaco.Uri) {
        return new Promise(function (r, e) {
          if (self.getEditor().getModel()!.uri.fsPath == uri.fsPath) {
            const textEditorModel = {
              load() {
                return Promise.resolve(textEditorModel);
              },
              dispose() {},
              textEditorModel: monaco.editor.getModel(uri),
            };
            r({
              object: textEditorModel,
              dispose() {},
            });
          } else {
            comm.on(
              self.namespace + "_modelResolved" + uri.fsPath,
              (content: string) => {
                let previewModel = getModel(uri, content);
                let textEditorModel = {
                  load() {
                    return Promise.resolve(textEditorModel);
                  },
                  dispose() {},
                  textEditorModel: previewModel,
                };
                r({
                  object: textEditorModel,
                  dispose() {},
                });
                let container = self.elemdiv.parentElement;
                let width = container?.clientWidth;
                if (width == null) {
                  width = 0;
                }
                let height = container?.clientHeight;
                if (height == null) {
                  height = 0;
                }
                self.editor.layout({ height: height + 1, width: width + 1 });
                self.editor.layout({ height: height, width: width });
              }
            );
            comm.send(self.namespace + "_getContentOf", { path: uri.fsPath });
          }
        });
      },
      registerTextModelContentProvider: () => ({ dispose: () => {} }),
    };
  }

  private editorTweaks(bindEclipseLsp: boolean): void {
    let comm = this.comm;
    let namespace = this.namespace;

    this.editor.addAction({
      id: "wordWrap",
      label: "Word Wrap",
      keybindings: [monaco.KeyMod.Alt | monaco.KeyCode.KEY_Z],
      run: function (editor: monaco.editor.IStandaloneCodeEditor): void {
        if (editor.getOption(monaco.editor.EditorOption.wordWrap) == "off") {
          editor.updateOptions({ wordWrap: "on" });
        } else {
          editor.updateOptions({ wordWrap: "off" });
        }
      },
    });
    this.editor.addAction({
      id: "revertFile",
      label: "Revert File",
      contextMenuGroupId: "navigation",
      contextMenuOrder: 1.0,
      run: function (editor: monaco.editor.IStandaloneCodeEditor): void {
        comm.send(namespace + "_doReload");
      },
    });
    if (bindEclipseLsp) {
      this.editor.addAction({
        id: "findAllReferences",
        label: "Find All References",
        keybindings: [
          monaco.KeyMod.Alt | monaco.KeyMod.Shift | monaco.KeyCode.F12,
        ],
        precondition: "editorHasSelection",
        contextMenuGroupId: "navigation",
        contextMenuOrder: 6.0,
        run: function (editor: monaco.editor.IStandaloneCodeEditor): void {
          comm.send(namespace + "_findAllReferences");
        },
      });
    }

    StandaloneCodeEditorServiceImpl.prototype.doOpenEditor = function (
      editor: any,
      input: any
    ) {
      comm.send("_openCodeEditor", {
        path: input.resource.path,
        selection: input.options.selection,
      });
      return null;
    };
    RenameAction.prototype.runCommand = function (accessor: any, args: any) {
      comm.send(namespace + "_makeRename");
      return null;
    };
  }
  /**
   * Initializes the EquoMonacoEditor instance.
   * @param {HTMLElement} element - Element on which the editor will be placed
   * @param {string} [filePath] - Optional
   */
  public create(element: HTMLElement, filePath?: string): void {
    this.comm.on(
      "_doCreateEditor",
      (values: {
        text: string;
        name: string;
        namespace: string;
        bindEclipseLsp: boolean;
        lspPath?: string;
        rootUri?: string;
      }) => {
        if (!this.wasCreated) {
          this.namespace = values.namespace;

          element.appendChild(this.elemdiv);

          let language = this.createModelAndGetLanguage(
            values.name,
            values.text
          );

          this.editor = monaco.editor.create(
            element,
            {
              model: this.model,
              lightbulb: {
                enabled: true,
              },
              automaticLayout: true,
            },
            { textModelService: this.generateTextModelService(language) }
          );

          this.editorTweaks(values.bindEclipseLsp);

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
      }
    );

    if (filePath) this.filePath = filePath;
    this.comm.send("_createEditor", { filePath: filePath });
  }
  /**
   * Gets the document state.
   * @returns {boolean} True if dirty (contains unsaved modifications) or false if not dirty.
   */
  public isDirty(): boolean {
    return this.lastSavedVersionId !== this.model.getAlternativeVersionId();
  }
  /**
   * Cleans the dirty state from the file.
   * @returns {void}
   */
  public clearDirtyState() {
    this.lastSavedVersionId = this.model.getAlternativeVersionId();
  }
  /**
   * Sets the message when file was externally modified.
   * @param {string} text - Message.
   * @returns {void}
   */
  public setTextLabel(text: string): void {
    this.elemdiv.innerText = text;
  }
  /**
   * Sets the DOM element for output message when state is dirty.
   * @param {HTMLElement} element - Element for contain dirty status message
   * @returns {void}
   */
  public setLabelChanges(element: HTMLElement): void {
    this.elemdiv = element;
  }
  /**
   * Activates the shortcuts
   * @returns {void}
   */
  public activateShortcuts(): void {
    this.shortcutsAdded = true;
    let thisEditor = this;
    if (this.editor) {
      this.editor.addCommand(
        monaco.KeyMod.CtrlCmd | monaco.KeyCode.KEY_S,
        function () {
          thisEditor.save();
        }
      );
    }
  }

  private actionForFileChange(): void {
    if (!this.isDirty()) {
      this.reload();
      return;
    }
    this.setTextLabel("New changes in the document. Click here to reaload");
  }

  private getLanguageOfFile(
    name: string
  ): monaco.languages.ILanguageExtensionPoint | undefined {
    let ext = "." + name.split(".").pop();
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
      let offsetStart = this.model.getOffsetAt({
        lineNumber: selection.startLineNumber,
        column: selection.startColumn,
      });
      let offsetEnd = this.model.getOffsetAt({
        lineNumber: selection.endLineNumber,
        column: selection.endColumn,
      });
      let length = offsetEnd - offsetStart;
      this.comm.send(this.namespace + "_selection", {
        offset: offsetStart,
        length: length,
      });
    });

    this.comm.on(
      this.namespace + "_doReinitialization",
      (values: {
        text: string;
        name: string;
        lspPath?: string;
        rootUri?: string;
      }) => {
        this.fileName = name;

        this.model.dispose();
        let language = this.createModelAndGetLanguage(values.name, values.text);

        this.editor.setModel(this.model);
        this.setTextLabel("");

        if (this.lspws) {
          //@ts-ignore
          this.lspws.close(1000, "", {
            keepClosed: true,
            fastClose: true,
            delay: 0,
          });
        }
        if (this.languageClient) this.languageClient.stop();

        this.connectLsp(values.lspPath, values.rootUri, language);
      }
    );

    this.comm.on(
      this.namespace + "_filePathChanged",
      (values: { path: string; name: string }) => {
        this.filePath = values.path;
        this.fileName = values.name;
        if (this.filePathChangedCallback)
          this.filePathChangedCallback(this.filePath, this.fileName);
      }
    );

    this.comm.on(this.namespace + "_doFind", () => {
      this.editor.focus();
      this.editor.getAction("actions.find").run();
    });

    this.comm.on(this.namespace + "_getContents", () => {
      this.comm.send(this.namespace + "_doGetContents", {
        contents: this.editor.getValue(),
      });
    });

    this.comm.on(this.namespace + "_undo", () => {
      (this.model as any).undo();
    });

    this.comm.on(this.namespace + "_redo", () => {
      (this.model as any).redo();
    });

    this.comm.on(this.namespace + "_didSave", () => {
      this.clearDirtyState();
      this.notifyChanges();
    });

    this.comm.on(this.namespace + "_subscribeModelChanges", () => {
      this.sendChangesToJavaSide = true;
    });

    this.comm.on(this.namespace + "_doCopy", () => {
      document.execCommand("copy");
    });

    this.comm.on(this.namespace + "_doCut", () => {
      document.execCommand("cut");
    });

    this.comm.on(this.namespace + "_doPaste", () => {
      document.execCommand("paste");
    });

    this.comm.on(this.namespace + "_doSelectAll", () => {
      const range = this.editor.getModel()!.getFullModelRange();
      this.editor.setSelection(range);
    });

    this.comm.on(this.namespace + "_reportChanges", () => {
      this.filePathChangedCallback();
    });

    this.comm.on(this.namespace + "_reload", (content: string) => {
      let editor = this.editor;
      editor.executeEdits("", [
        {
          range: editor.getModel()!.getFullModelRange(),
          text: content,
        },
      ]);
      this.clearDirtyState();
      this.setTextLabel("");
      this.notifyChanges();
    });

    this.comm.on(
      this.namespace + "_setContent",
      (values: { content: string; asEdit: boolean }) => {
        let editor = this.editor;
        if (values.asEdit) {
          editor.executeEdits("", [
            {
              range: editor.getModel()!.getFullModelRange(),
              text: values.content,
            },
          ]);
        } else {
          editor.setValue(values.content);
        }
        if (!this.isDirty()) {
          this.lastSavedVersionId = this.lastSavedVersionId - 1;
        }
        this.setTextLabel("");
        this.notifyChanges();
      }
    );

    this.comm.on(
      this.namespace + "_selectAndReveal",
      (values: { offset: number; length: number }) => {
        let position = this.model.getPositionAt(values.offset);
        let positionEnd = this.model.getPositionAt(
          values.offset + values.length
        );
        let range = {
          startLineNumber: position.lineNumber,
          startColumn: position.column,
          endLineNumber: positionEnd.lineNumber,
          endColumn: positionEnd.column,
        };
        this.editor.setPosition(position);
        this.editor.revealRangeInCenter(range);
        this.editor.setSelection(range);
      }
    );
  }

  private notifyChanges(): void {
    if (this.sendChangesToJavaSide) {
      this.comm.send(this.namespace + "_changesNotification", {
        isDirty:
          this.lastSavedVersionId !== this.model.getAlternativeVersionId(),
        canRedo: (this.model as any).canRedo(),
        canUndo: (this.model as any).canUndo(),
        content: this.editor.getValue(),
      });
    }
    this.notifyChangeCallback();
  }
}
/**
 * @namespace
 * @description The Equo Editor is a drop-in replacement for the Eclipse Generic Editor. based on Monaco web editor (same as VSCode).
It brings the beauty and the capabilities of a modern editor into Eclipse.
 *
 */
export namespace EquoMonaco {
  var comm: EquoComm = EquoCommService.get();
  /**
   * Creates a new editor under DOM element.
   * @function
   * @name create
   * @param {HTMLElement} element 
   * @param {string} [filePath] - Optional.
   * @returns {EquoMonacoEditor}
   */
  export function create(
    element: HTMLElement,
    filePath?: string
  ): EquoMonacoEditor {
    let monacoEditor = new EquoMonacoEditor(comm);
    monacoEditor.create(element, filePath);
    return monacoEditor;
  }
  /**
   * Adds a lsp server to be used by the editors on the files with the given extensions.
   * @function
   * @name addLspServer
   * @param {string[]} executionParameters - The parameters needed to start the lsp server through stdio. Example: ["html-languageserver", "--stdio"].
   * @param {string[]} extensions - A collection of extensions for what the editor will use the given lsp server. The extensions must not have the initial dot. Example: ["php", "php4"].
   * @returns {void}
   */
  export function addLspServer(
    executionParameters: Array<string>,
    extensions: Array<string>
  ): void {
    comm.send("_addLspServer", {
      executionParameters: executionParameters,
      extensions: extensions,
    });
  }
  /**
   * Removes a lsp server assigned to the given extensions.
   * @function
   * @name removeLspServer
   * @param {string[]} extensions - A collection of the file extensions for which the previously assigned lsp will be removed The extensions must not have the initial dot. Example: ["php", "php4"].
   * @returns {void}
   */
  export function removeLspServer(extensions: Array<string>): void {
    comm.send("_removeLspServer", { extensions: extensions });
  }
  /**
   * Adds a lsp comm server to be used by the editors on the files with the given extensions.
   * @function
   * @name addLspWsServer
   * @param {string} fullServerPath - The full path to the lsp server. Example: ws://127.0.0.1:3000/lspServer.
   * @param {string[]} extensions - A collection of extensions for what the editor will use the given lsp server. The extensions must not have the initial dot. Example: ["php", "php4"].
   * @returns {void}
   */
  export function addLspWsServer(
    fullServerPath: string,
    extensions: Array<string>
  ): void {
    comm.send("_addLspWsServer", {
      fullServerPath: fullServerPath,
      extensions: extensions,
    });
  }
}
