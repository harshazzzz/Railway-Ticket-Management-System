package com.railway;

import com.railway.database.*;
import com.railway.repository.JdbcRepository;
import com.railway.service.*;
import com.railway.view.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Developer utility: paints actual Swing views, never fabricated UI mockups. */
public final class ScreenshotCapture {
  private static MainFrame frame;

  private static void idle() throws Exception {
    for (int n = 0; n < 100; n++) {
      Thread.sleep(100);
      AtomicBoolean busy = new AtomicBoolean();
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              var field = MainFrame.class.getDeclaredField("busy");
              field.setAccessible(true);
              busy.set(field.getBoolean(frame));
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
      if (!busy.get()) return;
    }
    throw new IllegalStateException("UI action did not finish");
  }

  private static void capture(Path path) throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          try {
            frame.validate();
            BufferedImage image =
                new BufferedImage(
                    frame.getContentPane().getWidth(),
                    frame.getContentPane().getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            frame.getContentPane().printAll(g);
            g.dispose();
            ImageIO.write(image, "png", path.toFile());
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  public static void main(String[] args) throws Exception {
    Path out = Path.of(args.length == 0 ? "docs/screenshots" : args[0]);
    Files.createDirectories(out);
    Database database =
        new Database(
            "jdbc:h2:mem:screenshots;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    database.initialize();
    RailService service = new RailService(new JdbcRepository(database));
    DemoData.seed(database, service);
    Session passenger = service.login("passenger@railway.demo", "Journey123!".toCharArray()),
        admin = service.login("admin@railway.demo", "Railway123!".toCharArray());
    SwingUtilities.invokeAndWait(
        () -> {
          Theme.install();
          frame = new MainFrame(service, true);
          frame.addNotify();
          frame.validate();
        });
    capture(out.resolve("login.png"));
    SwingUtilities.invokeAndWait(() -> frame.dashboard(passenger));
    idle();
    capture(out.resolve("passenger-search.png"));
    SwingUtilities.invokeAndWait(() -> frame.bookingPage());
    idle();
    capture(out.resolve("bookings.png"));
    SwingUtilities.invokeAndWait(() -> frame.dashboard(admin));
    idle();
    capture(out.resolve("admin-reports.png"));
    SwingUtilities.invokeAndWait(() -> frame.trainPage());
    idle();
    capture(out.resolve("admin-trains.png"));
    SwingUtilities.invokeAndWait(() -> frame.userPage());
    idle();
    capture(out.resolve("admin-users.png"));
    SwingUtilities.invokeAndWait(() -> frame.dispose());
    System.out.println("Rendered six actual Swing screens to " + out.toAbsolutePath());
    System.exit(0);
  }
}
