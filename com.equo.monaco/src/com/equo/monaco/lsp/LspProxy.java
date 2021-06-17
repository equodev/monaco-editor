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

package com.equo.monaco.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;

/**
 * A Lsp Proxy will communicate the editor with some language server.
 */
public abstract class LspProxy {
  private int proxyPort;
  private ServerSocket socketPortReserve;
  protected LspWsProxy proxy;

  public LspProxy() {
    proxyPort = reservePortForProxy(0);
  }

  public abstract void startServer();

  /**
   * Stops the proxy.
   */
  public void stopServer() {
    if (proxy != null) {
      try {
        proxy.stop();
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  protected void startProxy(InputStream streamIn, OutputStream streamOut) {
    closeSocketPortReserve();
    proxy = new LspWsProxy(getPort(), streamIn, streamOut);
    proxy.start();
  }

  public int getPort() {
    return proxyPort;
  }

  protected void closeSocketPortReserve() {
    try {
      socketPortReserve.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Reserves a port to be used later by the ws proxy. It mantains a socket listen
   * on that port until the proxy is started
   * @param  port The port to be reserved. Use 0 to reserve a random port
   * @return      the port reserved
   */
  protected int reservePortForProxy(int port) {
    try {
      socketPortReserve = new ServerSocket(port);
      socketPortReserve.setReuseAddress(true);
      return socketPortReserve.getLocalPort();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return (int) (Math.random() * 10000 + 40000);
  }

}
