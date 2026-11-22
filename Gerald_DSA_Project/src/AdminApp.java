import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** AdminApp.java - landlord GUI.  Run: java AdminApp */
public class AdminApp extends JFrame {

    // ---------------------------------------------------------------- theme
    static final Color CREAM = new Color(0xF4EFE6), CARD = new Color(0xFCFAF6), WASH = new Color(0xEFE9DC),
            FOREST = new Color(0x1B3A2E), INK = new Color(0x14261E), SAGE = new Color(0x8FAE98),
            LINE = new Color(0xE2DACA), INPUT_LINE = new Color(0xD6CDBA), FOCUS = new Color(0x4F7A63),
            MUTED = new Color(0x7B7F74), GOLD = new Color(0xC8A24A), DANGER = new Color(0xA33A2C),
            SELECT = new Color(0xE4EDE6);
    static final String SERIF = pick(Font.SERIF, "Georgia", "Palatino Linotype", "Book Antiqua", "Times New Roman");
    static final String SANS = pick(Font.SANS_SERIF, "Segoe UI", "Helvetica Neue", "Inter", "Arial");

    static String pick(String fallback, String... names) {
        Set<String> have = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String n : names) if (have.contains(n)) return n;
        return fallback;
    }
    static Font sans(int style, int size) { return new Font(SANS, style, size); }
    static Font serif(int style, int size) { return new Font(SERIF, style, size); }
    /** Returns f, or a logical font of the same style/size when f has no glyph for some character in s (e.g. the peso sign or check marks). */
    static Font fit(Font f, String s) {
        if (s == null || f.canDisplayUpTo(s) < 0) return f;
        return new Font(f.getFamily().equals(SERIF) ? Font.SERIF : Font.SANS_SERIF, f.getStyle(), f.getSize());
    }
    static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    static Color stripe(int r) { return r % 2 == 0 ? CARD : new Color(0xF8F4EB); }

    static void theme() {
        UIManager.put("Panel.background", CREAM);
        UIManager.put("OptionPane.background", CREAM);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("Label.foreground", INK);
        UIManager.put("CheckBox.background", CREAM);
        UIManager.put("CheckBox.foreground", INK);
        UIManager.put("Label.font", new FontUIResource(sans(Font.PLAIN, 13)));
        UIManager.put("OptionPane.messageFont", new FontUIResource(sans(Font.PLAIN, 13)));
        UIManager.put("OptionPane.buttonFont", new FontUIResource(sans(Font.BOLD, 12)));
        UIManager.put("Button.font", new FontUIResource(sans(Font.BOLD, 12)));
        UIManager.put("CheckBox.font", new FontUIResource(sans(Font.PLAIN, 13)));
    }

    // ----------------------------------------------------- custom components
    /** Rounded surface used for cards, stat tiles and the sidebar. */
    static class Card extends JPanel {
        final Color fill, line; final int arc;
        Card(LayoutManager lm, Color fill, Color line, int arc) { super(lm); this.fill = fill; this.line = line; this.arc = arc; setOpaque(false); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
            RoundRectangle2D rr = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(fill); g2.fill(rr);
            if (line != null) { g2.setColor(line); g2.draw(rr); }
            g2.dispose();
        }
    }

    /** Pill button. kind: 0 = filled, 1 = outlined, 2 = outlined danger. */
    static class Btn extends JButton {
        final int kind;
        Btn(String t, int kind) {
            super(t); this.kind = kind;
            setFont(sans(Font.BOLD, 13)); setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false);
            setOpaque(false); setRolloverEnabled(true); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 22, 10, 22));
            setForeground(kind == 0 ? CREAM : kind == 1 ? FOREST : DANGER);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
            ButtonModel m = getModel(); int w = getWidth() - 1, h = getHeight() - 1;
            RoundRectangle2D rr = new RoundRectangle2D.Float(0, 0, w, h, h, h);
            if (kind == 0) { g2.setColor(m.isPressed() ? INK : m.isRollover() ? new Color(0x2A5340) : FOREST); g2.fill(rr); }
            else {
                Color c = kind == 1 ? FOREST : DANGER;
                if (m.isRollover() || m.isPressed()) { g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), m.isPressed() ? 36 : 20)); g2.fill(rr); }
                g2.setColor(c); g2.setStroke(new BasicStroke(1.3f)); g2.draw(rr);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Sidebar navigation item. */
    static class Nav extends JButton {
        boolean on;
        Nav(String t) {
            super(t); setFont(sans(Font.BOLD, 14)); setHorizontalAlignment(SwingConstants.LEFT);
            setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false); setOpaque(false);
            setRolloverEnabled(true); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(13, 20, 13, 20)); select(false);
        }
        void select(boolean b) { on = b; setForeground(b ? INK : new Color(0xDCE6DF)); repaint(); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
            int w = getWidth() - 1, h = getHeight() - 1;
            if (on) {
                g2.setColor(CREAM); g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
                g2.setColor(GOLD); g2.fill(new Ellipse2D.Float(w - 24, h / 2f - 3, 7, 7));
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 22)); g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Small triangle button used by the combo box and spinner. */
    static class Arrow extends JButton {
        final int dir;
        Arrow(int d) {
            dir = d; setBorderPainted(false); setContentAreaFilled(false); setFocusPainted(false); setFocusable(false);
            setOpaque(false); setMargin(new Insets(0, 0, 0, 0)); setBorder(null); setPreferredSize(new Dimension(28, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
            int cx = getWidth() / 2, cy = getHeight() / 2;
            Polygon p = dir == SwingConstants.NORTH
                    ? new Polygon(new int[]{cx - 4, cx + 4, cx}, new int[]{cy + 2, cy + 2, cy - 3}, 3)
                    : new Polygon(new int[]{cx - 4, cx + 4, cx}, new int[]{cy - 2, cy - 2, cy + 3}, 3);
            g2.setColor(FOCUS); g2.fillPolygon(p); g2.dispose();
        }
    }

    static class FlatCombo extends BasicComboBoxUI {
        protected JButton createArrowButton() { return new Arrow(SwingConstants.SOUTH); }
        public void paintCurrentValueBackground(Graphics g, Rectangle b, boolean focus) { }
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
            RoundRectangle2D rr = new RoundRectangle2D.Float(0, 0, c.getWidth() - 1, c.getHeight() - 1, 14, 14);
            g2.setColor(Color.WHITE); g2.fill(rr); g2.setColor(INPUT_LINE); g2.draw(rr); g2.dispose();
            super.paint(g, c);
        }
    }

    static class FlatSpin extends BasicSpinnerUI {
        protected Component createNextButton() {
            Component c = new Arrow(SwingConstants.NORTH); c.setName("Spinner.nextButton"); installNextButtonListeners(c); return c;
        }
        protected Component createPreviousButton() {
            Component c = new Arrow(SwingConstants.SOUTH); c.setName("Spinner.previousButton"); installPreviousButtonListeners(c); return c;
        }
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
            RoundRectangle2D rr = new RoundRectangle2D.Float(0, 0, c.getWidth() - 1, c.getHeight() - 1, 14, 14);
            g2.setColor(Color.WHITE); g2.fill(rr); g2.setColor(INPUT_LINE); g2.draw(rr); g2.dispose();
        }
    }

    /** Rounded text field with a focus ring. */
    static class RField extends JTextField {
        RField(String t, int c) { super(t, c); init(); }
        RField(int c) { super(c); init(); }
        private void init() {
            setOpaque(false); setBorder(new EmptyBorder(9, 14, 9, 14)); setFont(sans(Font.PLAIN, 13));
            setForeground(INK); setCaretColor(INK); setSelectionColor(new Color(0xCFE0D4)); setSelectedTextColor(INK);
            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { repaint(); }
                public void focusLost(FocusEvent e) { repaint(); }
            });
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
            RoundRectangle2D rr = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(Color.WHITE); g2.fill(rr);
            g2.setColor(isFocusOwner() ? FOCUS : INPUT_LINE); g2.setStroke(new BasicStroke(isFocusOwner() ? 1.6f : 1f)); g2.draw(rr);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class Dot implements Icon {
        final Color c; final int d;
        Dot(Color c, int d) { this.c = c; this.d = d; }
        public int getIconWidth() { return d; }
        public int getIconHeight() { return d; }
        public void paintIcon(Component k, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create(); aa(g2); g2.setColor(c); g2.fillOval(x, y, d, d); g2.dispose();
        }
    }

    static class ThinBar extends BasicScrollBarUI {
        JButton zero() {
            JButton b = new JButton(); Dimension z = new Dimension(0, 0);
            b.setPreferredSize(z); b.setMinimumSize(z); b.setMaximumSize(z); return b;
        }
        protected JButton createDecreaseButton(int o) { return zero(); }
        protected JButton createIncreaseButton(int o) { return zero(); }
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
            g2.setColor(isDragging ? SAGE : new Color(0xD3CBBB));
            g2.fill(new RoundRectangle2D.Float(r.x + 3, r.y + 2, r.width - 6, r.height - 4, 8, 8)); g2.dispose();
        }
        public Dimension getPreferredSize(JComponent c) { return new Dimension(12, 12); }
    }

    static class HeadR extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v == null ? "" : String.valueOf(v).toUpperCase(), false, false, r, c);
            setOpaque(true); setBackground(CARD); setForeground(MUTED); setFont(sans(Font.BOLD, 11));
            setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(0, 12, 0, 12)));
            return this;
        }
    }

    static class CellR extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, false, r, c);
            String s = String.valueOf(v);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setBackground(sel ? SELECT : stripe(r));
            boolean wait = s.startsWith("Awaiting");
            setForeground(wait ? MUTED : INK);
            setFont(fit(wait ? sans(Font.ITALIC, 13) : sans(c == 0 || (t.getColumnCount() == 7 && c == 5) ? Font.BOLD : Font.PLAIN, 13), s));
            return this;
        }
    }

    /** Renders the Status column as a coloured pill. */
    static class StatusR implements TableCellRenderer {
        final JComponent pill = new JComponent() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
                g2.setColor(cell); g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setFont(sans(Font.BOLD, 12)); FontMetrics fm = g2.getFontMetrics();
                int w = fm.stringWidth(text) + 26, h = 26, y = (getHeight() - h) / 2;
                g2.setColor(fill); g2.fill(new RoundRectangle2D.Float(12, y, w, h, h, h));
                g2.setColor(fg); g2.drawString(text, 25, y + (h + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        String text = ""; Color cell = CARD, fill = WASH, fg = INK;
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            text = String.valueOf(v); cell = sel ? SELECT : stripe(r);
            if (text.equals("Paid")) { fill = new Color(0xD7EBDC); fg = new Color(0x1E5B35); }
            else if (text.equals("Overdue")) { fill = new Color(0xF6D6D0); fg = new Color(0x9B2C20); }
            else { fill = new Color(0xFBEBC8); fg = new Color(0x8A5A00); }
            return pill;
        }
    }

    // ----------------------------------------------------------------- state
    JComboBox<String> mo = new JComboBox<>(Shared.MONTHS);
    JSpinner yr = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2020, 2100, 1));
    JLabel dSub = new JLabel(" "), lPaid = new JLabel("0"), lUnpaid = new JLabel("0"), lOver = new JLabel("0"), lGot = new JLabel("0");
    DefaultTableModel dm = model("Name", "Room", "Rent", "Water", "Electricity", "Total", "Status");
    DefaultTableModel bm = new DefaultTableModel(new String[]{"Name", "Room", "PIN", "Rent", "Move-in"}, 0) {   // editable cells
        public boolean isCellEditable(int r, int c) { return true; } };
    boolean loading = false;
    JTable dt = new JTable(dm), bt = new JTable(bm);
    List<Integer> dIds = new ArrayList<>(), bIds = new ArrayList<>();
    JTextField[] f = new JTextField[5], cfg = new JTextField[5];
    JTextField tb = new RField(8), tk = new RField(8);
    CardLayout cl = new CardLayout();
    JPanel cards = new JPanel(cl);
    Nav[] navs = new Nav[3];

    static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
    }

    AdminApp() {
        super("Boarding House - ADMIN"); setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(1180, 720);
        setMinimumSize(new Dimension(1000, 620)); setLocationRelativeTo(null);
        cards.setOpaque(false);
        cards.add(dashboard(), "dash"); cards.add(boarders(), "boarders"); cards.add(settings(), "settings");
        JPanel root = new JPanel(new BorderLayout()), side = new JPanel(new BorderLayout());
        root.setBackground(CREAM); side.setOpaque(false); side.setBorder(new EmptyBorder(14, 14, 14, 0));
        side.add(sidebar()); root.add(side, BorderLayout.WEST); root.add(cards, BorderLayout.CENTER); setContentPane(root);
        mo.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        mo.addActionListener(e -> refresh()); yr.addChangeListener(e -> refresh());
        styleTable(dt); styleTable(bt);
        bt.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);      // commit edit when clicking away
        RField editor = new RField(0);
        bt.setDefaultEditor(Object.class, new DefaultCellEditor(editor));
        bm.addTableModelListener(e -> {                                      // save edited boarder cell
            if (loading || e.getType() != TableModelEvent.UPDATE || e.getColumn() < 0) return;
            int r = e.getFirstRow(), c = e.getColumn(); if (r < 0 || r >= bIds.size()) return;
            String er = Shared.updateBoarder(bIds.get(r), c, String.valueOf(bm.getValueAt(r, c)).trim());
            if (er != null) err(er);
            SwingUtilities.invokeLater(this::refresh);                        // reload (restores old value if rejected)
        });
        dt.setDefaultRenderer(Object.class, new CellR());
        bt.setDefaultRenderer(Object.class, new CellR());
        dt.getColumnModel().getColumn(6).setCellRenderer(new StatusR());      // status shown as a pill
        int[] cw = {150, 70, 130, 130, 160, 120, 120};                          // column widths
        for (int i = 0; i < cw.length; i++) dt.getColumnModel().getColumn(i).setPreferredWidth(cw[i]);
        refresh(); new javax.swing.Timer(15000, e -> refresh()).start();
    }

    // ------------------------------------------------------------ UI helpers
    static void styleTable(JTable t) {
        t.setRowHeight(40); t.setShowGrid(false); t.setIntercellSpacing(new Dimension(0, 0)); t.setFillsViewportHeight(true);
        t.setBackground(CARD); t.setForeground(INK); t.setFont(sans(Font.PLAIN, 13));
        t.setSelectionBackground(SELECT); t.setSelectionForeground(INK);
        JTableHeader h = t.getTableHeader(); h.setReorderingAllowed(false); h.setDefaultRenderer(new HeadR());
        h.setBackground(CARD); h.setPreferredSize(new Dimension(0, 38));
    }

    static Card tableCard(JTable t) {
        Card c = new Card(new BorderLayout(), CARD, LINE, 26); c.setBorder(new EmptyBorder(12, 12, 12, 12));
        JScrollPane sp = new JScrollPane(t); sp.setBorder(null); sp.setBackground(CARD); sp.getViewport().setBackground(CARD);
        sp.setColumnHeaderView(t.getTableHeader()); sp.getColumnHeader().setBackground(CARD);
        for (JScrollBar sb : new JScrollBar[]{sp.getVerticalScrollBar(), sp.getHorizontalScrollBar()}) { sb.setUI(new ThinBar()); sb.setBackground(CARD); }
        JPanel corner = new JPanel(); corner.setBackground(CARD); sp.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
        c.add(sp); return c;
    }

    static JPanel page() {
        JPanel p = new JPanel(new BorderLayout(0, 18)); p.setOpaque(false); p.setBorder(new EmptyBorder(26, 28, 24, 28)); return p;
    }

    static JPanel head(String title, JLabel sub, Component right) {
        JPanel h = new JPanel(new BorderLayout(20, 0)), l = new JPanel(); h.setOpaque(false); l.setOpaque(false);
        l.setLayout(new BoxLayout(l, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title); t.setFont(serif(Font.PLAIN, 34)); t.setForeground(INK); t.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setFont(sans(Font.PLAIN, 13)); sub.setForeground(MUTED); sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.add(t); l.add(Box.createVerticalStrut(2)); l.add(sub); h.add(l, BorderLayout.WEST);
        if (right != null) { JPanel r = new JPanel(new GridBagLayout()); r.setOpaque(false); r.add(right); h.add(r, BorderLayout.EAST); }
        return h;
    }

    static JLabel cap(String s) {
        JLabel l = new JLabel(s); l.setFont(sans(Font.BOLD, 12)); l.setForeground(MUTED); return l;
    }

    static JPanel labeled(String s, JComponent field) {
        JPanel c = new JPanel(new BorderLayout(0, 6)); c.setOpaque(false); c.add(cap(s), BorderLayout.NORTH); c.add(field); return c;
    }

    static Card stat(String caption, JLabel val, Color dot, boolean dark) {
        Card c = new Card(new BorderLayout(0, 6), dark ? FOREST : CARD, dark ? null : LINE, 24);
        c.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel l = new JLabel(caption, new Dot(dot, 9), SwingConstants.LEFT); l.setIconTextGap(8); l.setFont(sans(Font.BOLD, 12));
        l.setForeground(dark ? new Color(0xBFD0C4) : MUTED);
        val.setFont(serif(Font.PLAIN, 28)); val.setForeground(dark ? CREAM : INK);
        c.add(l, BorderLayout.NORTH); c.add(val); return c;
    }

    void go(int i) {
        String[] n = {"dash", "boarders", "settings"}; cl.show(cards, n[i]);
        for (int j = 0; j < navs.length; j++) navs[j].select(j == i);
    }

    JPanel sidebar() {
        Card s = new Card(new BorderLayout(0, 26), FOREST, null, 32) {         // soft arcs, echoing the curves of the mock-up
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
                g2.clip(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 32, 32));
                g2.setColor(new Color(255, 255, 255, 16)); g2.setStroke(new BasicStroke(1.4f));
                float w = getWidth(), h = getHeight();
                g2.draw(new Ellipse2D.Float(-w * 0.5f, h - w * 0.9f, w * 1.6f, w * 1.6f));
                g2.draw(new Ellipse2D.Float(-w * 0.2f, h - w * 0.55f, w * 1.1f, w * 1.1f));
                g2.dispose();
            }
        };
        s.setBorder(new EmptyBorder(28, 18, 22, 18)); s.setPreferredSize(new Dimension(236, 10));

        JPanel brand = new JPanel(); brand.setOpaque(false); brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(0, 8, 0, 0));
        JLabel tag = new JLabel("ADMIN CONSOLE"), name = new JLabel("<html>Boarding<br>House</html>");
        tag.setFont(sans(Font.BOLD, 10)); tag.setForeground(GOLD); tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        name.setFont(serif(Font.PLAIN, 30)); name.setForeground(CREAM); name.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(tag); brand.add(Box.createVerticalStrut(8)); brand.add(name);

        String[] labels = {"Dashboard", "Boarders", "Settings"};
        JPanel nav = new JPanel(new GridLayout(0, 1, 0, 6)), navWrap = new JPanel(new BorderLayout());
        nav.setOpaque(false); navWrap.setOpaque(false);
        for (int i = 0; i < 3; i++) {
            final int k = i; navs[i] = new Nav(labels[i]); navs[i].addActionListener(e -> go(k)); nav.add(navs[i]);
        }
        navWrap.add(nav, BorderLayout.NORTH);

        JPanel foot = new JPanel(); foot.setOpaque(false); foot.setLayout(new BoxLayout(foot, BoxLayout.Y_AXIS));
        foot.setBorder(new EmptyBorder(0, 8, 0, 0));
        JLabel date = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))), live = new JLabel("Live view \u00B7 refreshes every 15 s");
        date.setFont(serif(Font.PLAIN, 15)); date.setForeground(CREAM); date.setAlignmentX(Component.LEFT_ALIGNMENT);
        live.setFont(sans(Font.PLAIN, 11)); live.setForeground(new Color(0x9DB3A4)); live.setAlignmentX(Component.LEFT_ALIGNMENT);
        foot.add(date); foot.add(Box.createVerticalStrut(3)); foot.add(live);

        s.add(brand, BorderLayout.NORTH); s.add(navWrap); s.add(foot, BorderLayout.SOUTH);
        go(0); return s;
    }

    static void styleCombo(JComboBox<String> c) {
        c.setUI(new FlatCombo()); c.setOpaque(false); c.setBackground(Color.WHITE); c.setFont(sans(Font.PLAIN, 13));
        c.setBorder(new EmptyBorder(0, 0, 0, 0)); c.setPreferredSize(new Dimension(150, 38));
        c.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, v, i, sel, false);
                setBorder(new EmptyBorder(6, 14, 6, 8)); setFont(sans(Font.PLAIN, 13)); setForeground(INK);
                setOpaque(i >= 0); setBackground(sel ? SELECT : Color.WHITE); return this;
            }
        });
    }

    static void styleSpinner(JSpinner s) {
        s.setUI(new FlatSpin()); s.setOpaque(false); s.setBorder(new EmptyBorder(0, 0, 0, 0)); s.setPreferredSize(new Dimension(96, 38));
        s.setEditor(new JSpinner.NumberEditor(s, "#"));                      // 2026, not 2,026
        JSpinner.DefaultEditor ed = (JSpinner.DefaultEditor) s.getEditor(); ed.setOpaque(false);
        JFormattedTextField tf = ed.getTextField(); tf.setOpaque(false); tf.setBorder(new EmptyBorder(0, 14, 0, 2));
        tf.setFont(sans(Font.PLAIN, 13)); tf.setForeground(INK); tf.setHorizontalAlignment(SwingConstants.LEFT);
    }

    // ----------------------------------------------------------------- pages
    JPanel dashboard() {
        JPanel p = page(), ctl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ctl.setOpaque(false);
        Btn pay = new Btn("Record payment (in person)", 0); pay.addActionListener(e -> payDialog());
        styleCombo(mo); styleSpinner(yr);
        ctl.add(mo); ctl.add(yr); ctl.add(pay);
        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0)); stats.setOpaque(false);
        stats.add(stat("Paid", lPaid, new Color(0x4C9A68), false));
        stats.add(stat("Unpaid", lUnpaid, GOLD, false));
        stats.add(stat("Overdue", lOver, DANGER, false));
        stats.add(stat("Collected", lGot, SAGE, true));
        Btn bPaid = new Btn("Print paid", 1), bRcpt = new Btn("Print receipt", 1);
        bPaid.addActionListener(e -> printPaid()); bRcpt.addActionListener(e -> printReceipt());
        JPanel prow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); prow.setOpaque(false); prow.add(bRcpt); prow.add(bPaid);
        JPanel mid = new JPanel(new BorderLayout(0, 14)); mid.setOpaque(false); mid.add(prow, BorderLayout.NORTH); mid.add(stats);
        JPanel top = new JPanel(new BorderLayout(0, 18)); top.setOpaque(false);
        top.add(head("Dashboard", dSub, ctl), BorderLayout.NORTH); top.add(mid);
        p.add(top, BorderLayout.NORTH); p.add(tableCard(dt)); return p;
    }

    int ym(boolean month) { return month ? mo.getSelectedIndex() + 1 : (Integer) yr.getValue(); }
    static String fmt(Shared.Bill b, int k) {
        double a = b.amt(k); return Double.isNaN(a) ? "Awaiting reading" : Shared.peso(a) + (b.paid[k] ? "  (paid)" : "");
    }

    void refresh() {
        int y = ym(false), m = ym(true); Shared.tick(y, m);
        int sel = dt.getSelectedRow() >= 0 ? dIds.get(dt.getSelectedRow()) : -1;
        dm.setRowCount(0); dIds.clear(); int paid = 0, unpaid = 0, over = 0; double got = 0;
        for (Shared.Boarder b : Shared.d.boarders) {
            if (!b.active) continue; Shared.Bill bl = Shared.bill(b, y, m);
            if (bl.status.equals("Paid")) paid++; else if (bl.status.equals("Overdue")) over++; else unpaid++;
            for (int k = 0; k < 3; k++) if (bl.paid[k] && !Double.isNaN(bl.amt(k))) got += bl.amt(k);
            dIds.add(b.id);
            dm.addRow(new Object[]{b.name, b.room, fmt(bl, 0), fmt(bl, 1), fmt(bl, 2), Shared.peso(bl.total), bl.status});
        }
        int row = dIds.indexOf(sel); if (row >= 0) dt.setRowSelectionInterval(row, row);
        lPaid.setText(String.valueOf(paid)); lUnpaid.setText(String.valueOf(unpaid)); lOver.setText(String.valueOf(over));
        lGot.setText(Shared.peso(got)); lGot.setFont(fit(serif(Font.PLAIN, 28), lGot.getText()));
        dSub.setText("Billing period: " + Shared.MONTHS[m - 1] + " " + y);
        if (!bt.isEditing()) {                                               // don't wipe a cell being typed in
            loading = true; bm.setRowCount(0); bIds.clear();
            for (Shared.Boarder b : Shared.d.boarders)
                if (b.active) { bIds.add(b.id); bm.addRow(new Object[]{b.name, b.room, b.pin, b.rent, b.moveIn}); }
            loading = false;
        }
    }

    void payDialog() {
        int r = dt.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Click a boarder row first."); return; }
        int y = ym(false), m = ym(true), id = dIds.get(r); Shared.Bill bl = Shared.bill(Shared.find(id), y, m);
        if (bl.full) { JOptionPane.showMessageDialog(this, "Everything is already paid."); return; }
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6)); p.setBorder(new EmptyBorder(6, 6, 6, 6));
        JLabel ask = new JLabel("Tick what the boarder paid in person:"); ask.setFont(sans(Font.BOLD, 13)); p.add(ask);
        JCheckBox[] cb = new JCheckBox[3];
        for (int k : bl.unpaid) {
            boolean ok = !Double.isNaN(bl.amt(k));
            cb[k] = new JCheckBox(Shared.KEYS[k] + "   " + (ok ? Shared.peso(bl.amt(k)) : "(boarder has not submitted reading)")); cb[k].setEnabled(ok);
            cb[k].setOpaque(false); cb[k].setFocusPainted(false); p.add(cb[k]);
        }
        if (JOptionPane.showConfirmDialog(this, p, "Record payment", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        boolean[] items = new boolean[3]; for (int k = 0; k < 3; k++) items[k] = cb[k] != null && cb[k].isSelected();
        Shared.pay(id, y, m, items); refresh();
    }

    JPanel boarders() {
        JPanel p = page();
        String[] lbl = {"Name", "Room", "PIN", "Rent", "Move-in"}, def = {"", "", "", "2000", LocalDate.now().toString()};
        JPanel fields = new JPanel(new GridLayout(1, 5, 12, 0)), btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        fields.setOpaque(false); btns.setOpaque(false);
        for (int i = 0; i < 5; i++) { f[i] = new RField(def[i], 9); fields.add(labeled(lbl[i], f[i])); }
        Btn add = new Btn("Add boarder", 0), out = new Btn("Move out selected", 2), edit = new Btn("Edit selected", 1);
        add.addActionListener(e -> {
            double rent; try { rent = Double.parseDouble(f[3].getText()); } catch (Exception ex) { err("Rent must be a number."); return; }
            String er = Shared.addBoarder(f[0].getText().trim(), f[1].getText().trim(), f[2].getText().trim(), rent, f[4].getText().trim());
            if (er != null) { err(er); return; } f[0].setText(""); f[2].setText(""); refresh();
        });
        
        out.addActionListener(e -> {
            int r = bt.getSelectedRow();
            if (r >= 0 && JOptionPane.showConfirmDialog(this, "Mark this boarder as moved out?", "Move out", JOptionPane.YES_NO_OPTION) == 0) { Shared.moveOut(bIds.get(r)); refresh(); }
        });
        edit.addActionListener(e -> editDialog());
        btns.add(add); btns.add(edit); btns.add(out);
        Card form = new Card(new BorderLayout(0, 16), CARD, LINE, 26); form.setBorder(new EmptyBorder(18, 20, 18, 20));
        form.add(fields, BorderLayout.NORTH); form.add(btns, BorderLayout.SOUTH);
        JPanel top = new JPanel(new BorderLayout(0, 18)); top.setOpaque(false);
        top.add(head("Boarders", new JLabel("Add a new boarder, or double-click any cell in the table to edit it"), null), BorderLayout.NORTH);
        top.add(form);
        p.add(top, BorderLayout.NORTH); p.add(tableCard(bt)); return p;
    }

    void editDialog() {
        int r = bt.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Click a boarder row first."); return; }
        int id = bIds.get(r); Shared.Boarder b = Shared.find(id);
        String[] lbl = {"Name", "Room", "PIN", "Rent", "Move-in (YYYY-MM-DD)"};
        String[] old = {b.name, b.room, b.pin, String.valueOf(b.rent), String.valueOf(b.moveIn)};
        JTextField[] tf = new JTextField[5]; JPanel p = new JPanel(new GridLayout(0, 2, 8, 8)); p.setBorder(new EmptyBorder(6, 6, 6, 6));
        for (int i = 0; i < 5; i++) { tf[i] = new RField(old[i], 14); p.add(cap(lbl[i])); p.add(tf[i]); }
        if (JOptionPane.showConfirmDialog(this, p, "Edit boarder", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        for (int i = 0; i < 5; i++) {
            String v = tf[i].getText().trim();
            if (v.equals(old[i])) continue;                       // unchanged
            String er = Shared.updateBoarder(id, i, v);
            if (er != null) { err(lbl[i] + ": " + er); break; }
        }
        refresh();
    }

    JPanel settings() {
        JPanel p = page();
        String[] lbl = {"Default rent", "Water fee / month", "Due day of month", "Beds per room", "Electricity rate per kWh"};
        double[] val = {Shared.d.rent, Shared.d.water, Shared.d.dueDay, Shared.d.beds, Shared.d.rate};
        JPanel g = new JPanel(new GridLayout(0, 2, 18, 16)); g.setOpaque(false);
        for (int i = 0; i < 5; i++) { cfg[i] = new RField(String.valueOf(val[i]), 12); g.add(labeled(lbl[i], cfg[i])); }
        g.add(new JPanel() {{ setOpaque(false); }});
        Btn save = new Btn("Save settings", 0), cb = new Btn("Compute rate", 1);
        save.addActionListener(e -> {
            try {
                double[] v = new double[5]; for (int i = 0; i < 5; i++) v[i] = Double.parseDouble(cfg[i].getText());
                Shared.saveSettings(v[0], v[1], v[4], (int) v[2], (int) v[3]); JOptionPane.showMessageDialog(this, "Settings saved."); refresh();
            } catch (NumberFormatException ex) { err("All settings must be numbers."); }
        });
        cb.addActionListener(e -> {
            try { cfg[4].setText(String.format("%.4f", Double.parseDouble(tb.getText()) / Double.parseDouble(tk.getText()))); }
            catch (Exception ex) { err("Enter valid numbers."); }
        });
        JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); saveRow.setOpaque(false); saveRow.add(save);
        Card rates = new Card(new BorderLayout(0, 20), CARD, LINE, 26); rates.setBorder(new EmptyBorder(22, 24, 22, 24));
        rates.add(g); rates.add(saveRow, BorderLayout.SOUTH);

        JLabel ct = new JLabel("Compute rate from main electric bill"), cs = new JLabel("Divide the total bill by the total kWh, then save the result as the rate above.");
        ct.setFont(serif(Font.PLAIN, 20)); ct.setForeground(INK); cs.setFont(sans(Font.PLAIN, 12)); cs.setForeground(MUTED);
        JPanel ctxt = new JPanel(); ctxt.setOpaque(false); ctxt.setLayout(new BoxLayout(ctxt, BoxLayout.Y_AXIS));
        ct.setAlignmentX(Component.LEFT_ALIGNMENT); cs.setAlignmentX(Component.LEFT_ALIGNMENT);
        ctxt.add(ct); ctxt.add(Box.createVerticalStrut(3)); ctxt.add(cs);
        JPanel calcRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); calcRow.setOpaque(false);
        calcRow.add(cap("Total bill")); calcRow.add(tb); calcRow.add(cap("Total kWh")); calcRow.add(tk); calcRow.add(cb);
        Card calc = new Card(new BorderLayout(0, 16), CARD, LINE, 26); calc.setBorder(new EmptyBorder(20, 24, 20, 24));
        calc.add(ctxt, BorderLayout.NORTH); calc.add(calcRow);

        JPanel top = new JPanel(new BorderLayout(0, 18)), stack = new JPanel(new GridLayout(0, 1, 0, 16));
        top.setOpaque(false); stack.setOpaque(false); stack.add(rates); stack.add(calc);
        top.add(head("Settings", new JLabel("Rent, utilities and billing rules used for every boarder"), null), BorderLayout.NORTH);
        top.add(stack);
        p.add(top, BorderLayout.NORTH); return p;
    }

    // -------------------------------------------------------------- printing
    static String money(double v) { return Double.isNaN(v) ? "-" : Shared.peso(v); }
    static String today() { return LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy")); }
    /** Draws s with a font that can show every character (e.g. the peso sign); right = true aligns the text's right edge at x. */
    static void ptext(Graphics2D g, String s, Font f, int x, int y, boolean right) {
        g.setFont(fit(f, s)); g.drawString(s, right ? x - g.getFontMetrics().stringWidth(s) : x, y);
    }

    /** One report page (or more, when there are many rows): title, table of rows, totals and page numbers. */
    static class ReportPage implements Printable {
        final String title, sub, footL, footR; final List<String[]> rows;
        ReportPage(String title, String sub, List<String[]> rows, String footL, String footR) {
            this.title = title; this.sub = sub; this.rows = rows; this.footL = footL; this.footR = footR;
        }
        public int print(Graphics g0, PageFormat pf, int page) {
            int w = (int) pf.getImageableWidth(), h = (int) pf.getImageableHeight();
            int top = 64, rh = 22, foot = 56, per = Math.max(1, (h - top - rh - foot) / rh);
            int pages = Math.max(1, (rows.size() + per - 1) / per);
            if (page >= pages) return NO_SUCH_PAGE;
            Graphics2D g = (Graphics2D) g0.create(); aa(g);
            g.translate((int) pf.getImageableX(), (int) pf.getImageableY()); g.setColor(Color.BLACK);
            ptext(g, title, serif(Font.BOLD, 18), 0, 20, false);
            g.setColor(Color.DARK_GRAY); ptext(g, sub, sans(Font.PLAIN, 10), 0, 38, false);
            String[] cols = {"Name", "Room", "Rent", "Water", "Electricity", "Total"};
            double[] fr = {0.26, 0.09, 0.16, 0.16, 0.16, 0.17};
            int[] cx = new int[6], cw = new int[6]; int acc = 0;
            for (int i = 0; i < 6; i++) { cx[i] = acc; cw[i] = (int) (w * fr[i]); acc += cw[i]; }
            g.setColor(Color.BLACK);
            for (int i = 0; i < 6; i++) ptext(g, cols[i], sans(Font.BOLD, 10), i < 2 ? cx[i] + 4 : cx[i] + cw[i] - 4, top + 14, i >= 2);
            g.drawLine(0, top + rh - 4, w, top + rh - 4);
            int from = page * per, to = Math.min(rows.size(), from + per);
            for (int r = from; r < to; r++) {
                int base = top + rh + (r - from) * rh + 15;
                g.setColor(Color.BLACK);
                for (int i = 0; i < 6; i++) ptext(g, rows.get(r)[i], sans(i == 0 ? Font.BOLD : Font.PLAIN, 10), i < 2 ? cx[i] + 4 : cx[i] + cw[i] - 4, base, i >= 2);
                g.setColor(Color.LIGHT_GRAY); g.drawLine(0, base + 5, w, base + 5);
            }
            if (page == pages - 1) {                                            // totals on the last page
                int fy = top + rh + (to - from) * rh + 22;
                g.setColor(Color.BLACK); g.drawLine(0, fy - 14, w, fy - 14);
                ptext(g, footL, sans(Font.BOLD, 11), 0, fy, false); ptext(g, footR, sans(Font.BOLD, 11), w, fy, true);
            }
            g.setColor(Color.GRAY); String pn = "Page " + (page + 1) + " of " + pages;
            ptext(g, pn, sans(Font.PLAIN, 9), (w - g.getFontMetrics().stringWidth(pn)) / 2, h - 4, false);
            g.dispose(); return PAGE_EXISTS;
        }
    }

    /** One receipt page: title, label / value lines, then the total. */
    static class ReceiptPage implements Printable {
        final String title, sub, totalLabel, totalVal, note; final List<String[]> lines;
        ReceiptPage(String title, String sub, List<String[]> lines, String totalLabel, String totalVal, String note) {
            this.title = title; this.sub = sub; this.lines = lines; this.totalLabel = totalLabel; this.totalVal = totalVal; this.note = note;
        }
        public int print(Graphics g0, PageFormat pf, int page) {
            if (page > 0) return NO_SUCH_PAGE;
            int w = Math.min((int) pf.getImageableWidth(), 380);
            Graphics2D g = (Graphics2D) g0.create(); aa(g);
            g.translate((int) pf.getImageableX(), (int) pf.getImageableY()); g.setColor(Color.BLACK);
            ptext(g, title, serif(Font.BOLD, 20), 0, 24, false);
            g.setColor(Color.DARK_GRAY); ptext(g, sub, sans(Font.PLAIN, 10), 0, 42, false);
            g.setColor(Color.BLACK); g.drawLine(0, 54, w, 54);
            int y = 78;
            for (String[] l : lines) {
                g.setColor(Color.DARK_GRAY); ptext(g, l[0], sans(Font.PLAIN, 11), 0, y, false);
                g.setColor(Color.BLACK); ptext(g, l[1], sans(Font.PLAIN, 11), w, y, true);
                g.setColor(Color.LIGHT_GRAY); g.drawLine(0, y + 8, w, y + 8); y += 26;
            }
            g.setColor(Color.BLACK); y += 8;
            ptext(g, totalLabel, sans(Font.BOLD, 13), 0, y, false); ptext(g, totalVal, sans(Font.BOLD, 13), w, y, true);
            g.setColor(Color.DARK_GRAY); ptext(g, note, sans(Font.PLAIN, 10), 0, y + 28, false);
            g.dispose(); return PAGE_EXISTS;
        }
    }

    void sendToPrinter(Printable p, String jobName) {
        PrinterJob job = PrinterJob.getPrinterJob(); job.setJobName(jobName); job.setPrintable(p);
        if (!job.printDialog()) return;                                       // user pressed Cancel
        try { job.print(); } catch (PrinterException ex) { err("Could not print: " + ex.getMessage()); }
    }

    /** Prints a report of every boarder who has fully paid for the month shown on the Dashboard. */
    void printPaid() {
        int y = ym(false), m = ym(true); refresh();
        List<String[]> rows = new ArrayList<>(); double sum = 0;
        for (Shared.Boarder b : Shared.d.boarders) {
            if (!b.active) continue; Shared.Bill bl = Shared.bill(b, y, m);
            if (!bl.full) continue;
            rows.add(new String[]{b.name, b.room, money(bl.amt(0)), money(bl.amt(1)), money(bl.amt(2)), Shared.peso(bl.total)}); sum += bl.total;
        }
        String period = Shared.MONTHS[m - 1] + " " + y;
        if (rows.isEmpty()) { JOptionPane.showMessageDialog(this, "No paid boarders for " + period + "."); return; }
        sendToPrinter(new ReportPage("Paid boarders - " + period, "Printed " + today(), rows,
                rows.size() + (rows.size() == 1 ? " paid boarder" : " paid boarders"), "Collected: " + Shared.peso(sum)), "Paid boarders " + period);
    }

    /** Prints a receipt for the boarder selected in the Dashboard table (only when everything is paid). */
    void printReceipt() {
        int r = dt.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Click a boarder row first."); return; }
        int id = dIds.get(r), y = ym(false), m = ym(true); refresh();
        Shared.Boarder b = Shared.find(id); if (b == null) return;
        Shared.Bill bl = Shared.bill(b, y, m); String period = Shared.MONTHS[m - 1] + " " + y;
        if (!bl.full) { JOptionPane.showMessageDialog(this, b.name + " still has unpaid items for " + period + ". A receipt is only printed once everything is paid."); return; }
        int n = Shared.occupants(b, y, m).size(); String sh = n > 1 ? " (shared by " + n + ")" : "";
        List<String[]> lines = new ArrayList<>();
        lines.add(new String[]{"Boarder", b.name + " (Room " + b.room + ")"});
        lines.add(new String[]{"Rent" + sh, money(bl.amt(0))});
        lines.add(new String[]{"Water", money(bl.amt(1))});
        lines.add(new String[]{"Electricity" + sh, money(bl.amt(2))});
        sendToPrinter(new ReceiptPage("Payment receipt", "Boarding House - " + period, lines, "Total paid", Shared.peso(bl.total),
                "Status: Paid - Printed " + today()), "Receipt " + b.name + " " + period);
    }

    void err(String s) { JOptionPane.showMessageDialog(this, s, "Error", JOptionPane.ERROR_MESSAGE); }

    public static void main(String[] a) {
        System.setProperty("awt.useSystemAAFontSettings", "on"); System.setProperty("swing.aatext", "true");
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignore) { }
        theme();
        Shared.init(); Shared.load(); SwingUtilities.invokeLater(() -> new AdminApp().setVisible(true));
    }
}