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
import java.util.List;

/**
 * Lsp Proxy that runs a program with the given parameters and communicates the
 * editor with it.
 */
public class CommonLspProxy extends LspProxy {
  private List<String> program;
  private Process process;

  public CommonLspProxy(List<String> programArguments) {
    super();
    program = programArguments;
  }

  @Override
  public synchronized void startServer() {
    ProcessBuilder builder = new ProcessBuilder(program);
    try {
      process = builder.start();
      startProxy(process.getInputStream(), process.getOutputStream());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public synchronized void stopServer() {
    super.stopServer();
    process.destroy();
  }

}
