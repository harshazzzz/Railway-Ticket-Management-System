package com.railway;

import com.railway.database.*;
import com.railway.repository.JdbcRepository;
import com.railway.service.RailService;
import com.railway.utils.AppException;
import com.railway.view.*;
import java.util.*;
import javax.swing.*;

public final class App {
  private App() {}

  public static void main(String[] args) {
    List<String> flags = Arrays.asList(args);
    boolean demo = flags.contains("--demo");
    try {
      Database db = Database.configured(demo);
      if (demo) db.initialize();
      RailService service = new RailService(new JdbcRepository(db));
      if (flags.contains("--bootstrap-admin")) {
        var console = System.console();
        if (console == null)
          throw new AppException(
              "Run --bootstrap-admin from a terminal with an interactive console.");
        String name = console.readLine("Admin name: "), email = console.readLine("Admin email: ");
        service.bootstrapAdmin(
            name, email, console.readPassword("Admin password (10+ characters): "));
        System.out.println("Administrator created. Start the application normally to sign in.");
        return;
      }
      if (demo) DemoData.seed(db, service);
      SwingUtilities.invokeLater(
          () -> {
            Theme.install();
            new MainFrame(service, demo).setVisible(true);
          });
    } catch (AppException e) {
      System.err.println(e.getMessage());
      if (!java.awt.GraphicsEnvironment.isHeadless())
        JOptionPane.showMessageDialog(
            null, e.getMessage(), "Railway startup", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
  }
}
