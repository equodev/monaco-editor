// @ts-ignore
import { EquoMonaco } from '@equo/equo-monaco-editor';
// @ts-ignore
import { EquoWebSocketService, EquoWebSocket } from '@equo/websocket';

var websocket: EquoWebSocket = EquoWebSocketService.get();

websocket.on('_getIsEditorCreated', () => {
    if (document.getElementsByClassName('monaco-editor').length > 0) {
        websocket.send('_doGetIsEditorCreated', {
            created: true
        });
    }
});

EquoMonaco.create(document.getElementById('container')!);