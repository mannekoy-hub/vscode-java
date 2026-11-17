package DSAproject;

// ============================================================================
//  BUDGET OPTIMIZER  -  Java GUI VERSION (Swing)
//
//  Direct conversion of the Qt 6 version. The system logic (Expense class,
//  menu actions, validation rules and the 0/1 knapsack algorithm) is the same.
//
//  Mapping of the menu -> GUI:
//     1. Set Budget      -> "Set Budget" button
//     2. Add Expense     -> "Add Expense" button
//     3. View Expenses   -> the table (always visible)
//     4. Remove Expense  -> select a row, press "Remove Selected"
//     5. Update Expense  -> select a row, edit the boxes, press "Update Selected"
//     6. Optimize Budget -> "Optimize Budget" button
//     7. Budget Summary  -> "View Budget Summary" button
//     8. Exit            -> "Exit" button
//
//  Compile:  javac BudgetOptimizer.java
//  Run:      java BudgetOptimizer
// ============================================================================

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BudgetOptimizer extends JFrame {

    static class Expense {
        String name;
        int cost;
        int priority;
    }

    // ----------------------------- Data -------------------------------------
    private final List<Expense> expenses = new ArrayList<>();
    private int budget = 0;

    // ----------------------------- Widgets ----------------------------------
    private JTextField editBudget;
    private JLabel lblBudget;
    private JTextField editName;
    private JTextField editCost;
    private JTextField editPriority;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextArea result;

    private static final Color BLACK = Color.BLACK;
    private static final Color WHITE = Color.WHITE;
    private static final Font BASE_FONT = new Font("SansSerif", Font.PLAIN, 14);

    public BudgetOptimizer() {
        super("Budget Optimizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 760);
        setLocationRelativeTo(null);
        buildGui();
        updateBudgetLabel();
    }

    // ----------------------------- Helpers ----------------------------------

    // Safe integer parsing (no crash on letters / empty / huge numbers)
    // Returns null when the text is not a valid int.
    private static Integer parseInt(String raw) {
        String s = raw.trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Budget Optimizer", JOptionPane.WARNING_MESSAGE);
    }

    private void setResult(String text) {
        result.setText(text);
        result.setCaretPosition(0);
    }

    private void updateBudgetLabel() {
        lblBudget.setText("Current Budget: " + budget);
    }

    private void clearItemFields() {
        editName.setText("");
        editCost.setText("");
        editPriority.setText("");
    }

    private int selectedIndex() {
        return table.getSelectedRow();
    }

    // Rebuild the expense table (same as "View Expenses")
    private void refreshList() {
        tableModel.setRowCount(0);

        for (int i = 0; i < expenses.size(); i++) {
            tableModel.addRow(new Object[] {
                    String.valueOf(i + 1),
                    expenses.get(i).name,
                    String.valueOf(expenses.get(i).cost),
                    String.valueOf(expenses.get(i).priority)
            });
        }
    }

    // ----------------------------- Menu actions -----------------------------

    // 1. Set Budget
    private void doSetBudget() {
        Integer value = parseInt(editBudget.getText());
        if (value == null || value < 0) {
            budget = 0;
            updateBudgetLabel();
            showError("Invalid budget!");
            return;
        }
        budget = value;
        updateBudgetLabel();
        setResult("Budget set to " + budget);
    }

    // 2. Add Expense
    private void doAdd() {
        Expense item = new Expense();
        item.name = editName.getText().trim();

        Integer cost = parseInt(editCost.getText());
        Integer priority = parseInt(editPriority.getText());

        if (item.name.isEmpty() || cost == null || priority == null) {
            showError("Invalid item details!");
            return;
        }
        item.cost = cost;
        item.priority = priority;

        if (item.priority > 10) {
            showError("Priority should be 1 to 10 ");
            return;
        }

        if (item.cost <= 0 || item.priority <= 0) {
            showError("Invalid item details!");
        } else {
            expenses.add(item);
            refreshList();
            clearItemFields();
            setResult("Expense added successfully!");
        }
    }

    // 4. Remove Expense (select a row in the table)
    private void doRemove() {
        int index = selectedIndex();
        if (index >= 0 && index < expenses.size()) {
            expenses.remove(index);
            refreshList();
            clearItemFields();
            setResult("Expense removed!");
        } else {
            showError("Invalid expense number! Select an expense in the list first.");
        }
    }

    // 5. Update Expense (select a row, edit the boxes, press Update)
    private void doUpdate() {
        int index = selectedIndex();
        if (index >= 0 && index < expenses.size()) {
            Expense item = new Expense();
            item.name = editName.getText().trim();

            Integer cost = parseInt(editCost.getText());
            Integer priority = parseInt(editPriority.getText());

            if (item.name.isEmpty() || cost == null || priority == null
                    || cost <= 0 || priority <= 0) {
                showError("Invalid details! Please update again.");
                return;
            }
            item.cost = cost;
            item.priority = priority;

            if (item.priority > 10) {
                showError("Priority should be 1 to 10 ");
                return;
            }

            expenses.set(index, item);
            refreshList();
            table.setRowSelectionInterval(index, index);
            setResult("Expense updated!");
        } else {
            showError("Invalid expense number! Select an expense in the list first.");
        }
    }

    // 6. Optimize Budget
    private void doOptimize() {

        int n = expenses.size();

        if (budget <= 0 || n == 0) {
            showError("Set a budget and add expenses first.");
            return;
        }

        // Safety check so a huge budget cannot crash the program (memory)
        if ((long) (n + 1) * ((long) budget + 1L) > 30000000L) {
            showError("Budget is too large for this many expenses. Please use a smaller budget.");
            return;
        }

        try {

            //============ 1. DP Table Construction =============================//
            int[][] dp = new int[n + 1][budget + 1];
            //====================================================================//


            //============ 0/1 Knapsack Algorithm (DP Tabulation) ===============//
            for (int i = 1; i <= n; i++) {

                int cost = expenses.get(i - 1).cost;
                int priority = expenses.get(i - 1).priority;

                for (int w = 0; w <= budget; w++) {//checks  every amount of money from 0 to budget

                    dp[i][w] = dp[i - 1][w];

                    if (cost <= w) {

                        dp[i][w] = Math.max(
                                dp[i][w],
                                priority + dp[i - 1][w - cost]
                        );
                    }
                }
            }
            //====================================================================//


            //============ 3. Solution Reconstruction ============================//
            int w = budget;
            int totalCost = 0;

            List<Expense> selected = new ArrayList<>();

            for (int i = n; i > 0; i--) {

                if (dp[i][w] != dp[i - 1][w]) {

                    selected.add(expenses.get(i - 1));

                    totalCost += expenses.get(i - 1).cost;

                    w -= expenses.get(i - 1).cost;
                }
            }
            //====================================================================//


            //============ 4. Display Results ====================================//
            StringBuilder out = new StringBuilder();

            out.append("========== OPTIMIZATION RESULT ==========\n");

            out.append("Selected Expenses:\n");

            for (int i = 0; i < selected.size(); i++) {
                out.append(selected.get(i).name)
                   .append(" | Cost: ").append(selected.get(i).cost)
                   .append(" | Priority: ").append(selected.get(i).priority)
                   .append("\n");
            }

            out.append("\nTotal Cost: ").append(totalCost);
            out.append("\nRemaining Budget: ").append(budget - totalCost);
            out.append("\nTotal Priority: ").append(dp[n][budget]);

            out.append("\n=========================================\n");

            setResult(out.toString());
            //====================================================================//

        } catch (OutOfMemoryError e) {
            showError("Not enough memory. Please use a smaller budget.");
        }
    }

    // 7. View Budget Summary
    private void doSummary() {

        int total = 0;

        for (int i = 0; i < expenses.size(); i++) {
            total += expenses.get(i).cost;
        }

        StringBuilder out = new StringBuilder();
        out.append("========== BUDGET SUMMARY ==========\n");
        out.append("Available Budget: ").append(budget).append("\n");
        out.append("Total Expense Value: ").append(total).append("\n");
        out.append("Remaining Before Optimization: ").append(budget - total).append("\n");
        out.append("====================================\n");

        setResult(out.toString());
    }

    // 8. Exit
    private void doExit() {
        JOptionPane.showMessageDialog(this, "Thank you for using Budget Optimizer!",
                "Budget Optimizer", JOptionPane.INFORMATION_MESSAGE);
        dispose();
        System.exit(0);
    }

    // When a row is selected, load it into the boxes for easy updating
    private void onSelectionChanged() {
        int i = selectedIndex();
        if (i >= 0 && i < expenses.size()) {
            editName.setText(expenses.get(i).name);
            editCost.setText(String.valueOf(expenses.get(i).cost));
            editPriority.setText(String.valueOf(expenses.get(i).priority));
        }
    }

    // ----------------------------- GUI helpers ------------------------------

    // Same idea as QLineEdit::setMaxLength
    private static class LengthFilter extends DocumentFilter {
        private final int max;

        LengthFilter(int max) {
            this.max = max;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string == null) return;
            int room = max - fb.getDocument().getLength();
            if (room <= 0) return;
            if (string.length() > room) string = string.substring(0, room);
            super.insertString(fb, offset, string, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text == null) text = "";
            int room = max - (fb.getDocument().getLength() - length);
            if (room < 0) room = 0;
            if (text.length() > room) text = text.substring(0, room);
            super.replace(fb, offset, length, text, attrs);
        }
    }

    private JTextField makeField(int maxLength) {
        JTextField f = new JTextField();
        f.setFont(BASE_FONT);
        f.setBackground(WHITE);
        f.setForeground(BLACK);
        f.setCaretColor(BLACK);
        f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BLACK, 1), new EmptyBorder(4, 4, 4, 4)));
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new LengthFilter(maxLength));
        return f;
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BASE_FONT);
        l.setForeground(BLACK);
        return l;
    }

    private JButton makeButton(String text) {
        final JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setFont(BASE_FONT);
        b.setForeground(BLACK);
        b.setBackground(WHITE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BLACK, 1), new EmptyBorder(7, 16, 7, 16)));
        b.setCursor(Cursor.getDefaultCursor());

        // hover = #eeeeee, pressed = #dddddd, normal = white
        ChangeListener cl = e -> {
            if (b.getModel().isPressed() && b.getModel().isArmed()) {
                b.setBackground(new Color(0xdd, 0xdd, 0xdd));
            } else if (b.getModel().isRollover()) {
                b.setBackground(new Color(0xee, 0xee, 0xee));
            } else {
                b.setBackground(WHITE);
            }
        };
        b.addChangeListener(cl);
        return b;
    }

    private GridBagConstraints gbc(int x, double weightx) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = 0;
        c.weightx = weightx;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 6);
        return c;
    }

    // ----------------------------- GUI construction -------------------------
    private void buildGui() {

        // Clean look: white background, black letters and numbers
        UIManager.put("OptionPane.background", WHITE);
        UIManager.put("Panel.background", WHITE);
        UIManager.put("OptionPane.messageForeground", BLACK);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(WHITE);
        root.setBorder(new EmptyBorder(15, 20, 20, 20));
        setContentPane(root);

        // Title
        JLabel title = new JLabel("BUDGET OPTIMIZER");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(BLACK);

        // Budget row
        editBudget = makeField(10);
        editBudget.putClientProperty("placeholder", "Enter budget");
        JButton btnSetBudget = makeButton("Set Budget");
        lblBudget = makeLabel("");

        JPanel rowBudget = new JPanel(new GridBagLayout());
        rowBudget.setBackground(WHITE);
        rowBudget.add(makeLabel("Budget:"), gbc(0, 0));
        rowBudget.add(editBudget, gbc(1, 1));
        rowBudget.add(btnSetBudget, gbc(2, 0));
        GridBagConstraints cLbl = gbc(3, 2);
        cLbl.insets = new Insets(0, 20, 0, 0);
        rowBudget.add(lblBudget, cLbl);

        // Item row
        editName = makeField(60);
        editCost = makeField(10);
        editPriority = makeField(10);

        JPanel rowItem = new JPanel(new GridBagLayout());
        rowItem.setBackground(WHITE);
        rowItem.add(makeLabel("Item name:"), gbc(0, 0));
        rowItem.add(editName, gbc(1, 3));
        rowItem.add(makeLabel("Cost:"), gbc(2, 0));
        rowItem.add(editCost, gbc(3, 1));
        rowItem.add(makeLabel("Priority (1-10):"), gbc(4, 0));
        GridBagConstraints cPri = gbc(5, 1);
        cPri.insets = new Insets(0, 0, 0, 0);
        rowItem.add(editPriority, cPri);

        // Add / Update / Remove row
        JButton btnAdd = makeButton("Add Expense");
        JButton btnUpdate = makeButton("Update Selected");
        JButton btnRemove = makeButton("Remove Selected");

        JPanel rowEdit = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        rowEdit.setBackground(WHITE);
        rowEdit.add(btnAdd);
        rowEdit.add(btnUpdate);
        rowEdit.add(btnRemove);

        // Expense table
        tableModel = new DefaultTableModel(new Object[] {"#", "Item Name", "Cost", "Priority"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setFont(BASE_FONT);
        table.setForeground(BLACK);
        table.setBackground(WHITE);
        table.setGridColor(BLACK);
        table.setShowGrid(true);
        table.setRowHeight(26);
        table.setSelectionBackground(new Color(0xd0, 0xd0, 0xd0));
        table.setSelectionForeground(BLACK);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(500);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getTableHeader().setReorderingAllowed(false);

        // header look: white background, black bold text, black border
        DefaultTableCellRenderer headRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        t, value, false, false, row, column);
                l.setBackground(WHITE);
                l.setForeground(BLACK);
                l.setFont(new Font("SansSerif", Font.BOLD, 14));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BLACK, 1), new EmptyBorder(4, 4, 4, 4)));
                return l;
            }
        };
        for (int c = 0; c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setHeaderRenderer(headRenderer);
        }

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(new LineBorder(BLACK, 1));
        tableScroll.getViewport().setBackground(WHITE);

        // Action row
        JButton btnOptimize = makeButton("Optimize Budget");
        JButton btnSummary = makeButton("View Budget Summary");
        JButton btnExit = makeButton("Exit");

        JPanel rowActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        rowActions.setBackground(WHITE);
        rowActions.add(btnOptimize);
        rowActions.add(btnSummary);
        rowActions.add(btnExit);

        // Result box (read-only)
        result = new JTextArea();
        result.setEditable(false);
        result.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        result.setBackground(WHITE);
        result.setForeground(BLACK);
        result.setBorder(new EmptyBorder(4, 4, 4, 4));

        JScrollPane resultScroll = new JScrollPane(result);
        resultScroll.setBorder(new LineBorder(BLACK, 1));

        // Main layout
        GridBagConstraints m = new GridBagConstraints();
        m.gridx = 0;
        m.fill = GridBagConstraints.HORIZONTAL;
        m.weightx = 1;
        m.anchor = GridBagConstraints.WEST;
        m.insets = new Insets(0, 0, 12, 0);

        m.gridy = 0; m.weighty = 0; root.add(title, m);
        m.gridy = 1; m.weighty = 0; root.add(rowBudget, m);
        m.gridy = 2; m.weighty = 0; root.add(rowItem, m);
        m.gridy = 3; m.weighty = 0; root.add(rowEdit, m);

        m.gridy = 4; m.weighty = 3; m.fill = GridBagConstraints.BOTH;
        root.add(tableScroll, m);

        m.gridy = 5; m.weighty = 0; m.fill = GridBagConstraints.HORIZONTAL;
        root.add(rowActions, m);

        m.gridy = 6; m.weighty = 2; m.fill = GridBagConstraints.BOTH;
        m.insets = new Insets(0, 0, 0, 0);
        root.add(resultScroll, m);

        // Connections
        btnSetBudget.addActionListener(e -> doSetBudget());
        btnAdd.addActionListener(e -> doAdd());
        btnUpdate.addActionListener(e -> doUpdate());
        btnRemove.addActionListener(e -> doRemove());
        btnOptimize.addActionListener(e -> doOptimize());
        btnSummary.addActionListener(e -> doSummary());
        btnExit.addActionListener(e -> doExit());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });

        // Press Enter in a box = same as its button
        editBudget.addActionListener(e -> doSetBudget());
        editPriority.addActionListener(e -> doAdd());

        // Placeholder texts (grey hints like QLineEdit::setPlaceholderText)
        installPlaceholder(editBudget, "Enter budget");
        installPlaceholder(editName, "Item name");
        installPlaceholder(editCost, "Cost");
        installPlaceholder(editPriority, "1 - 10");
    }

    // Draws grey hint text while a box is empty
    private void installPlaceholder(final JTextField field, final String hint) {
        // Replace the field's painting with one that also draws the hint
        field.setUI(new javax.swing.plaf.basic.BasicTextFieldUI() {
            @Override
            protected void paintSafely(Graphics g) {
                super.paintSafely(g);
                if (field.getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(0x80, 0x80, 0x80));
                    g2.setFont(field.getFont());
                    Insets in = field.getInsets();
                    FontMetrics fm = g2.getFontMetrics();
                    int y = (field.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(hint, in.left, y);
                    g2.dispose();
                }
            }
        });
        field.setBackground(WHITE);
        field.setForeground(BLACK);
        field.setCaretColor(BLACK);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            BudgetOptimizer window = new BudgetOptimizer();
            window.setVisible(true);
        });
    }
}