## Classes

<dl>
<dt><a href="#EquoMonacoEditor">EquoMonacoEditor</a></dt>
<dd></dd>
</dl>

## Objects

<dl>
<dt><a href="#EquoMonaco">EquoMonaco</a> : <code>object</code></dt>
<dd><p>The Equo Editor is a drop-in replacement for the Eclipse Generic Editor. based on Monaco web editor (same as VSCode).
It brings the beauty and the capabilities of a modern editor into Eclipse.</p>
<p>see <a href="how-to-include-equo-components.md">more</a> about how to include Equo component in your projects.</p>
</dd>
</dl>

## Functions

<dl>
<dt><a href="#create">create(element, [filePath])</a> ⇒ <code><a href="#EquoMonacoEditor">EquoMonacoEditor</a></code></dt>
<dd><p>Create a new editor under DOM element.</p>
</dd>
<dt><a href="#addLspServer">addLspServer(executionParameters, extensions)</a> ⇒ <code>void</code></dt>
<dd><p>Add a lsp server to be used by the editors on the files with the given extensions.</p>
</dd>
<dt><a href="#removeLspServer">removeLspServer(extensions)</a> ⇒ <code>void</code></dt>
<dd><p>Remove a lsp server assigned to the given extensions.</p>
</dd>
<dt><a href="#addLspWsServer">addLspWsServer(fullServerPath, extensions)</a> ⇒ <code>void</code></dt>
<dd><p>Add a lsp websocket server to be used by the editors on the files with the given extensions.</p>
</dd>
</dl>

<a name="EquoMonacoEditor"></a>

## EquoMonacoEditor
**Kind**: global class  

* [EquoMonacoEditor](#EquoMonacoEditor)
    * [.getEditor()](#EquoMonacoEditor+getEditor) ⇒ <code>IStandaloneCodeEditor</code>
    * [.getFilePath()](#EquoMonacoEditor+getFilePath) ⇒ <code>string</code>
    * [.getFileName()](#EquoMonacoEditor+getFileName) ⇒ <code>string</code>
    * [.dispose()](#EquoMonacoEditor+dispose) ⇒ <code>void</code>
    * [.saveAs()](#EquoMonacoEditor+saveAs) ⇒ <code>void</code>
    * [.save()](#EquoMonacoEditor+save) ⇒ <code>void</code>
    * [.reload()](#EquoMonacoEditor+reload) ⇒ <code>void</code>
    * [.setFilePathChangedListener(callback)](#EquoMonacoEditor+setFilePathChangedListener) ⇒ <code>void</code>
    * [.setActionDirtyState(callback)](#EquoMonacoEditor+setActionDirtyState) ⇒ <code>void</code>
    * [.create(element, [filePath])](#EquoMonacoEditor+create)
    * [.isDirty()](#EquoMonacoEditor+isDirty) ⇒ <code>boolean</code>
    * [.clearDirtyState()](#EquoMonacoEditor+clearDirtyState) ⇒ <code>void</code>
    * [.setTextLabel(text)](#EquoMonacoEditor+setTextLabel) ⇒ <code>void</code>
    * [.setLabelChanges(element)](#EquoMonacoEditor+setLabelChanges) ⇒ <code>void</code>
    * [.activateShortcuts()](#EquoMonacoEditor+activateShortcuts) ⇒ <code>void</code>

<a name="EquoMonacoEditor+getEditor"></a>

### equoMonacoEditor.getEditor() ⇒ <code>IStandaloneCodeEditor</code>
Get editor.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  
<a name="EquoMonacoEditor+getFilePath"></a>

### equoMonacoEditor.getFilePath() ⇒ <code>string</code>
Get file path

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  
<a name="EquoMonacoEditor+getFileName"></a>

### equoMonacoEditor.getFileName() ⇒ <code>string</code>
Get file name.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  
<a name="EquoMonacoEditor+dispose"></a>

### equoMonacoEditor.dispose() ⇒ <code>void</code>
Dispose the editor.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  
<a name="EquoMonacoEditor+saveAs"></a>

### equoMonacoEditor.saveAs() ⇒ <code>void</code>
Action for save content in custom path.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  
<a name="EquoMonacoEditor+save"></a>

### equoMonacoEditor.save() ⇒ <code>void</code>
Action for save content in default path.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  
<a name="EquoMonacoEditor+reload"></a>

### equoMonacoEditor.reload() ⇒ <code>void</code>
Action for reload content document.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  
<a name="EquoMonacoEditor+setFilePathChangedListener"></a>

### equoMonacoEditor.setFilePathChangedListener(callback) ⇒ <code>void</code>
Set custom callback when file path changed.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  

| Param | Type |
| --- | --- |
| callback | <code>function</code> | 

<a name="EquoMonacoEditor+setActionDirtyState"></a>

### equoMonacoEditor.setActionDirtyState(callback) ⇒ <code>void</code>
Set custom action when state is dirty.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  

| Param | Type |
| --- | --- |
| callback | <code>Funtion</code> | 

<a name="EquoMonacoEditor+create"></a>

### equoMonacoEditor.create(element, [filePath])
Initialize EquoMonacoEditor.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  

| Param | Type | Description |
| --- | --- | --- |
| element | <code>HTMLElement</code> |  |
| [filePath] | <code>string</code> | Optional |

<a name="EquoMonacoEditor+isDirty"></a>

### equoMonacoEditor.isDirty() ⇒ <code>boolean</code>
Get if document state is dirty.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  
<a name="EquoMonacoEditor+clearDirtyState"></a>

### equoMonacoEditor.clearDirtyState() ⇒ <code>void</code>
Clean dirty state.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  
<a name="EquoMonacoEditor+setTextLabel"></a>

### equoMonacoEditor.setTextLabel(text) ⇒ <code>void</code>
Set message when state is dirty.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  

| Param | Type | Description |
| --- | --- | --- |
| text | <code>string</code> | Dirty status message. |

<a name="EquoMonacoEditor+setLabelChanges"></a>

### equoMonacoEditor.setLabelChanges(element) ⇒ <code>void</code>
Set DOM element for output message when state is dirty.

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  

| Param | Type | Description |
| --- | --- | --- |
| element | <code>HTMLElement</code> | Element for contain dirty status message |

<a name="EquoMonacoEditor+activateShortcuts"></a>

### equoMonacoEditor.activateShortcuts() ⇒ <code>void</code>
Activate shortcuts

**Kind**: instance method of [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)  
<a name="EquoMonaco"></a>

## EquoMonaco : <code>object</code>
The Equo Editor is a drop-in replacement for the Eclipse Generic Editor. based on Monaco web editor (same as VSCode).
It brings the beauty and the capabilities of a modern editor into Eclipse.

see [more](how-to-include-equo-components.md) about how to include Equo component in your projects.

**Kind**: global namespace  
<a name="create"></a>

## create(element, [filePath]) ⇒ [<code>EquoMonacoEditor</code>](#EquoMonacoEditor)
Create a new editor under DOM element.

**Kind**: global function  

| Param | Type | Description |
| --- | --- | --- |
| element | <code>HTMLElement</code> |  |
| [filePath] | <code>string</code> | Optional. |

<a name="addLspServer"></a>

## addLspServer(executionParameters, extensions) ⇒ <code>void</code>
Add a lsp server to be used by the editors on the files with the given extensions.

**Kind**: global function  

| Param | Type | Description |
| --- | --- | --- |
| executionParameters | <code>Array.&lt;string&gt;</code> | The parameters needed to start the lsp server through stdio. Example: ["html-languageserver", "--stdio"]. |
| extensions | <code>Array.&lt;string&gt;</code> | A collection of extensions for what the editor will use the given lsp server. The extensions must not have the initial dot. Example: ["php", "php4"]. |

<a name="removeLspServer"></a>

## removeLspServer(extensions) ⇒ <code>void</code>
Remove a lsp server assigned to the given extensions.

**Kind**: global function  

| Param | Type | Description |
| --- | --- | --- |
| extensions | <code>Array.&lt;string&gt;</code> | A collection of the file extensions for which the previously assigned lsp will be removed The extensions must not have the initial dot. Example: ["php", "php4"]. |

<a name="addLspWsServer"></a>

## addLspWsServer(fullServerPath, extensions) ⇒ <code>void</code>
Add a lsp websocket server to be used by the editors on the files with the given extensions.

**Kind**: global function  

| Param | Type | Description |
| --- | --- | --- |
| fullServerPath | <code>string</code> | The full path to the lsp server. Example: ws://127.0.0.1:3000/lspServer. |
| extensions | <code>Array.&lt;string&gt;</code> | A collection of extensions for what the editor will use the given lsp server. The extensions must not have the initial dot. Example: ["php", "php4"]. |

