package com.railway.view;

import com.railway.controller.AppController;
import com.railway.model.*;
import com.railway.service.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** Swing view: forms and rendering only. Domain rules are enforced by RailService. */
public final class MainFrame extends JFrame {
  private final RailService service;
  private final boolean demo;
  private final AppController controller = new AppController(this);
  private Session session;
  private JPanel content;
  private final JLabel status = Theme.label("Ready", 12, Theme.MUTED);
  private int generation;
  private boolean busy;
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

  public MainFrame(RailService service, boolean demo) {
    super("Railway | Train Ticket Reservation System");
    this.service = service;
    this.demo = demo;
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(1000, 700));
    setSize(1220, 800);
    setLocationRelativeTo(null);
    showLogin();
  }

  private <T> void run(Callable<T> action, Consumer<T> success) {
    if (busy) return;
    busy = true;
    int page = generation;
    status.setText("Working...");
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    controller.execute(
        action,
        result -> {
          if (page == generation) success.accept(result);
        },
        () -> {
          busy = false;
          status.setText("Ready");
          setCursor(Cursor.getDefaultCursor());
        });
  }

  private void mount(JPanel root) {
    generation++;
    setContentPane(root);
    revalidate();
    repaint();
  }

  public void showLogin() {
    JPanel root = new JPanel(new GridLayout(1, 2));
    JPanel brand = new JPanel(new BorderLayout());
    brand.setBackground(Theme.NAVY);
    brand.setBorder(new EmptyBorder(52, 48, 42, 42));
    brand.add(
        Theme.label("RAILWAY  /  RESERVATIONS", 18, new Color(106, 216, 198)), BorderLayout.NORTH);
    JPanel hero = Theme.panel(new GridLayout(0, 1, 0, 20));
    hero.add(Theme.label("A better journey", 40, Color.WHITE));
    hero.add(Theme.label("starts here.", 40, Color.WHITE));
    hero.add(Theme.label("Find your train. Choose your seat.", 17, new Color(193, 209, 224)));
    hero.add(Theme.label("Keep every ticket in one place.", 17, new Color(193, 209, 224)));
    hero.add(
        Theme.label("01  Search     02  Reserve     03  Travel", 14, new Color(106, 216, 198)));
    JPanel center = Theme.panel(new GridBagLayout());
    center.add(hero);
    brand.add(center, BorderLayout.CENTER);
    brand.add(
        Theme.label("JAVA DESKTOP  /  COURSEWORK EDITION", 12, new Color(164, 184, 203)),
        BorderLayout.SOUTH);
    JPanel right = new JPanel(new GridBagLayout());
    right.setBorder(new EmptyBorder(42, 48, 42, 48));
    JPanel form = Theme.panel(new GridBagLayout());
    GridBagConstraints g = constraints();
    addRow(form, g, Theme.label("Welcome back", 30, Theme.NAVY));
    addRow(form, g, Theme.label("Sign in to plan your next journey.", 14, Theme.MUTED));
    JTextField email = new JTextField(25);
    JPasswordField password = new JPasswordField(25);
    field(form, g, "Email address", email);
    field(form, g, "Password", password);
    JButton login = Theme.button("Sign in", true),
        register = Theme.button("Create passenger account", false);
    addRow(form, g, login);
    addRow(form, g, register);
    addRow(
        form,
        g,
        Theme.label(
            demo ? "DEMO MODE  /  local sample data" : "MYSQL MODE  /  configured database",
            12,
            Theme.TEAL));
    if (demo) {
      addRow(form, g, Theme.label("passenger@railway.demo  /  Journey123!", 12, Theme.MUTED));
      addRow(form, g, Theme.label("admin@railway.demo  /  Railway123!", 12, Theme.MUTED));
    }
    addRow(form, g, status);
    right.add(form);
    root.add(brand);
    root.add(right);
    login.addActionListener(
        e -> {
          String mail = email.getText();
          char[] secret = password.getPassword();
          password.setText("");
          run(() -> service.login(mail, secret), this::dashboard);
        });
    register.addActionListener(e -> registration());
    getRootPane().setDefaultButton(login);
    mount(root);
  }

  private void registration() {
    JTextField name = new JTextField(24), email = new JTextField(24);
    JPasswordField pass = new JPasswordField(24);
    JPanel form = form("Full name", name, "Email", email, "Password (10+ characters)", pass);
    if (JOptionPane.showConfirmDialog(
            this,
            form,
            "Create passenger account",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE)
        == JOptionPane.OK_OPTION) {
      String n = name.getText(), m = email.getText();
      char[] p = pass.getPassword();
      pass.setText("");
      run(
          () -> service.register(n, m, p),
          id -> JOptionPane.showMessageDialog(this, "Account created. You can now sign in."));
    }
  }

  public void dashboard(Session session) {
    this.session = session;
    JPanel root = new JPanel(new BorderLayout());
    JPanel sidebar = new JPanel(new BorderLayout(0, 30));
    sidebar.setBackground(Theme.NAVY);
    sidebar.setPreferredSize(new Dimension(224, 0));
    sidebar.setBorder(new EmptyBorder(32, 20, 24, 20));
    sidebar.add(Theme.label("RAILWAY", 26, Color.WHITE), BorderLayout.NORTH);
    JPanel nav = Theme.panel(new GridLayout(0, 1, 0, 12));
    if (session.user() instanceof Admin) {
      nav(nav, "Overview & reports", this::reports);
      nav(nav, "Train management", this::trainPage);
      nav(nav, "User management", this::userPage);
      nav(nav, "All bookings", this::bookingPage);
    } else {
      nav(nav, "Find a journey", this::searchPage);
      nav(nav, "My bookings", this::bookingPage);
    }
    JPanel navTop = Theme.panel(new BorderLayout());
    navTop.add(nav, BorderLayout.NORTH);
    sidebar.add(navTop, BorderLayout.CENTER);
    JButton logout = Theme.button("Sign out", false);
    logout.addActionListener(
        e -> {
          service.logout(this.session);
          this.session = null;
          showLogin();
        });
    sidebar.add(logout, BorderLayout.SOUTH);
    JPanel main = new JPanel(new BorderLayout(0, 24));
    main.setBorder(new EmptyBorder(28, 32, 20, 32));
    JPanel header = Theme.panel(new BorderLayout());
    JPanel titles = Theme.panel(new GridLayout(2, 1, 0, 4));
    titles.add(Theme.label(session.user().dashboardTitle(), 28, Theme.NAVY));
    titles.add(
        Theme.label(
            "Welcome, " + session.user().getName() + "  /  " + session.user().role().toLowerCase(),
            14,
            Theme.MUTED));
    header.add(titles, BorderLayout.CENTER);
    header.add(
        Theme.label(demo ? "DEMO  /  LKR" : "MYSQL  /  LKR", 12, Theme.TEAL), BorderLayout.EAST);
    main.add(header, BorderLayout.NORTH);
    content = Theme.panel(new BorderLayout(16, 18));
    main.add(content, BorderLayout.CENTER);
    JPanel foot = Theme.panel(new BorderLayout());
    foot.add(status, BorderLayout.WEST);
    foot.add(
        Theme.label("Payments are simulated. No money is transferred.", 12, Theme.MUTED),
        BorderLayout.EAST);
    main.add(foot, BorderLayout.SOUTH);
    root.add(sidebar, BorderLayout.WEST);
    root.add(main, BorderLayout.CENTER);
    mount(root);
    // Called as an async completion: defer page load until the current worker has finished.
    SwingUtilities.invokeLater(
        () -> {
          if (this.session != null) {
            if (this.session.user() instanceof Admin) reports();
            else searchPage();
          }
        });
  }

  private void nav(JPanel nav, String title, Runnable action) {
    JButton b = Theme.button(title, false);
    b.setHorizontalAlignment(SwingConstants.LEFT);
    b.addActionListener(
        e -> {
          if (!busy) action.run();
        });
    nav.add(b);
  }

  private void page(String title, String subtitle) {
    generation++;
    content.removeAll();
    JPanel heading = Theme.panel(new GridLayout(2, 1, 0, 5));
    heading.add(Theme.label(title, 23, Theme.NAVY));
    heading.add(Theme.label(subtitle, 13, Theme.MUTED));
    content.add(heading, BorderLayout.NORTH);
  }

  private void refresh() {
    content.revalidate();
    content.repaint();
  }

  public void searchPage() {
    page("Find a journey", "Search stations and a date, then choose your seat.");
    JPanel body = Theme.panel(new BorderLayout(0, 18));
    JPanel filters = Theme.card();
    JTextField origin = new JTextField(10),
        destination = new JTextField(10),
        date = new JTextField(LocalDate.now().plusDays(1).toString(), 10);
    JPanel inputs = Theme.panel(new GridLayout(1, 3, 14, 0));
    inputs.add(form("From (blank = all)", origin));
    inputs.add(form("To (blank = all)", destination));
    inputs.add(form("Date (yyyy-MM-dd)", date));
    filters.add(inputs, BorderLayout.CENTER);
    JButton search = Theme.button("Search trains", true);
    filters.add(search, BorderLayout.EAST);
    body.add(filters, BorderLayout.NORTH);
    JPanel results = Theme.panel(new BorderLayout(0, 12));
    body.add(results, BorderLayout.CENTER);
    content.add(body, BorderLayout.CENTER);
    Runnable load =
        () -> {
          LocalDate day;
          try {
            day = LocalDate.parse(date.getText().strip());
          } catch (Exception e) {
            warn("Enter a date as yyyy-MM-dd.");
            return;
          }
          String o = origin.getText(), d = destination.getText();
          run(
              () -> service.search(session, o, d, day),
              schedules -> {
                results.removeAll();
                JTable table =
                    table(
                        new String[] {
                          "Train", "From", "To", "Departure", "Arrival", "Seats", "Fare (LKR)"
                        },
                        schedules.stream()
                            .map(
                                s ->
                                    new Object[] {
                                      s.train().name(),
                                      s.train().origin(),
                                      s.train().destination(),
                                      s.departure().format(TIME),
                                      s.arrival().toLocalTime(),
                                      s.availableSeats() + " / " + s.capacity(),
                                      s.fare()
                                    })
                            .toArray(Object[][]::new));
                results.add(new JScrollPane(table), BorderLayout.CENTER);
                JPanel actions = Theme.panel(new BorderLayout());
                actions.add(
                    Theme.label(
                        schedules.isEmpty()
                            ? "No journeys found. Try another date or station."
                            : schedules.size() + " journeys available",
                        13,
                        Theme.MUTED),
                    BorderLayout.WEST);
                JButton reserve = Theme.button("Choose seat & book", true);
                reserve.setEnabled(!schedules.isEmpty());
                actions.add(reserve, BorderLayout.EAST);
                results.add(actions, BorderLayout.SOUTH);
                reserve.addActionListener(
                    e -> {
                      int row = selected(table);
                      if (row >= 0) chooseSeat(schedules.get(row));
                    });
                refresh();
              });
        };
    search.addActionListener(e -> load.run());
    refresh();
    load.run();
  }

  private void chooseSeat(Schedule schedule) {
    run(
        () -> service.seats(session, schedule.id()),
        seats -> {
          JDialog dialog = new JDialog(this, "Choose your seat", true);
          dialog.setSize(620, 650);
          dialog.setLocationRelativeTo(this);
          JPanel root = new JPanel(new BorderLayout(16, 16));
          root.setBorder(new EmptyBorder(24, 24, 24, 24));
          JPanel heading = Theme.panel(new GridLayout(0, 1, 0, 8));
          heading.add(
              Theme.label(
                  schedule.train().name() + "  /  " + schedule.fare() + " LKR", 23, Theme.NAVY));
          heading.add(
              Theme.label(
                  schedule.train().origin() + " to " + schedule.train().destination(),
                  15,
                  Theme.MUTED));
          heading.add(Theme.label("Select a seat. Grey seats are unavailable.", 13, Theme.MUTED));
          root.add(heading, BorderLayout.NORTH);
          JPanel grid = Theme.panel(new GridLayout(0, 4, 10, 10));
          ButtonGroup group = new ButtonGroup();
          int[] chosen = {0};
          for (Seat seat : seats) {
            JToggleButton b = new JToggleButton(String.format("%02d", seat.number()));
            b.setPreferredSize(new Dimension(85, 44));
            b.setEnabled(seat.available());
            if (seat.available()) b.setBackground(new Color(226, 245, 240));
            group.add(b);
            grid.add(b);
            b.addActionListener(e -> chosen[0] = seat.number());
          }
          JPanel topGrid = Theme.panel(new BorderLayout());
          topGrid.add(grid, BorderLayout.NORTH);
          root.add(new JScrollPane(topGrid), BorderLayout.CENTER);
          JComboBox<String> method =
              new JComboBox<>(
                  new String[] {
                    "Card (simulated approval)",
                    "Cash (simulated receipt)",
                    "Card (simulate decline)"
                  });
          JButton book = Theme.button("Confirm & pay " + schedule.fare() + " LKR", true);
          JPanel bottom = Theme.panel(new BorderLayout(0, 10));
          bottom.add(form("Payment simulation - no real charge", method), BorderLayout.CENTER);
          bottom.add(book, BorderLayout.SOUTH);
          root.add(bottom, BorderLayout.SOUTH);
          book.addActionListener(
              e -> {
                if (chosen[0] == 0) {
                  warn("Choose an available seat.");
                  return;
                }
                Payment payment =
                    switch (method.getSelectedIndex()) {
                      case 1 -> new CashPayment();
                      case 2 -> new CardPayment(false);
                      default -> new CardPayment(true);
                    };
                dialog.dispose();
                SwingUtilities.invokeLater(
                    () ->
                        run(
                            () -> service.book(session, schedule.id(), chosen[0], payment),
                            id -> {
                              JOptionPane.showMessageDialog(
                                  this,
                                  "Booking #" + id + " confirmed. Your ticket is in My bookings.");
                              SwingUtilities.invokeLater(this::bookingPage);
                            }));
              });
          dialog.setContentPane(root);
          SwingUtilities.invokeLater(() -> dialog.setVisible(true));
        });
  }

  public void bookingPage() {
    page(
        session.user() instanceof Admin ? "All bookings" : "My bookings",
        "Your journey history, tickets and cancellation status.");
    run(
        () -> service.bookings(session),
        rows -> {
          JTable table =
              table(
                  new String[] {
                    "Booking",
                    "Passenger",
                    "Train",
                    "Departure",
                    "Seat",
                    "Fare (LKR)",
                    "Status",
                    "Payment"
                  },
                  rows.stream()
                      .map(
                          b ->
                              new Object[] {
                                b.id(),
                                b.passenger(),
                                b.train(),
                                b.departure().format(TIME),
                                b.seatNumber(),
                                b.price(),
                                b.status(),
                                b.paymentStatus()
                              })
                      .toArray(Object[][]::new));
          content.add(new JScrollPane(table), BorderLayout.CENTER);
          JPanel actions = Theme.panel(new FlowLayout(FlowLayout.RIGHT));
          JButton ticket = Theme.button("View ticket", false),
              cancel = Theme.button("Cancel & refund", false);
          actions.add(ticket);
          actions.add(cancel);
          content.add(actions, BorderLayout.SOUTH);
          ticket.addActionListener(
              e -> {
                int i = selected(table);
                if (i < 0) return;
                Booking b = rows.get(i);
                run(
                    () -> service.ticket(session, b.id()),
                    t -> {
                      JTextArea text =
                          new JTextArea(
                              "RAILWAY  /  E-TICKET\n\n"
                                  + b.train()
                                  + "\n"
                                  + b.origin()
                                  + " -> "
                                  + b.destination()
                                  + "\n"
                                  + b.departure().format(TIME)
                                  + "\n\nPassenger: "
                                  + b.passenger()
                                  + "\nSeat: "
                                  + t.seatNumber()
                                  + "\nFare: LKR "
                                  + t.price()
                                  + "\nStatus: "
                                  + b.status()
                                  + "\n\nReference: "
                                  + t.reference()
                                  + "\n\nCoursework simulation; not valid for travel.");
                      text.setEditable(false);
                      text.setFont(new Font("Monospaced", Font.PLAIN, 14));
                      text.setBorder(new EmptyBorder(18, 18, 18, 18));
                      JOptionPane.showMessageDialog(
                          this, text, "Ticket #" + b.id(), JOptionPane.PLAIN_MESSAGE);
                    });
              });
          cancel.addActionListener(
              e -> {
                int i = selected(table);
                if (i < 0) return;
                Booking b = rows.get(i);
                if (JOptionPane.showConfirmDialog(
                        this,
                        "Cancel booking #" + b.id() + " and simulate a full refund?",
                        "Confirm cancellation",
                        JOptionPane.YES_NO_OPTION)
                    == JOptionPane.YES_OPTION)
                  run(
                      () -> {
                        service.cancel(session, b.id());
                        return true;
                      },
                      ok -> SwingUtilities.invokeLater(this::bookingPage));
              });
          refresh();
        });
    refresh();
  }

  public void trainPage() {
    page("Train management", "Manage the fleet and publish dated departures.");
    run(
        () -> service.trains(session),
        rows -> {
          JTable table =
              table(
                  new String[] {"ID", "Train", "From", "To", "Capacity", "Active"},
                  rows.stream()
                      .map(
                          t ->
                              new Object[] {
                                t.id(),
                                t.name(),
                                t.origin(),
                                t.destination(),
                                t.capacity(),
                                t.active()
                              })
                      .toArray(Object[][]::new));
          content.add(new JScrollPane(table), BorderLayout.CENTER);
          JPanel actions = Theme.panel(new FlowLayout(FlowLayout.RIGHT));
          JButton add = Theme.button("Add train", true),
              edit = Theme.button("Edit", false),
              schedule = Theme.button("Add departure", false),
              delete = Theme.button("Retire train", false);
          for (JButton b : new JButton[] {add, edit, schedule, delete}) actions.add(b);
          content.add(actions, BorderLayout.SOUTH);
          add.addActionListener(e -> trainForm(null));
          edit.addActionListener(
              e -> {
                int i = selected(table);
                if (i >= 0) trainForm(rows.get(i));
              });
          schedule.addActionListener(
              e -> {
                int i = selected(table);
                if (i >= 0) scheduleForm(rows.get(i));
              });
          delete.addActionListener(
              e -> {
                int i = selected(table);
                if (i >= 0
                    && JOptionPane.showConfirmDialog(
                            this,
                            "Retire " + rows.get(i).name() + "? History will remain available.",
                            "Confirm retirement",
                            JOptionPane.YES_NO_OPTION)
                        == JOptionPane.YES_OPTION)
                  run(
                      () -> {
                        service.deleteTrain(session, rows.get(i).id());
                        return true;
                      },
                      ok -> SwingUtilities.invokeLater(this::trainPage));
              });
          refresh();
        });
    refresh();
  }

  private void trainForm(Train train) {
    JTextField name = new JTextField(train == null ? "" : train.name(), 22),
        origin = new JTextField(train == null ? "" : train.origin()),
        destination = new JTextField(train == null ? "" : train.destination());
    JSpinner capacity =
        new JSpinner(new SpinnerNumberModel(train == null ? 80 : train.capacity(), 1, 500, 1));
    if (JOptionPane.showConfirmDialog(
            this,
            form(
                "Train name",
                name,
                "Origin",
                origin,
                "Destination",
                destination,
                "Capacity",
                capacity),
            train == null ? "Add train" : "Edit train",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE)
        == JOptionPane.OK_OPTION) {
      String n = name.getText(), o = origin.getText(), d = destination.getText();
      int cap = (int) capacity.getValue();
      run(
          () -> service.saveTrain(session, train == null ? 0 : train.id(), n, o, d, cap),
          id -> SwingUtilities.invokeLater(this::trainPage));
    }
  }

  private void scheduleForm(Train train) {
    String day = LocalDate.now().plusDays(1).toString();
    JTextField departure = new JTextField(day + "T08:00", 22),
        arrival = new JTextField(day + "T11:00"),
        fare = new JTextField("1500.00");
    if (JOptionPane.showConfirmDialog(
            this,
            form(
                "Departure (yyyy-MM-ddTHH:mm)",
                departure,
                "Arrival (yyyy-MM-ddTHH:mm)",
                arrival,
                "Fare (LKR)",
                fare),
            "Publish departure: " + train.name(),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE)
        == JOptionPane.OK_OPTION) {
      try {
        LocalDateTime d = LocalDateTime.parse(departure.getText().strip()),
            a = LocalDateTime.parse(arrival.getText().strip());
        BigDecimal f = new BigDecimal(fare.getText().strip());
        run(
            () -> service.addSchedule(session, train.id(), d, a, f),
            id -> JOptionPane.showMessageDialog(this, "Departure #" + id + " published."));
      } catch (Exception e) {
        warn("Check date/time format and enter a numeric fare.");
      }
    }
  }

  public void userPage() {
    page("User management", "Create passenger accounts, edit profiles and deactivate access.");
    run(
        () -> service.users(session),
        rows -> {
          JTable table =
              table(
                  new String[] {"ID", "Name", "Email", "Role", "Active"},
                  rows.stream()
                      .map(
                          u ->
                              new Object[] {
                                u.getId(), u.getName(), u.getEmail(), u.role(), u.isActive()
                              })
                      .toArray(Object[][]::new));
          content.add(new JScrollPane(table), BorderLayout.CENTER);
          JPanel actions = Theme.panel(new FlowLayout(FlowLayout.RIGHT));
          JButton add = Theme.button("Add passenger", true),
              edit = Theme.button("Edit / deactivate", false);
          actions.add(add);
          actions.add(edit);
          content.add(actions, BorderLayout.SOUTH);
          add.addActionListener(e -> userForm(null));
          edit.addActionListener(
              e -> {
                int i = selected(table);
                if (i >= 0) userForm(rows.get(i));
              });
          refresh();
        });
    refresh();
  }

  private void userForm(User user) {
    JTextField name = new JTextField(user == null ? "" : user.getName(), 24),
        email = new JTextField(user == null ? "" : user.getEmail());
    JPasswordField password = new JPasswordField();
    JCheckBox active = new JCheckBox("Account active", user == null || user.isActive());
    JPanel fields =
        user == null
            ? form("Name", name, "Email", email, "Initial password (10+ characters)", password)
            : form("Name", name, "Email", email, "Access", active);
    if (JOptionPane.showConfirmDialog(
            this,
            fields,
            user == null ? "Add passenger" : "Edit user",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE)
        == JOptionPane.OK_OPTION) {
      String n = name.getText(), e = email.getText();
      boolean a = active.isSelected();
      char[] p = password.getPassword();
      password.setText("");
      run(
          () -> {
            if (user == null) return service.addPassenger(session, n, e, p);
            service.updateUser(session, user.getId(), n, e, a);
            return user.getId();
          },
          id -> SwingUtilities.invokeLater(this::userPage));
    }
  }

  public void reports() {
    page(
        "Overview & reports", "Booking totals and simulated payment activity across all journeys.");
    run(
        () -> new ReportData(service.bookings(session), service.transactions(session)),
        data -> {
          JPanel body = Theme.panel(new BorderLayout(0, 22));
          JPanel stats = Theme.panel(new GridLayout(1, 3, 16, 0));
          long active = data.bookings.stream().filter(b -> b.status().equals("CONFIRMED")).count();
          BigDecimal revenue =
              data.transactions.stream()
                  .map(t -> t.type().equals("REFUND") ? t.amount().negate() : t.amount())
                  .reduce(BigDecimal.ZERO, BigDecimal::add);
          stats.add(stat("TOTAL BOOKINGS", String.valueOf(data.bookings.size())));
          stats.add(stat("CONFIRMED", String.valueOf(active)));
          stats.add(stat("NET SIMULATED REVENUE", "LKR " + revenue));
          body.add(stats, BorderLayout.NORTH);
          JPanel ledger = Theme.card();
          ledger.add(Theme.label("Payment ledger", 19, Theme.NAVY), BorderLayout.NORTH);
          JTable table =
              table(
                  new String[] {"Transaction", "Payment", "Type", "Amount (LKR)", "Recorded"},
                  data.transactions.stream()
                      .map(
                          t ->
                              new Object[] {
                                t.id(),
                                t.paymentId(),
                                t.type(),
                                t.amount(),
                                t.createdAt().format(TIME)
                              })
                      .toArray(Object[][]::new));
          ledger.add(new JScrollPane(table), BorderLayout.CENTER);
          body.add(ledger, BorderLayout.CENTER);
          content.add(body, BorderLayout.CENTER);
          refresh();
        });
    refresh();
  }

  private record ReportData(List<Booking> bookings, List<Transaction> transactions) {}

  private JPanel stat(String title, String value) {
    JPanel p = Theme.card();
    p.add(Theme.label(title, 11, Theme.MUTED), BorderLayout.NORTH);
    p.add(Theme.label(value, 25, Theme.NAVY), BorderLayout.CENTER);
    return p;
  }

  private int selected(JTable table) {
    int i = table.getSelectedRow();
    if (i < 0) warn("Select a row first.");
    return i < 0 ? -1 : table.convertRowIndexToModel(i);
  }

  private void warn(String message) {
    JOptionPane.showMessageDialog(this, message, "Action needed", JOptionPane.WARNING_MESSAGE);
  }

  private static JTable table(String[] columns, Object[][] rows) {
    JTable t =
        new JTable(
            new DefaultTableModel(rows, columns) {
              @Override
              public boolean isCellEditable(int r, int c) {
                return false;
              }

              @Override
              public Class<?> getColumnClass(int column) {
                return rows.length == 0 || rows[0][column] == null
                    ? Object.class
                    : rows[0][column].getClass();
              }
            });
    t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    t.setAutoCreateRowSorter(true);
    t.setFillsViewportHeight(true);
    t.setShowVerticalLines(false);
    t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
    t.setRowHeight(44);
    // Give long routes and timestamps more room than IDs/seat numbers.
    FontMetrics metrics = t.getFontMetrics(t.getFont());
    for (int column = 0; column < columns.length; column++) {
      int width = metrics.stringWidth(columns[column]) + 28;
      for (Object[] row : rows)
        width = Math.max(width, metrics.stringWidth(String.valueOf(row[column])) + 24);
      t.getColumnModel().getColumn(column).setMinWidth(45);
      t.getColumnModel().getColumn(column).setPreferredWidth(Math.min(width, 260));
    }
    if (rows.length > 0) t.setRowSelectionInterval(0, 0);
    return t;
  }

  private static GridBagConstraints constraints() {
    GridBagConstraints g = new GridBagConstraints();
    g.gridx = 0;
    g.gridy = 0;
    g.weightx = 1;
    g.fill = GridBagConstraints.HORIZONTAL;
    g.insets = new Insets(7, 0, 7, 0);
    return g;
  }

  private static void addRow(JPanel panel, GridBagConstraints g, Component c) {
    panel.add(c, g);
    g.gridy++;
  }

  private static void field(JPanel panel, GridBagConstraints g, String title, JComponent input) {
    JLabel label = Theme.label(title, 13, Theme.MUTED);
    label.setLabelFor(input);
    addRow(panel, g, label);
    input.setPreferredSize(new Dimension(240, 40));
    addRow(panel, g, input);
  }

  private static JPanel form(Object... pairs) {
    JPanel p = Theme.panel(new GridBagLayout());
    GridBagConstraints g = constraints();
    for (int i = 0; i < pairs.length; i += 2)
      field(p, g, (String) pairs[i], (JComponent) pairs[i + 1]);
    return p;
  }
}
