package com.equo.monaco.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * This class is in charge for the communication Proxy <-> Editor, which is done
 * with websockets.
 */
public class LspWsProxy extends WebSocketServer {
  private static final String CONTENT_LENGTH = "Content-Length: ";
  private static final int SIZE_STRING_CONTENT_LENGTH = CONTENT_LENGTH.length();
  private static final int ASCII_0 = 48;
  private static final int READING_HEADER_STATE = 0;
  private static final int READING_CONTENT_LENGTH_STATE = 1;
  private static final int SEARCHING_START_MESSAGE_STATE = 2;
  private static final int READING_MESSAGE_STATE = 3;
  private OutputStream streamOut;
  private Thread redirectThread = null;
  private ExecutorService executorService = Executors.newCachedThreadPool();
  private final AtomicBoolean finish = new AtomicBoolean(false);

  /**
   * Parameterized constructor.
   */
  public LspWsProxy(int port, InputStream streamIn, OutputStream streamOut) {
    super(new InetSocketAddress(port));
    this.streamOut = streamOut;
    redirectThread = new Thread(() -> {
      int state = 0;
      int lengthMessage = 0;
      StringBuilder builderMessage = null;
      StringBuilder builderHeader = new StringBuilder();
      int lengthReaded = 0;
      try {
        while (true) {
          // Block of read message
          int readed = streamIn.read();
          if (finish.get()) {
            return;
          }
          switch (state) {
            case READING_HEADER_STATE:
              builderHeader.append((char) readed);
              final int lastIndexOf = builderHeader.lastIndexOf(CONTENT_LENGTH);
              if (lastIndexOf >= 0
                  && lastIndexOf == builderHeader.length() - SIZE_STRING_CONTENT_LENGTH) {
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
                builderMessage = new StringBuilder(lengthMessage);
                builderMessage.append((char) readed);
                state++;
                lengthReaded++;
              }
              break;
            case READING_MESSAGE_STATE:
              builderMessage.append((char) readed);
              if (++lengthReaded == lengthMessage) {
                broadcast(builderMessage.toString());
                lengthReaded = 0;
                lengthMessage = 0;
                builderHeader = new StringBuilder();
                state = READING_HEADER_STATE;
              }
              break;
            default:
              break;
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }, "Lsp -> Equo Editor");
    redirectThread.start();
  }

  @Override
  public void stop() throws IOException, InterruptedException {
    finish.set(true);
    super.stop();
    executorService.shutdown();
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
    executorService.submit(() -> {
      String fullMessage = CONTENT_LENGTH + message.getBytes().length + "\r\n\r\n" + message;
      final byte[] bytes = fullMessage.getBytes();
      try {
        synchronized (streamOut) {
          streamOut.write(bytes);
          streamOut.flush();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
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
