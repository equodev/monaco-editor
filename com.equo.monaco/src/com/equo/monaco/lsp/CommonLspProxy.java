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
