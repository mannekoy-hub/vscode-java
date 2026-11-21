import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.LocalDate;

/** BoarderApp.java - boarder GUI.  Run: java BoarderApp  (admin must add the boarder first)
 *  Shares its look (colours, fonts, buttons, fields) with AdminApp.java, so keep both files in the same folder. */
public class BoarderApp extends JFrame {
    static final Color GOOD = new Color(0x1E7B45), AMBER = new Color(0xB57A0C), IDLE = new Color(0xB9B3A5);

    // ---------------------------------------------------- custom components
    /** Status text drawn on a soft pill tinted with its own foreground colour. */
    static class PillLabel extends JLabel {
        PillLabel() { super("", SwingConstants.CENTER); setFont(AdminApp.sans(Font.BOLD, 12)); setBorder(new EmptyBorder(5, 14, 5, 14)); setOpaque(false); setHorizontalTextPosition(SwingConstants.LEFT); setIconTextGap(6); }
        protected void paintComponent(Graphics g) {
            if (getText() != null && !getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create(); AdminApp.aa(g2);
                Color c = getForeground(); g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 30));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight())); g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /** Rounded password field with a focus ring (same look as AdminApp.RField). */
    static class RPass extends JPasswordField {
        RPass(int c) {
            super(c); setOpaque(false); setBorder(new EmptyBorder(9, 14, 9, 14)); setFont(AdminApp.sans(Font.PLAIN, 13));
            setForeground(AdminApp.INK); setCaretColor(AdminApp.INK);
            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { repaint(); }
                public void focusLost(FocusEvent e) { repaint(); }
            });
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); AdminApp.aa(g2);
            RoundRectangle2D rr = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(Color.WHITE); g2.fill(rr);
            g2.setColor(isFocusOwner() ? AdminApp.FOCUS : AdminApp.INPUT_LINE); g2.setStroke(new BasicStroke(isFocusOwner() ? 1.6f : 1f)); g2.draw(rr);
            g2.dispose(); super.paintComponent(g);
        }
    }

    /** Check / partial / cross / dash drawn as shapes, so no font is needed. kind: 0 dash, 1 check, 2 partial, 3 cross. */
    static class MarkIcon implements Icon {
        final int kind, s; final Color c;
        MarkIcon(int kind, Color c, int s) { this.kind = kind; this.c = c; this.s = s; }
        public int getIconWidth() { return s; }
        public int getIconHeight() { return s; }
        public void paintIcon(Component k, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create(); AdminApp.aa(g2); g2.setColor(c);
            g2.setStroke(new BasicStroke(Math.max(1.6f, s / 7f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (kind == 1) { Path2D.Float p = new Path2D.Float(); p.moveTo(x + s * 0.12f, y + s * 0.55f); p.lineTo(x + s * 0.40f, y + s * 0.82f); p.lineTo(x + s * 0.90f, y + s * 0.20f); g2.draw(p); }
            else if (kind == 2) { g2.draw(new Ellipse2D.Float(x + 1.5f, y + 1.5f, s - 3, s - 3)); g2.fill(new Arc2D.Float(x + 1.5f, y + 1.5f, s - 3, s - 3, 90, 180, Arc2D.PIE)); }
            else if (kind == 3) { g2.draw(new Line2D.Float(x + s * 0.22f, y + s * 0.22f, x + s * 0.78f, y + s * 0.78f)); g2.draw(new Line2D.Float(x + s * 0.78f, y + s * 0.22f, x + s * 0.22f, y + s * 0.78f)); }
            else g2.draw(new Line2D.Float(x + s * 0.25f, y + s * 0.5f, x + s * 0.75f, y + s * 0.5f));
            g2.dispose();
        }
    }

    /** Page that follows the scroll-pane width, so cards always stretch to fit. */
    static class ScrollPage extends JPanel implements Scrollable {
        ScrollPage() { super(new BorderLayout()); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(40, r.height - 40); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    static void arcs(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(255, 255, 255, 16)); g2.setStroke(new BasicStroke(1.4f));
        g2.draw(new Ellipse2D.Float(w * 0.50f, -h * 0.95f, h * 1.9f, h * 1.9f));
        g2.draw(new Ellipse2D.Float(w * 0.68f, -h * 0.55f, h * 1.2f, h * 1.2f));
    }

    /** Dark green banner with a rounded bottom edge, used on the login page. */
    static class Hero extends JPanel {
        Hero(String tag, String title, String sub) {
            super(new GridBagLayout()); setOpaque(false); setPreferredSize(new Dimension(10, 230));
            JPanel b = new JPanel(); b.setOpaque(false); b.setLayout(new BoxLayout(b, BoxLayout.Y_AXIS));
            JLabel a = new JLabel(tag), t = new JLabel(title), s = new JLabel(sub);
            a.setFont(AdminApp.sans(Font.BOLD, 11)); a.setForeground(AdminApp.GOLD);
            t.setFont(AdminApp.serif(Font.PLAIN, 38)); t.setForeground(AdminApp.CREAM);
            s.setFont(AdminApp.sans(Font.PLAIN, 13)); s.setForeground(new Color(0xBFD0C4));
            a.setAlignmentX(Component.CENTER_ALIGNMENT); t.setAlignmentX(Component.CENTER_ALIGNMENT); s.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.add(a); b.add(Box.createVerticalStrut(8)); b.add(t); b.add(Box.createVerticalStrut(6)); b.add(s); add(b);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); AdminApp.aa(g2);
            RoundRectangle2D rr = new RoundRectangle2D.Float(0, -80, getWidth(), getHeight() + 80, 60, 60);
            g2.setColor(AdminApp.FOREST); g2.fill(rr); g2.clip(rr); arcs(g2, getWidth(), getHeight()); g2.dispose();
        }
    }

    // ----------------------------------------------------------------- state
    CardLayout cards = new CardLayout(); JPanel root = new JPanel(cards);
    JTextField name = new AdminApp.RField(16), reading = new AdminApp.RField(8); JPasswordField pin = new RPass(16);
    JLabel welcome = new JLabel(), head = new JLabel(), due = new JLabel(), prev = new JLabel(), tot = new JLabel(), stat = new PillLabel();
    JLabel[] amt = new JLabel[3], st = new JLabel[3], cell = new JLabel[12];
    JTextArea notes = new JTextArea(8, 30); javax.swing.Timer timer;
    int meId, y = LocalDate.now().getYear(), m = LocalDate.now().getMonthValue();

    BoarderApp() {
        super("Boarding House - BOARDER"); setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(600, 820);
        setMinimumSize(new Dimension(560, 640)); setLocationRelativeTo(null);
        root.setBackground(AdminApp.CREAM);
        root.add(loginPanel(), "login"); root.add(mainPanel(), "main"); add(root);
        timer = new javax.swing.Timer(5000, e -> { if (meId > 0) refresh(); });
    }

    // ------------------------------------------------------------ UI helpers
    static JLabel text(String s, Font f, Color c) { JLabel l = new JLabel(s); l.setFont(f); l.setForeground(c); return l; }

    static JPanel right(JComponent c) { JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); p.setOpaque(false); p.add(c); return p; }

    static void stack(JPanel p, JComponent c, int gap) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(c); if (gap > 0) p.add(Box.createVerticalStrut(gap));
    }

    static JPanel vbox() { JPanel p = new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); return p; }

    static Card_ card(int pad) {
        Card_ c = new Card_(new BorderLayout(0, 12)); c.setBorder(new EmptyBorder(pad, pad + 2, pad, pad + 2)); return c;
    }
    /** Light card (kept as a tiny wrapper so the call sites stay short). */
    static class Card_ extends AdminApp.Card { Card_(LayoutManager lm) { super(lm, AdminApp.CARD, AdminApp.LINE, 26); } }

    // ------------------------------------------------------------------ login
    JPanel loginPanel() {
        JPanel p = new JPanel(new BorderLayout()), f = new JPanel(new GridLayout(0, 1, 0, 4)), wrap = new JPanel(new GridBagLayout());
        p.setOpaque(false); f.setOpaque(false); wrap.setOpaque(false);
        Hero hero = new Hero("BOARDER LOGIN", "Boarding House", "Your room, your bills, all in one place.");
        JPanel tb = vbox(); JLabel t = text("Welcome back", AdminApp.serif(Font.PLAIN, 28), AdminApp.INK),
                s = text("Sign in with the name and PIN you were given.", AdminApp.sans(Font.PLAIN, 13), AdminApp.MUTED);
        t.setAlignmentX(Component.LEFT_ALIGNMENT); s.setAlignmentX(Component.LEFT_ALIGNMENT); tb.add(t); tb.add(Box.createVerticalStrut(3)); tb.add(s);
        AdminApp.Btn go = new AdminApp.Btn("Login", 0); go.addActionListener(e -> doLogin()); pin.addActionListener(e -> doLogin());
        JPanel goRow = new JPanel(new BorderLayout()); goRow.setOpaque(false); goRow.add(go, BorderLayout.NORTH);
        f.add(tb); f.add(AdminApp.labeled("Name", name)); f.add(AdminApp.labeled("PIN", pin)); f.add(goRow);
        f.setPreferredSize(new Dimension(330, 250));
        Card_ c = new Card_(new BorderLayout()); c.setBorder(new EmptyBorder(28, 34, 28, 34)); c.add(f);
        GridBagConstraints g = new GridBagConstraints(); g.gridy = 0; g.anchor = GridBagConstraints.NORTH; g.insets = new Insets(36, 0, 0, 0); wrap.add(c, g);
        JLabel hint = text("New here? Ask your landlord to add you first.", AdminApp.sans(Font.PLAIN, 12), AdminApp.MUTED);
        g.gridy = 1; g.weighty = 1; g.insets = new Insets(14, 0, 0, 0); wrap.add(hint, g);
        p.add(hero, BorderLayout.NORTH); p.add(wrap); return p;
    }

    void doLogin() {
        Shared.Boarder b = Shared.login(name.getText().trim(), new String(pin.getPassword()).trim());
        if (b == null) { JOptionPane.showMessageDialog(this, "Wrong name or PIN.", "Login failed", JOptionPane.ERROR_MESSAGE); return; }
        meId = b.id; welcome.setText("Welcome, " + b.name + "  (Room " + b.room + ")"); refresh(); timer.start(); cards.show(root, "main");
    }

    // ------------------------------------------------------------------- main
    JPanel mainPanel() {
        JPanel p = vbox(); p.setBorder(new EmptyBorder(18, 20, 18, 20));

        // header banner
        AdminApp.Card hero = new AdminApp.Card(new BorderLayout(), AdminApp.FOREST, null, 30) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create(); AdminApp.aa(g2);
                g2.clip(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 30, 30)); arcs(g2, getWidth(), getHeight()); g2.dispose();
            }
        };
        hero.setBorder(new EmptyBorder(20, 26, 20, 26));
        JPanel hl = vbox(); JLabel tag = text("BOARDER PORTAL", AdminApp.sans(Font.BOLD, 11), AdminApp.GOLD);
        welcome.setFont(AdminApp.serif(Font.PLAIN, 24)); welcome.setForeground(AdminApp.CREAM);
        head.setText(Shared.MONTHS[m - 1] + " " + y + " bill"); head.setFont(AdminApp.sans(Font.BOLD, 13)); head.setForeground(new Color(0xBFD0C4));
        due.setFont(AdminApp.sans(Font.PLAIN, 13)); due.setForeground(AdminApp.GOLD);
        stack(hl, tag, 8); stack(hl, welcome, 12); stack(hl, head, 2); stack(hl, due, 0);
        hero.add(hl); stack(p, hero, 14);

        // bill breakdown
        Card_ bill = card(18); JPanel rows = vbox();
        for (int k = 0; k < 3; k++) {
            amt[k] = new JLabel(); st[k] = new PillLabel();
            amt[k].setFont(AdminApp.sans(Font.PLAIN, 14)); amt[k].setForeground(AdminApp.INK);
            JLabel key = text(Shared.KEYS[k], AdminApp.sans(Font.BOLD, 14), AdminApp.INK);
            JPanel row = new JPanel(new GridLayout(1, 3, 8, 0)); row.setOpaque(false);
            row.setBorder(new CompoundBorder(k < 2 ? new MatteBorder(0, 0, 1, 0, AdminApp.LINE) : new EmptyBorder(0, 0, 0, 0), new EmptyBorder(11, 0, 11, 0)));
            row.add(key); row.add(amt[k]); row.add(right(st[k])); stack(rows, row, 0);
        }
        tot.setFont(AdminApp.serif(Font.PLAIN, 20)); tot.setForeground(AdminApp.INK);
        JPanel totRow = new JPanel(new BorderLayout(10, 0)); totRow.setOpaque(false);
        totRow.setBorder(new CompoundBorder(new MatteBorder(2, 0, 0, 0, AdminApp.FOREST), new EmptyBorder(12, 0, 0, 0)));
        totRow.add(tot, BorderLayout.WEST); totRow.add(right(stat), BorderLayout.EAST);
        bill.add(text("Bill breakdown", AdminApp.serif(Font.PLAIN, 20), AdminApp.INK), BorderLayout.NORTH); bill.add(rows); bill.add(totRow, BorderLayout.SOUTH);
        stack(p, bill, 14);

        // electric meter
        Card_ rf = card(18); AdminApp.Btn sub = new AdminApp.Btn("Submit", 0); sub.addActionListener(e -> submit());
        prev.setFont(AdminApp.sans(Font.PLAIN, 13)); prev.setForeground(AdminApp.MUTED);
        JPanel inRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); inRow.setOpaque(false);
        inRow.add(AdminApp.cap("Present reading:")); inRow.add(Box.createHorizontalStrut(12)); inRow.add(reading);
        inRow.add(Box.createHorizontalStrut(12)); inRow.add(sub);
        rf.add(text("Electric meter reading", AdminApp.serif(Font.PLAIN, 20), AdminApp.INK), BorderLayout.NORTH);
        rf.add(prev); rf.add(inRow, BorderLayout.SOUTH); stack(p, rf, 14);

        // history
        Card_ hf = card(18); JPanel grid = new JPanel(new GridLayout(2, 12, 0, 4)); grid.setOpaque(false);
        for (int i = 0; i < 12; i++) grid.add(text(Shared.MONTHS[i].substring(0, 3), AdminApp.sans(Font.PLAIN, 11), AdminApp.MUTED, SwingConstants.CENTER));
        for (int i = 0; i < 12; i++) { cell[i] = new JLabel("", SwingConstants.CENTER); cell[i].setFont(AdminApp.sans(Font.BOLD, 16)); grid.add(cell[i]); }
        JPanel hh = vbox();
        stack(hh, text(y + " history", AdminApp.serif(Font.PLAIN, 20), AdminApp.INK), 2);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); legend.setOpaque(false);
        String[] ln = {"paid", "partial", "unpaid"}; Color[] lc = {GOOD, AMBER, AdminApp.DANGER};
        for (int i = 0; i < 3; i++) {
            JLabel l = new JLabel(ln[i], new MarkIcon(i + 1, lc[i], 12), SwingConstants.LEFT);
            l.setIconTextGap(5); l.setFont(AdminApp.sans(Font.PLAIN, 12)); l.setForeground(AdminApp.MUTED); legend.add(l);
            if (i < 2) legend.add(Box.createHorizontalStrut(14));
        }
        stack(hh, legend, 0);
        hf.add(hh, BorderLayout.NORTH); hf.add(grid); stack(p, hf, 14);

        // notifications
        Card_ nf = card(18); notes.setEditable(false); notes.setLineWrap(true); notes.setWrapStyleWord(true);
        notes.setFont(AdminApp.sans(Font.PLAIN, 13)); notes.setForeground(AdminApp.INK); notes.setBackground(AdminApp.CARD);
        notes.setBorder(new EmptyBorder(6, 2, 6, 2));
        JScrollPane sp = new JScrollPane(notes); sp.setBorder(new MatteBorder(1, 0, 0, 0, AdminApp.LINE)); sp.setBackground(AdminApp.CARD);
        sp.getViewport().setBackground(AdminApp.CARD); sp.getVerticalScrollBar().setUI(new AdminApp.ThinBar()); sp.getVerticalScrollBar().setBackground(AdminApp.CARD);
        sp.setPreferredSize(new Dimension(100, 150));
        nf.add(text("Notifications", AdminApp.serif(Font.PLAIN, 20), AdminApp.INK), BorderLayout.NORTH); nf.add(sp); stack(p, nf, 14);

        AdminApp.Btn out = new AdminApp.Btn("Logout", 1); out.addActionListener(e -> { timer.stop(); meId = 0; pin.setText(""); cards.show(root, "login"); });
        JPanel outRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); outRow.setOpaque(false); outRow.add(out); stack(p, outRow, 0);

        ScrollPage page = new ScrollPage(); page.add(p, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(page, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null); scroll.setOpaque(false); scroll.getViewport().setBackground(AdminApp.CREAM);
        scroll.getVerticalScrollBar().setUI(new AdminApp.ThinBar()); scroll.getVerticalScrollBar().setBackground(AdminApp.CREAM);
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(scroll); return wrap;
    }

    static JLabel text(String s, Font f, Color c, int align) { JLabel l = text(s, f, c); l.setHorizontalAlignment(align); return l; }

    void refresh() {
        Shared.tick(y, m); Shared.Boarder b = Shared.find(meId); Shared.Bill bl = Shared.bill(b, y, m);
        due.setText("Due date: " + Shared.dueDate(y, m));
        for (int k = 0; k < 3; k++) {
            double a = bl.amt(k);
            amt[k].setText(Double.isNaN(a) ? "waiting for your reading" : Shared.peso(a));
            amt[k].setForeground(Double.isNaN(a) ? AdminApp.MUTED : AdminApp.INK); amt[k].setFont(AdminApp.fit(AdminApp.sans(Font.PLAIN, 14), amt[k].getText()));
            st[k].setText(bl.paid[k] ? "PAID" : "UNPAID"); st[k].setIcon(bl.paid[k] ? new MarkIcon(1, GOOD, 11) : null); st[k].setForeground(bl.paid[k] ? GOOD : AdminApp.DANGER);
        }
        tot.setText("Total: " + Shared.peso(bl.total) + (bl.elec == null ? "  (+ electricity)" : ""));
        tot.setFont(AdminApp.fit(AdminApp.serif(Font.PLAIN, 20), tot.getText()));
        stat.setText(bl.status.toUpperCase());
        stat.setForeground(bl.status.equals("Paid") ? GOOD : bl.status.equals("Overdue") ? AdminApp.DANGER : AMBER);
        prev.setText("Previous: " + bl.prev + (bl.present != null ? "  Submitted: " + bl.present + " (" + bl.kwh + " kWh)  " : "  "));
        int[] mk = Shared.masks(b, y);
        for (int i = 1; i <= 12; i++) {
            int n = 0; for (int k = 0; k < 3; k++) if (Shared.isPaid(mk[k], i)) n++;
            boolean past = i <= m;
            cell[i - 1].setIcon(new MarkIcon(n == 3 ? 1 : n > 0 ? 2 : past ? 3 : 0, n == 3 ? GOOD : n > 0 ? AMBER : past ? AdminApp.DANGER : IDLE, 16));
        }
        StringBuilder s = new StringBuilder();
        for (int i = Shared.d.notifs.size() - 1; i >= 0; i--) {        // newest first
            Shared.Notif n = Shared.d.notifs.get(i);
            if (n.bid == meId && n.sent == 1) s.append("[").append(n.due.toLocalDate()).append("] ").append(n.msg).append("\n\n");
        }
        notes.setText(s.toString()); notes.setCaretPosition(0);
    }

    void submit() {
        double v; try { v = Double.parseDouble(reading.getText().trim()); } catch (Exception e) { JOptionPane.showMessageDialog(this, "Enter a number.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        String er = Shared.submitReading(meId, y, m, v);
        if (er != null) { JOptionPane.showMessageDialog(this, er, "Error", JOptionPane.ERROR_MESSAGE); return; }
        reading.setText(""); refresh(); JOptionPane.showMessageDialog(this, "Reading submitted. Your bill was updated.");
    }

    public static void main(String[] a) {
        System.setProperty("awt.useSystemAAFontSettings", "on"); System.setProperty("swing.aatext", "true");
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignore) { }
        AdminApp.theme();
        Shared.init(); SwingUtilities.invokeLater(() -> new BoarderApp().setVisible(true));
    }
}

/*
after i input the 2 boarders with the same room the rent should be devided depends on the number of boarder each room

what should we do about that?
 */