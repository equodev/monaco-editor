package com.make.equo.monaco.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class LspWsProxy extends WebSocketServer {
	private static final int ASCII_0 = 48;
	private static final int READING_HEADER_STATE = 0;
	private static final int READING_CONTENT_LENGTH_STATE = 1;
	private static final int SEARCHING_START_MESSAGE_STATE = 2;
	private static final int READING_MESSAGE_STATE = 3;
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
					case READING_HEADER_STATE:
						if ((char) readed == ' ') {
							state++;
						}
						break;
					case READING_CONTENT_LENGTH_STATE:
						if ((char) readed == '\r') {
							state++;
						} else {
							lengthMessage = (lengthMessage * 10) + readed - ASCII_0;
						}
						break;
					case SEARCHING_START_MESSAGE_STATE:
						if ((char) readed == '{') {
							builder = new StringBuilder();
							builder.append((char) readed);
							state++;
							lengthReaded++;
						}
						break;
					case READING_MESSAGE_STATE:
						builder.append((char) readed);
						if (++lengthReaded == lengthMessage) {
							broadcast(builder.toString());
							lengthReaded = 0;
							lengthMessage = 0;
							state = READING_HEADER_STATE;
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
