#!/usr/bin/env node

import * as http from 'http';
import * as fs from 'fs';

import * as parseArgs from 'minimist';
import * as yaml from 'js-yaml';
import * as ws from 'ws';
import * as rpc from '@sourcegraph/vscode-ws-jsonrpc';
import * as rpcServer from '@sourcegraph/vscode-ws-jsonrpc/lib/server';

let argv = parseArgs(process.argv.slice(2));


let serverPort : number = parseInt(argv.port) || 3000;

const wss : ws.Server = new ws.Server({
  port: serverPort,
  perMessageDeflate: false
}, () => {
  //console.log(`Listening to http and ws requests on ${serverPort}`);
});

function toSocket(webSocket: ws): rpc.IWebSocket {
  return {
      send: content => webSocket.send(content),
      onMessage: cb => webSocket.onmessage = event => cb(event.data),
      onError: cb => webSocket.onerror = event => {
          if ('message' in event) {
              cb((event as any).message)
          }
      },
      onClose: cb => webSocket.onclose = event => cb(event.code, event.reason),
      dispose: () => webSocket.close()
  }
}

var myProcess = require('process');
wss.on('connection', (client : ws, request : http.IncomingMessage) => {
  //let localConnection = rpcServer.createProcessStreamConnection(myProcess);
  let localConnection = rpcServer.createStreamConnection(myProcess.stdin, myProcess.stdout, () => myProcess.kill());
  let socket : rpc.IWebSocket = toSocket(client);
  let connection = rpcServer.createWebSocketConnection(socket);
  rpcServer.forward(connection, localConnection);
  socket.onClose((code, reason) => {
    localConnection.dispose();
  });
});
