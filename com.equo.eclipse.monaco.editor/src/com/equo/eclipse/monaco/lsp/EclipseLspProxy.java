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

package com.equo.eclipse.monaco.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4e.LaunchConfigurationStreamProvider;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.equo.monaco.lsp.LspProxy;

/**
 * Proxy between a language server process runned by Eclipse and the editor.
 */
public class EclipseLspProxy extends LspProxy {

  private StreamConnectionProvider streamConnectionProvider;
  private Process process;

  protected static final Logger logger = LoggerFactory.getLogger(EclipseLspProxy.class);

  /**
   * Parameterized constructor.
   */
  public EclipseLspProxy(LanguageServerWrapper lspServer) {
    try {
      Field field = lspServer.getClass().getDeclaredField("lspStreamProvider");
      field.setAccessible(true);
      this.streamConnectionProvider = (StreamConnectionProvider) field.get(lspServer);
    } catch (Exception e) {
      logger.error("Error obtaining LSP stream provider", e);
    }
  }

  private Process getProcessFromStreamConnectionProvider() {
    Field field;
    try {
      if (streamConnectionProvider instanceof ProcessStreamConnectionProvider) {
        field = ProcessStreamConnectionProvider.class.getDeclaredField("process");
      } else if (streamConnectionProvider instanceof LaunchConfigurationStreamProvider) {
        field = LaunchConfigurationStreamProvider.class.getDeclaredField("process");
        field.setAccessible(true);
        IProcess result = (IProcess) field.get(streamConnectionProvider);
        if (!(result instanceof RuntimeProcess)) {
          return null;
        }
        Method systemProcessGetter = RuntimeProcess.class.getDeclaredMethod("getSystemProcess");
        systemProcessGetter.setAccessible(true);
        return (Process) systemProcessGetter.invoke(result);
      } else {
        return null;
      }
      field.setAccessible(true);
      return (Process) field.get(streamConnectionProvider);
    } catch (Exception e) {
      logger.error("Error obtaining language server process", e);
    }
    return null;
  }

  @Override
  public void startServer() {
    if (streamConnectionProvider == null) {
      return;
    }
    try {
      synchronized (streamConnectionProvider) {
        streamConnectionProvider.start();
        this.process = getProcessFromStreamConnectionProvider();

        InputStream streamIn = streamConnectionProvider.getInputStream();
        OutputStream streamOut = streamConnectionProvider.getOutputStream();
        startProxy(streamIn, streamOut);
      }
    } catch (IOException e) {
      logger.error("Error starting language server", e);
    }
  }

  @Override
  public void stopServer() {
    super.stopServer();
    synchronized (streamConnectionProvider) {
      if (streamConnectionProvider != null) {
        if (this.process == getProcessFromStreamConnectionProvider()) {
          streamConnectionProvider.stop();
        } else if (this.process != null) {
          process.destroy();
        }
      }
    }
  }

}
