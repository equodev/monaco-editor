package com.make.equo.monaco.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class LspWsProxy extends WebSocketServer {
	private OutputStream streamOut;
	private Thread redirectThread = null;

	public LspWsProxy(int port, InputStream streamIn, OutputStream streamOut) {
		super(new InetSocketAddress(port));
		this.streamOut = streamOut;
		redirectThread = new Thread(() -> {
			int state = 0;
			int lengthMessage = 0;
			StringBuilder builder = null;
			int lengthReaded = 0;
			try {
				while (true) {
					// Block of read message
					int readed = streamIn.read();
					switch (state) {
					case 0:
						if ((char) readed == ' ') {
							state++;
						}
						break;
					case 1:
						if ((char) readed == '\r') {
							state++;
						}else {
							lengthMessage = (lengthMessage * 10) + readed - 48;
						}
						break;
					case 2:
						if ((char) readed == '{') {
							builder = new StringBuilder();
							builder.append((char) readed);
							state++;
							lengthReaded++;
						}
						break;
					case 3:
						builder.append((char) readed);
						if (++lengthReaded == lengthMessage) {
							broadcast(builder.toString());
							lengthReaded = 0;
							lengthMessage = 0;
							state = 0;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		redirectThread.start();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void stop() throws IOException, InterruptedException {
		if (redirectThread != null)
			redirectThread.stop();
		super.stop();
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		new Thread(() -> {
			try {
				synchronized (streamOut) {
					String fullMessage = "Content-Length: " + message.getBytes().length + "\r\n\r\n" + message;
					byte[] bytes = fullMessage.getBytes();
					streamOut.write(bytes);
					streamOut.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub

	}

}
