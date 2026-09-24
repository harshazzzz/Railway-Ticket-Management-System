package com.railway.controller;

import com.railway.utils.AppException;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.*;
import javax.swing.*;

/** Executes all JDBC/password work off the Swing event-dispatch thread. */
public final class AppController {
  private final Component parent;

  public AppController(Component parent) {
    this.parent = parent;
  }

  public <T> void execute(Callable<T> action, Consumer<T> success, Runnable finished) {
    new SwingWorker<T, Void>() {
      @Override
      protected T doInBackground() throws Exception {
        return action.call();
      }

      @Override
      protected void done() {
        try {
          success.accept(get());
        } catch (Exception e) {
          Throwable cause = e.getCause() == null ? e : e.getCause();
          String message =
              cause instanceof AppException
                  ? cause.getMessage()
                  : "The operation could not be completed. Please retry.";
          if (!(cause instanceof AppException))
            Logger.getLogger(AppController.class.getName())
                .log(Level.WARNING, "Unexpected application error", cause);
          JOptionPane.showMessageDialog(
              parent, message, "Action needed", JOptionPane.WARNING_MESSAGE);
        } finally {
          finished.run();
        }
      }
    }.execute();
  }
}
