package DSAproject;

// ============================================================================
//  STUDENT BUDGET OPTIMIZER  -  Java GUI VERSION (Swing)  -  Dashboard edition
//
//  Student edition. The system logic (Expense class, validation rules,
//  0/1 knapsack, two-pointer pair finder) is unchanged.
//  Counting sort was removed and the SLIDING WINDOW algorithm was added.
//  Data is saved in a SQLite database (budget.db) through BudgetDatabase.java.
//
//  Screens:
//     Dashboard  -> set the budget, daily spending (days the budget lasts),
//                   live stats, budget usage, priority spread
//     Expenses   -> add / update / remove expenses, the expense table
//     Optimizer  -> Optimize Budget, Budget Summary, Spending Window (sliding
//                   window), Find Pair by Cost, and the results panel
//
//  Print     -> the "Print Report" button (sidebar) prints the whole report
//
//  Needs lib/sqlite-jdbc-3.54.1.0.jar and BudgetDatabase.java in the same package.
// ============================================================================

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BudgetOptimizer extends JFrame {

    static class Expense {
        int id;
        String name;
        int cost;
        int priority;
    }

    // ----------------------------- Data -------------------------------------
    private final List<Expense> expenses = new ArrayList<>();
    private int budget = 0;

    // Database state: dbReady = tables are ready, dbLoading = startup load in progress
    private boolean dbReady = false;
    private boolean dbLoading = false;

    // ----------------------------- Widgets ----------------------------------
    private JTextField editBudget;
    private JLabel lblBudget;
    private JTextField editName;
    private JTextField editCost;
    private JTextField editPriority;
    private JTextField editDaily;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextArea result;

    private JLabel statusLabel;
    private JLabel lblStatBudget;
    private JLabel lblStatTotal;
    private JLabel lblStatRemaining;
    private JLabel lblStatCount;
    private JLabel lblStatDays;
    private JTextArea txtRunway;

    private CardLayout cards;
    private JPanel pages;
    private final Map<String, JToggleButton> navButtons = new LinkedHashMap<>();

    private HeroPanel hero;
    private GaugePanel gauge;
    private PriorityChart chart;

    private PillButton btnSetBudget;
    private PillButton btnRunOptimizer;
    private PillButton btnAdd;
    private PillButton btnUpdate;
    private PillButton btnRemove;
    private PillButton btnRemoveAll;
    private PillButton btnOptimize;
    private PillButton btnSummary;
    private PillButton btnWindow;
    private PillButton btnPair;
    private PillButton btnPrint;
    private PillButton btnExit;

    // ----------------------------- Palette (from the flyer) ------------------
    private static final Color NAVY = new Color(0x13202F);
    private static final Color NAVY_2 = new Color(0x1C2D40);
    private static final Color GREEN = new Color(0x1B9A4A);
    private static final Color GREEN_DK = new Color(0x137A39);
    private static final Color GREEN_LT = new Color(0xDDF2E4);
    private static final Color GOLD = new Color(0xE0A42B);
    private static final Color INK = new Color(0x15181C);
    private static final Color MUTED = new Color(0x66707A);
    private static final Color PAPER = new Color(0xF2F4F3);
    private static final Color LINE = new Color(0xDCE2DE);
    private static final Color RED = new Color(0xC4392D);
    private static final Color WHITE = Color.WHITE;
    private static final Color NAV_MUTED = new Color(0xA9B6C3);
    private static final Font BASE_FONT = new Font("SansSerif", Font.PLAIN, 14);

    public BudgetOptimizer() {
        super("Student Budget Optimizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1040, 740));
        setLocationRelativeTo(null);
        buildGui();
        loadFromDatabase();
        updateBudgetLabel();
        refreshList();
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
        JOptionPane.showMessageDialog(this, msg, "Student Budget Optimizer", JOptionPane.WARNING_MESSAGE);
    }

    private void setResult(String text) {
        result.setText(text);
        result.setCaretPosition(0);
        String first = text.trim();
        statusLabel.setText(first.indexOf('\n') < 0 ? first : "Results updated. Open the Optimizer page to read them.");
    }

    private void updateBudgetLabel() {
        lblBudget.setText(String.valueOf(budget));
        refreshDashboard();
    }

    private void clearItemFields() {
        editName.setText("");
        editCost.setText("");
        editPriority.setText("");
    }

    private int selectedIndex() {
        return table.getSelectedRow();
    }

    // ----------------------------- Database ---------------------------------

    // Startup: create the tables if needed, then load budget, daily spending and expenses
    private void loadFromDatabase() {
        try {
            BudgetDatabase.init();
            dbLoading = true;
            budget = BudgetDatabase.loadBudget();
            expenses.clear();
            expenses.addAll(BudgetDatabase.loadExpenses());
            if (budget > 0) editBudget.setText(String.valueOf(budget));
            int daily = BudgetDatabase.loadDaily();
            if (daily > 0) editDaily.setText(String.valueOf(daily));
            dbReady = true;
        } catch (java.sql.SQLException e) {
            showError("Database error: " + e.getMessage());
        } finally {
            dbLoading = false;
        }
    }

    // Saves the daily spending box (runs every time the box changes)
    private void saveDailyToDatabase() {
        if (!dbReady || dbLoading) return;
        Integer d = parseInt(editDaily.getText());
        try {
            BudgetDatabase.saveDaily(d == null || d < 0 ? 0 : d);
        } catch (java.sql.SQLException e) {
            showError("Database error: " + e.getMessage());
        }
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
        refreshDashboard();
    }

    // Updates every number and picture on the dashboard
    private void refreshDashboard() {
        if (lblStatBudget == null || hero == null) return;

        long total = 0;
        for (int i = 0; i < expenses.size(); i++) {
            total += expenses.get(i).cost;
        }
        long remaining = (long) budget - total;

        lblStatBudget.setText(String.valueOf(budget));
        lblStatTotal.setText(String.valueOf(total));
        lblStatRemaining.setText(String.valueOf(remaining));
        lblStatRemaining.setForeground(remaining < 0 ? RED : INK);
        lblStatCount.setText(String.valueOf(expenses.size()));
        updateRunway();

        hero.repaint();
        gauge.repaint();
        chart.repaint();
    }

    // How many days the budget lasts at a fixed daily spending
    private void updateRunway() {
        if (lblStatDays == null || txtRunway == null || editDaily == null) return;

        Integer daily = parseInt(editDaily.getText());
        lblStatDays.setForeground(INK);

        if (budget <= 0) {
            lblStatDays.setText("--");
            txtRunway.setText("Set a budget first.");
        } else if (daily == null || daily <= 0) {
            lblStatDays.setText("--");
            txtRunway.setText("Enter how much you spend each day.");
        } else {
            int days = budget / daily;
            int left = budget % daily;
            lblStatDays.setText(String.valueOf(days));
            if (days == 0) {
                lblStatDays.setForeground(RED);
                txtRunway.setText("Your budget is less than one day of spending.");
            } else if (left == 0) {
                txtRunway.setText(days + (days == 1 ? " day" : " days") + ", nothing left over.");
            } else {
                txtRunway.setText(days + (days == 1 ? " day" : " days") + " + " + left + " left over.");
            }
        }
    }

    private void showPage(String key) {
        cards.show(pages, key);
        JToggleButton b = navButtons.get(key);
        if (b != null) b.setSelected(true);
    }

    // ----------------------------- Menu actions -----------------------------

    // 1. Set Budget
    private void doSetBudget() {
        Integer value = parseInt(editBudget.getText());
        if (value == null || value < 0) {
            budget = 0;
            if (dbReady) {
                try {
                    BudgetDatabase.saveBudget(0);
                } catch (java.sql.SQLException e) {
                    showError("Database error: " + e.getMessage());
                }
            }
            updateBudgetLabel();
            showError("Invalid budget!");
            return;
        }
        if (dbReady) {
            try {
                BudgetDatabase.saveBudget(value);
            } catch (java.sql.SQLException e) {
                showError("Database error: " + e.getMessage());
                return;
            }
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
            if (dbReady) {
                try {
                    BudgetDatabase.insertExpense(item);
                } catch (java.sql.SQLException e) {
                    showError("Database error: " + e.getMessage());
                    return;
                }
            }
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
            if (dbReady) {
                try {
                    BudgetDatabase.deleteExpense(expenses.get(index).id);
                } catch (java.sql.SQLException e) {
                    showError("Database error: " + e.getMessage());
                    return;
                }
            }
            expenses.remove(index);
            refreshList();
            clearItemFields();
            setResult("Expense removed!");
        } else {
            showError("Invalid expense number! Select an expense in the list first.");
        }
    }

    // 4b. Remove All Expenses (asks for confirmation first)
    private void doRemoveAll() {
        if (expenses.isEmpty()) {
            showError("There are no expenses to remove.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Remove all " + expenses.size() + " expenses? This cannot be undone.",
                "Remove All", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;

        if (dbReady) {
            try {
                BudgetDatabase.deleteAllExpenses();
            } catch (java.sql.SQLException e) {
                showError("Database error: " + e.getMessage());
                return;
            }
        }
        expenses.clear();
        refreshList();
        clearItemFields();
        setResult("All expenses removed!");
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

            item.id = expenses.get(index).id;
            if (dbReady) {
                try {
                    BudgetDatabase.updateExpense(item);
                } catch (java.sql.SQLException e) {
                    showError("Database error: " + e.getMessage());
                    return;
                }
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

    // ========================================================================
    // ##################  ALGORITHM 2: SLIDING WINDOW  #######################
    // ========================================================================
    // Looks at every group of "size" expenses in a row (in the order they were
    // added, like a week of spending). The window total is updated in O(1) when
    // the window moves: add the expense that enters, subtract the one that leaves.

    // Writes the expenses of one window (start .. start + size - 1) into the result text
    private void appendWindow(StringBuilder out, int start, int size) {
        for (int i = start; i < start + size; i++) {
            out.append(i + 1).append(". ").append(expenses.get(i).name)
               .append(" | Cost: ").append(expenses.get(i).cost)
               .append(" | Priority: ").append(expenses.get(i).priority)
               .append("\n");
        }
    }

    // Button action: "Spending Window"  (uses Algorithm 2)
    private void doSlidingWindow() {
        int n = expenses.size();
        if (n < 2) {
            showError("Add at least 2 expenses first.");
            return;
        }

        String input = JOptionPane.showInputDialog(this,
                "Enter the window size (how many expenses in a row, 1 to " + n + "):",
                "Spending Window", JOptionPane.QUESTION_MESSAGE);
        if (input == null) return;   // user pressed Cancel

        Integer k = parseInt(input);
        if (k == null || k < 1 || k > n) {
            showError("Invalid window size! Enter a number from 1 to " + n + ".");
            return;
        }

        // First window: expenses 0 .. k-1
        long windowSum = 0;
        for (int i = 0; i < k; i++) {
            windowSum += expenses.get(i).cost;
        }

        long maxSum = windowSum;
        int maxStart = 0;
        long minSum = windowSum;
        int minStart = 0;

        // Slide the window one expense at a time
        for (int i = k; i < n; i++) {
            windowSum += (long) expenses.get(i).cost - expenses.get(i - k).cost;
            int start = i - k + 1;

            if (windowSum > maxSum) {
                maxSum = windowSum;
                maxStart = start;
            }
            if (windowSum < minSum) {
                minSum = windowSum;
                minStart = start;
            }
        }

        StringBuilder out = new StringBuilder();
        out.append("====== SLIDING WINDOW: SPENDING CHECK ======\n");
        out.append("Window size: ").append(k).append(" expense(s) in a row\n");
        out.append("Windows checked: ").append(n - k + 1).append("\n\n");

        out.append("Heaviest spending window:\n");
        appendWindow(out, maxStart, k);
        out.append("Window total: ").append(maxSum).append("\n");
        if (budget > 0) {
            if (maxSum > budget) {
                out.append("This is ").append(maxSum - budget).append(" over your budget.\n");
            } else {
                out.append("This fits your budget (").append(budget - maxSum).append(" left).\n");
            }
        }

        out.append("\nLightest spending window:\n");
        appendWindow(out, minStart, k);
        out.append("Window total: ").append(minSum).append("\n");
        out.append("============================================\n");

        setResult(out.toString());
    }
    // ##################  END OF ALGORITHM 2: SLIDING WINDOW  ################


    // ========================================================================
    // ############  ALGORITHM 3: TWO-POINTER PAIR FINDER  ####################
    // ========================================================================

    private List<Expense> sortByCostCopy(List<Expense> list) {
        List<Expense> copy = new ArrayList<>(list);
        for (int i = 1; i < copy.size(); i++) {
            Expense key = copy.get(i);
            int j = i - 1;
            while (j >= 0 && copy.get(j).cost > key.cost) {
                copy.set(j + 1, copy.get(j));
                j--;
            }
            copy.set(j + 1, key);
        }
        return copy;
    }

    // Button action: "Find Pair by Cost"  (uses Algorithm 3)
    private void doFindPair() {
        if (expenses.size() < 2) {
            showError("Add at least 2 expenses first.");
            return;
        }

        String input = JOptionPane.showInputDialog(this,
                "Enter the total amount the two expenses should add up to:",
                "Find Pair by Cost", JOptionPane.QUESTION_MESSAGE);
        if (input == null) return;   // user pressed Cancel

        Integer target = parseInt(input);
        if (target == null || target <= 0) {
            showError("Invalid amount!");
            return;
        }

        List<Expense> sorted = sortByCostCopy(expenses);

        int left = 0;                    // pointer on the cheapest expense
        int right = sorted.size() - 1;   // pointer on the most expensive expense
        Expense foundA = null;
        Expense foundB = null;

        while (left < right) {
            long sum = (long) sorted.get(left).cost + sorted.get(right).cost;

            if (sum == target) {         // match: pair found
                foundA = sorted.get(left);
                foundB = sorted.get(right);
                break;
            } else if (sum < target) {   // too small: move left pointer up
                left++;
            } else {                     // too big: move right pointer down
                right--;
            }
        }

        StringBuilder out = new StringBuilder();
        out.append("====== TWO-POINTER: PAIR FINDER ======\n");
        out.append("Target amount: ").append(target).append("\n\n");

        if (foundA != null) {
            out.append("Pair found:\n");
            out.append(foundA.name).append(" | Cost: ").append(foundA.cost).append("\n");
            out.append(foundB.name).append(" | Cost: ").append(foundB.cost).append("\n");
            out.append("\nTotal: ").append(foundA.cost + foundB.cost).append("\n");
        } else {
            out.append("No two expenses add up to ").append(target).append(".\n");
        }
        out.append("======================================\n");

        setResult(out.toString());
    }
    // ############  END OF ALGORITHM 3: TWO-POINTER PAIR FINDER  #############

    // ========================================================================
    //  PRINT REPORT  (one click: opens the printer dialog, prints everything)
    // ========================================================================

    // Builds the text lines of the report: summary, daily spending, expenses, latest result
    private List<String> buildReportLines() {
        List<String> lines = new ArrayList<>();
        String sep = "------------------------------------------------------------";

        long total = 0;
        for (int i = 0; i < expenses.size(); i++) {
            total += expenses.get(i).cost;
        }

        lines.add("Student Budget Optimizer - Budget Report");
        lines.add("Date: " + java.time.LocalDate.now());
        lines.add("");
        lines.add("SUMMARY");
        lines.add(sep);
        lines.add("Budget:           " + budget);
        lines.add("Total expenses:   " + total);
        lines.add("Remaining:        " + ((long) budget - total));

        Integer daily = parseInt(editDaily.getText());
        if (budget > 0 && daily != null && daily > 0) {
            lines.add("Daily spending:   " + daily);
            lines.add("Budget lasts:     " + (budget / daily) + " day(s), "
                    + (budget % daily) + " left over");
        }

        lines.add("");
        lines.add("EXPENSES (" + expenses.size() + ")");
        lines.add(sep);
        if (expenses.isEmpty()) {
            lines.add("No expenses added.");
        } else {
            lines.add(String.format("%-4s%-32s%12s%10s", "#", "Item", "Cost", "Priority"));
            for (int i = 0; i < expenses.size(); i++) {
                Expense e = expenses.get(i);
                String name = e.name.length() > 30 ? e.name.substring(0, 27) + "..." : e.name;
                lines.add(String.format("%-4d%-32s%12d%10d", i + 1, name, e.cost, e.priority));
            }
        }

        String text = result.getText().trim();
        if (!text.isEmpty() && !text.startsWith("Choose an action")) {
            lines.add("");
            lines.add("LATEST RESULT");
            lines.add(sep);
            String[] parts = text.split("\n");
            for (int i = 0; i < parts.length; i++) {
                lines.add(parts[i]);
            }
        }
        return lines;
    }

    // Button action: "Print Report"
    private void doPrint() {
        final List<String> lines = buildReportLines();

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Student Budget Report");
        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics g, PageFormat pf, int pageIndex) {
                Graphics2D g2 = (Graphics2D) g;
                g2.translate(pf.getImageableX(), pf.getImageableY());
                g2.setColor(Color.BLACK);

                Font body = new Font(Font.MONOSPACED, Font.PLAIN, 10);
                Font title = new Font(Font.SANS_SERIF, Font.BOLD, 14);
                int lineH = g2.getFontMetrics(body).getHeight() + 2;
                int perPage = Math.max(1, (int) (pf.getImageableHeight() / lineH));
                int pages = (lines.size() + perPage - 1) / perPage;
                if (pageIndex >= pages) return NO_SUCH_PAGE;

                int first = pageIndex * perPage;
                int last = Math.min(lines.size(), first + perPage);
                for (int i = first; i < last; i++) {
                    g2.setFont(i == 0 ? title : body);
                    g2.drawString(lines.get(i), 0, (i - first + 1) * lineH);
                }
                return PAGE_EXISTS;
            }
        });

        if (job.printDialog()) {   // false when the user presses Cancel
            try {
                job.print();
                statusLabel.setText("Report sent to the printer.");
            } catch (PrinterException e) {
                showError("Could not print: " + e.getMessage());
            }
        }
    }

    // 8. Exit
    private void doExit() {
        JOptionPane.showMessageDialog(this, "Thank you for using Student Budget Optimizer!",
                "Student Budget Optimizer", JOptionPane.INFORMATION_MESSAGE);
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

    // ========================================================================
    //  GUI CODE (everything below is presentation only)
    // ========================================================================

    private static Font font(int style, int size) {
        return new Font("SansSerif", style, size);
    }

    private static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    private static Color mix(Color a, Color b, float t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }

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

    // ----------------------------- Custom widgets ---------------------------

    // Rounded button with hover / pressed states
    private static class PillButton extends JButton {
        enum Kind { PRIMARY, GOLD, DARK, GHOST, DANGER }

        private final Kind kind;

        PillButton(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setFont(font(Font.BOLD, 13));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 18, 10, 18));
            switch (kind) {
                case GOLD:   setForeground(INK);   break;
                case DANGER: setForeground(RED);   break;
                default:     setForeground(WHITE); break;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth();
            int h = getHeight();
            boolean pressed = getModel().isPressed() && getModel().isArmed();
            boolean hover = getModel().isRollover();

            Color bg = null;
            Color line = null;
            switch (kind) {
                case PRIMARY:
                    bg = pressed ? GREEN_DK : (hover ? mix(GREEN, WHITE, 0.14f) : GREEN);
                    break;
                case GOLD:
                    bg = pressed ? mix(GOLD, Color.BLACK, 0.12f) : (hover ? mix(GOLD, WHITE, 0.18f) : GOLD);
                    break;
                case DARK:
                    bg = pressed ? Color.BLACK : (hover ? mix(NAVY, WHITE, 0.16f) : NAVY);
                    break;
                case GHOST:
                    bg = pressed ? new Color(255, 255, 255, 70) : (hover ? new Color(255, 255, 255, 40) : null);
                    line = new Color(255, 255, 255, 190);
                    break;
                case DANGER:
                    bg = pressed ? new Color(0xF6D6D2) : (hover ? new Color(0xFBECEA) : WHITE);
                    line = RED;
                    break;
            }
            if (bg != null) {
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 16, 16);
            }
            if (line != null) {
                g2.setColor(line);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 15, 15);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Sidebar navigation button (draws its own icon + text)
    private static class NavButton extends JToggleButton {
        private final int iconKind;

        NavButton(String text, int iconKind) {
            super(text);
            this.iconKind = iconKind;
            setFont(font(Font.BOLD, 14));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(180, 46);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth();
            int h = getHeight();
            boolean sel = isSelected();
            if (sel) {
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(GOLD);
                g2.fillRoundRect(0, 10, 5, h - 20, 5, 5);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
            }
            Color fg = sel ? WHITE : NAV_MUTED;
            drawNavIcon(g2, iconKind, 20, (h - 18) / 2, fg);
            g2.setColor(fg);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), 54, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }

        private static void drawNavIcon(Graphics2D g2, int kind, int x, int y, Color c) {
            g2.setColor(c);
            if (kind == 0) {            // dashboard: four tiles
                g2.fillRoundRect(x, y, 8, 8, 3, 3);
                g2.fillRoundRect(x + 10, y, 8, 8, 3, 3);
                g2.fillRoundRect(x, y + 10, 8, 8, 3, 3);
                g2.fillRoundRect(x + 10, y + 10, 8, 8, 3, 3);
            } else if (kind == 1) {     // expenses: list
                for (int i = 0; i < 3; i++) {
                    int yy = y + i * 6 + 1;
                    g2.fillRect(x, yy, 3, 3);
                    g2.fillRoundRect(x + 6, yy, 12, 3, 2, 2);
                }
            } else {                    // optimizer: target
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x + 1, y + 1, 16, 16);
                g2.fillOval(x + 6, y + 6, 6, 6);
            }
        }
    }

    // Brand mark
    private static class Logo extends JComponent {
        Logo() {
            setPreferredSize(new Dimension(44, 44));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(GREEN);
            g2.fillRoundRect(0, 0, 44, 44, 14, 14);
            g2.setColor(WHITE);
            g2.fillRoundRect(9, 27, 7, 8, 3, 3);
            g2.fillRoundRect(19, 22, 7, 13, 3, 3);
            g2.fillRoundRect(29, 17, 7, 18, 3, 3);
            g2.setColor(GOLD);
            g2.fillOval(29, 6, 8, 8);
            g2.dispose();
        }
    }

    // White rounded panel with a soft shadow and an optional colored edge
    private static class Card extends JPanel {
        private final Color fill;
        private final Color accent;

        Card(LayoutManager lm, Color fill, Color accent) {
            super(lm);
            this.fill = fill;
            this.accent = accent;
            setOpaque(false);
            setBorder(new EmptyBorder(16, 20, 19, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth();
            int h = getHeight() - 3;
            g2.setColor(new Color(0, 0, 0, 16));
            g2.fillRoundRect(0, 3, w, h, 20, 20);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, 20, 20);
            if (fill.equals(WHITE)) {
                g2.setColor(LINE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);
            }
            if (accent != null) {
                Area bar = new Area(new RoundRectangle2D.Float(0, 0, w, h, 20, 20));
                bar.intersect(new Area(new Rectangle2D.Float(0, 0, 6, h)));
                g2.setColor(accent);
                g2.fill(bar);
            }
            g2.dispose();
        }
    }

    // Small status dot
    private static class DotIcon implements Icon {
        private final Color color;

        DotIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = aa(g);
            g2.setColor(color);
            g2.fillOval(x, y, 10, 10);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 10;
        }

        @Override
        public int getIconHeight() {
            return 10;
        }
    }

    // Left sidebar background: navy with the flyer's green + gold shapes
    private static class Sidebar extends JPanel {
        Sidebar() {
            super(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(236, 100));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, new Color(0x18283A), 0, h, new Color(0x0D1620)));
            g2.fillRect(0, 0, w, h);

            g2.setColor(GOLD);
            g2.fillOval(-62, h - 262, 124, 124);

            int cx = (int) (w * 0.45);
            int cy = h + 40;
            g2.setColor(GREEN);
            g2.fillOval(cx - 210, cy - 210, 420, 420);
            g2.setColor(new Color(0xE0, 0xA4, 0x2B, 170));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - 224, cy - 224, 448, 448);
            g2.dispose();
        }
    }

    // Table cell renderer: zebra rows + colored priority pill
    private static class CellRenderer extends JLabel implements TableCellRenderer {
        private Color rowBg = WHITE;
        private Color pill = null;

        CellRenderer() {
            setOpaque(false);
            setFont(BASE_FONT);
            setBorder(new EmptyBorder(0, 14, 0, 14));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String text = value == null ? "" : value.toString();
            setText(text);
            rowBg = isSelected ? GREEN_LT : (row % 2 == 0 ? WHITE : new Color(0xF7F9F8));
            pill = null;
            setForeground(INK);
            setFont(BASE_FONT);
            setHorizontalAlignment(column == 0 || column == 3 ? SwingConstants.CENTER : SwingConstants.LEFT);

            if (column == 0) {
                setForeground(MUTED);
            } else if (column == 1) {
                setFont(font(Font.BOLD, 14));
            } else if (column == 3) {
                int p = 0;
                try {
                    p = Integer.parseInt(text);
                } catch (NumberFormatException ignored) {
                }
                if (p >= 8) {
                    pill = GREEN;
                    setForeground(WHITE);
                } else if (p >= 5) {
                    pill = GOLD;
                    setForeground(INK);
                } else {
                    pill = new Color(0xE3E8E5);
                    setForeground(INK);
                }
                setFont(font(Font.BOLD, 13));
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(rowBg);
            g2.fillRect(0, 0, w, h);
            if (pill != null) {
                FontMetrics fm = g2.getFontMetrics(getFont());
                int pw = Math.max(44, fm.stringWidth(getText()) + 22);
                int ph = 24;
                g2.setColor(pill);
                g2.fillRoundRect((w - pw) / 2, (h - ph) / 2, pw, ph, 24, 24);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class HeaderRenderer extends JLabel implements TableCellRenderer {
        HeaderRenderer() {
            setOpaque(true);
            setBackground(NAVY);
            setForeground(WHITE);
            setFont(font(Font.BOLD, 13));
            setBorder(new EmptyBorder(0, 14, 0, 14));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            setHorizontalAlignment(column == 0 || column == 3 ? SwingConstants.CENTER : SwingConstants.LEFT);
            return this;
        }
    }

    // ----------------------------- Dashboard pictures -----------------------

    // Banner: the flyer's skyline, diagonal white panel, green + gold slices
    private class HeroPanel extends JComponent {
        HeroPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(100, 190));
            setMinimumSize(new Dimension(100, 190));
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            // Draw the scene into an image whose corners are rounded smoothly
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig = img.createGraphics();
            ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ig.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            ig.setColor(Color.WHITE);
            ig.fill(new RoundRectangle2D.Float(0, 0, w, h, 26, 26));
            ig.setComposite(AlphaComposite.SrcAtop);
            paintScene(ig, w, h);
            ig.dispose();
            g.drawImage(img, 0, 0, null);
        }

        private void paintScene(Graphics2D g2, int w, int h) {
            // stormy sky
            g2.setPaint(new GradientPaint(0, 0, new Color(0x1B2A3B), w, h, new Color(0x4A6178)));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fill(new Ellipse2D.Float(w * 0.50f, -40, w * 0.30f, 110));
            g2.fill(new Ellipse2D.Float(w * 0.72f, -20, w * 0.35f, 100));

            // skyline with lit windows
            Random rnd = new Random(11);
            int x = (int) (w * 0.36);
            while (x < w) {
                int bw = 26 + rnd.nextInt(22);
                int bh = (int) (h * (0.40 + rnd.nextDouble() * 0.52));
                int by = h - bh;
                g2.setPaint(new GradientPaint(x, by, new Color(0x4F7CA8), x, h, new Color(0x1A2B40)));
                g2.fillRect(x, by, bw, bh);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRect(x, by, 3, bh);
                for (int wy = by + 8; wy < h - 6; wy += 9) {
                    for (int wx = x + 7; wx < x + bw - 6; wx += 8) {
                        if (rnd.nextInt(100) < 32) {
                            g2.setColor(new Color(255, 214, 120, 200));
                            g2.fillRect(wx, wy, 3, 4);
                        }
                    }
                }
                x += bw + 3 + rnd.nextInt(4);
            }

            // gold + green diagonal slices
            g2.setColor(GOLD);
            g2.fill(new Polygon(
                    new int[] {(int) (w * 0.56), (int) (w * 0.585), (int) (w * 0.465), (int) (w * 0.44)},
                    new int[] {0, 0, h, h}, 4));
            g2.setColor(GREEN);
            g2.fill(new Polygon(
                    new int[] {(int) (w * 0.585), (int) (w * 0.605), (int) (w * 0.485), (int) (w * 0.465)},
                    new int[] {0, 0, h, h}, 4));

            // white diagonal panel
            g2.setColor(WHITE);
            g2.fill(new Polygon(
                    new int[] {0, (int) (w * 0.56), (int) (w * 0.44), 0},
                    new int[] {0, 0, h, h}, 4));

            // headline
            g2.setColor(INK);
            g2.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 30));
            g2.drawString("Stretch Your", 28, 52);

            Font big = font(Font.BOLD, 46);
            GlyphVector gv = big.createGlyphVector(g2.getFontRenderContext(), "BUDGET");
            Shape outline = gv.getOutline(28f, 100f);
            g2.setColor(WHITE);
            g2.fill(outline);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(GREEN);
            g2.draw(outline);
            g2.setStroke(new BasicStroke(1f));

            // gold tag
            g2.setFont(font(Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            String tag = "Student edition";
            int tw = fm.stringWidth(tag) + 26;
            g2.setColor(GOLD);
            g2.fillRoundRect(28, 114, tw, 26, 8, 8);
            g2.setColor(INK);
            g2.drawString(tag, 41, 114 + (26 - fm.getHeight()) / 2 + fm.getAscent());

            g2.setColor(new Color(0x4B5563));
            g2.setFont(font(Font.PLAIN, 13));
            g2.drawString("Make every allowance count for school and life.", 28, 166);

            // live chip (top right)
            String chip = expenses.size() + (expenses.size() == 1 ? " expense" : " expenses")
                    + "   |   Budget " + budget;
            g2.setFont(font(Font.BOLD, 12));
            FontMetrics cf = g2.getFontMetrics();
            int cw = cf.stringWidth(chip) + 28;
            int chx = w - cw - 18;
            g2.setColor(new Color(10, 18, 28, 160));
            g2.fillRoundRect(chx, 16, cw, 28, 28, 28);
            g2.setColor(WHITE);
            g2.drawString(chip, chx + 14, 16 + (28 - cf.getHeight()) / 2 + cf.getAscent());
        }
    }

    // Ring showing total expenses against the budget
    private class GaugePanel extends JComponent {
        GaugePanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(170, 170));
            setMinimumSize(new Dimension(120, 120));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth();
            int h = getHeight();
            int size = Math.min(w, h) - 8;
            if (size < 40) {
                g2.dispose();
                return;
            }
            int x = (w - size) / 2;
            int y = (h - size) / 2;
            float sw = Math.max(10f, size / 11f);
            double inset = sw / 2.0;

            g2.setStroke(new BasicStroke(sw));
            g2.setColor(new Color(0xE7ECE9));
            g2.draw(new Ellipse2D.Double(x + inset, y + inset, size - sw, size - sw));

            long total = 0;
            for (int i = 0; i < expenses.size(); i++) {
                total += expenses.get(i).cost;
            }
            boolean has = budget > 0;
            double pct = has ? (double) total / budget : 0;

            if (has && total > 0) {
                Color c = pct > 1.0 ? RED : (pct > 0.85 ? GOLD : GREEN);
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(c);
                g2.draw(new Arc2D.Double(x + inset, y + inset, size - sw, size - sw,
                        90, -360.0 * Math.min(pct, 1.0), Arc2D.OPEN));
            }

            String big = has ? (pct * 100 > 999 ? ">999%" : Math.round(pct * 100) + "%") : "--";
            String small = has ? (pct > 1.0 ? "over budget" : "of budget used") : "set a budget";

            g2.setColor(INK);
            g2.setFont(font(Font.BOLD, Math.max(18, size / 5)));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(big, (w - fm.stringWidth(big)) / 2, h / 2 + fm.getAscent() / 2 - 4);

            g2.setColor(MUTED);
            g2.setFont(font(Font.PLAIN, 12));
            FontMetrics sf = g2.getFontMetrics();
            g2.drawString(small, (w - sf.stringWidth(small)) / 2, h / 2 + fm.getAscent() / 2 + sf.getAscent() + 2);
            g2.dispose();
        }
    }

    // Bars: how many expenses sit at each priority 1..10
    private class PriorityChart extends JComponent {
        PriorityChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(300, 170));
            setMinimumSize(new Dimension(200, 120));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth();
            int h = getHeight();

            int[] cnt = new int[11];
            for (int i = 0; i < expenses.size(); i++) {
                int p = expenses.get(i).priority;
                if (p >= 1 && p <= 10) cnt[p]++;
            }
            int max = 0;
            for (int p = 1; p <= 10; p++) max = Math.max(max, cnt[p]);

            int left = 4;
            int right = 4;
            int top = 20;
            int bottom = 24;
            float slotW = (w - left - right) / 10f;
            int bw = (int) Math.max(6, slotW - 9);
            int baseY = h - bottom;
            int chartH = baseY - top;
            if (chartH < 10) {
                g2.dispose();
                return;
            }

            for (int p = 1; p <= 10; p++) {
                float cx = left + slotW * (p - 1) + slotW / 2f;
                int bx = (int) (cx - bw / 2f);

                g2.setColor(new Color(0xF0F3F1));
                g2.fillRoundRect(bx, top, bw, chartH, 8, 8);

                if (cnt[p] > 0 && max > 0) {
                    int bh = (int) Math.round(chartH * (cnt[p] / (double) max));
                    if (bh < 8) bh = 8;
                    g2.setColor(p >= 8 ? GREEN : (p >= 5 ? GOLD : new Color(0x9DB1A5)));
                    g2.fillRoundRect(bx, baseY - bh, bw, bh, 8, 8);

                    g2.setColor(INK);
                    g2.setFont(font(Font.BOLD, 11));
                    FontMetrics cf = g2.getFontMetrics();
                    String s = String.valueOf(cnt[p]);
                    g2.drawString(s, (int) (cx - cf.stringWidth(s) / 2f), baseY - bh - 5);
                }

                g2.setColor(MUTED);
                g2.setFont(font(Font.PLAIN, 12));
                FontMetrics lf = g2.getFontMetrics();
                String lab = String.valueOf(p);
                g2.drawString(lab, (int) (cx - lf.stringWidth(lab) / 2f), h - 6);
            }

            if (max == 0) {
                g2.setColor(MUTED);
                g2.setFont(font(Font.PLAIN, 13));
                FontMetrics mf = g2.getFontMetrics();
                String msg = "No expenses yet";
                g2.drawString(msg, (w - mf.stringWidth(msg)) / 2, top + chartH / 2);
            }
            g2.dispose();
        }
    }

    // ----------------------------- GUI helpers ------------------------------

    private JTextField makeField(int maxLength, final String hint) {
        final JTextField f = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = aa(g);
                    g2.setColor(new Color(0x9AA3AB));
                    g2.setFont(getFont());
                    Insets in = getInsets();
                    FontMetrics fm = g2.getFontMetrics();
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(hint, in.left, y);
                    g2.dispose();
                }
            }
        };
        final Border normal = BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xBFC8C2), 1, true), new EmptyBorder(8, 11, 8, 11));
        final Border focused = BorderFactory.createCompoundBorder(
                new LineBorder(GREEN, 2, true), new EmptyBorder(7, 10, 7, 10));
        f.setFont(BASE_FONT);
        f.setBackground(WHITE);
        f.setForeground(INK);
        f.setCaretColor(INK);
        f.setSelectionColor(GREEN_LT);
        f.setSelectedTextColor(INK);
        f.setBorder(normal);
        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                f.setBorder(focused);
            }

            @Override
            public void focusLost(FocusEvent e) {
                f.setBorder(normal);
            }
        });
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new LengthFilter(maxLength));
        return f;
    }

    private JPanel labeled(String caption, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel l = new JLabel(caption);
        l.setFont(font(Font.BOLD, 12));
        l.setForeground(MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel cardHeader(String title, String sub) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 16));
        t.setForeground(INK);
        JLabel s = new JLabel(sub);
        s.setFont(font(Font.PLAIN, 12));
        s.setForeground(MUTED);
        p.add(t);
        p.add(s);
        return p;
    }

    private JPanel pageHeader(String title, String sub) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 3));
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 26));
        t.setForeground(INK);
        JLabel s = new JLabel(sub);
        s.setFont(font(Font.PLAIN, 13));
        s.setForeground(MUTED);
        p.add(t);
        p.add(s);
        return p;
    }

    private JLabel statValueLabel() {
        JLabel l = new JLabel("0");
        l.setFont(font(Font.BOLD, 26));
        l.setForeground(INK);
        return l;
    }

    private Card statCard(String caption, JLabel value, String sub, Color accent) {
        Card c = new Card(new BorderLayout(0, 4), WHITE, accent);
        c.setBorder(new EmptyBorder(14, 20, 17, 12));
        JLabel cap = new JLabel(caption);
        cap.setFont(font(Font.BOLD, 13));
        cap.setForeground(MUTED);
        JLabel s = new JLabel(sub);
        s.setFont(font(Font.PLAIN, 12));
        s.setForeground(MUTED);
        c.add(cap, BorderLayout.NORTH);
        c.add(value, BorderLayout.CENTER);
        c.add(s, BorderLayout.SOUTH);
        return c;
    }

    private Card actionCard(String title, String desc, PillButton button, Color accent) {
        Card c = new Card(new BorderLayout(0, 10), WHITE, accent);
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 16));
        t.setForeground(INK);

        JTextArea d = new JTextArea(desc);
        d.setEditable(false);
        d.setFocusable(false);
        d.setOpaque(false);
        d.setLineWrap(true);
        d.setWrapStyleWord(true);
        d.setRows(5);
        d.setFont(font(Font.PLAIN, 13));
        d.setForeground(MUTED);
        d.setBorder(null);

        c.add(t, BorderLayout.NORTH);
        c.add(d, BorderLayout.CENTER);
        c.add(button, BorderLayout.SOUTH);
        return c;
    }

    private void addNav(ButtonGroup group, JPanel holder, final String key, String label, int icon) {
        NavButton b = new NavButton(label, icon);
        group.add(b);
        holder.add(b);
        navButtons.put(key, b);
        b.addActionListener(e -> cards.show(pages, key));
    }

    // ----------------------------- GUI construction -------------------------
    private void buildGui() {

        // Dialog colors
        UIManager.put("OptionPane.background", WHITE);
        UIManager.put("Panel.background", WHITE);
        UIManager.put("OptionPane.messageForeground", INK);

        // Shared widgets
        editBudget = makeField(10, "Enter your allowance");
        editName = makeField(60, "e.g. School supplies");
        editCost = makeField(10, "Cost");
        editPriority = makeField(10, "1 - 10");
        editDaily = makeField(10, "Spend per day");
        editDaily.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateRunway();
                saveDailyToDatabase();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateRunway();
                saveDailyToDatabase();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateRunway();
                saveDailyToDatabase();
            }
        });

        txtRunway = new JTextArea("Enter how much you spend each day.");
        txtRunway.setEditable(false);
        txtRunway.setFocusable(false);
        txtRunway.setOpaque(false);
        txtRunway.setLineWrap(true);
        txtRunway.setWrapStyleWord(true);
        txtRunway.setColumns(10);
        txtRunway.setRows(2);
        txtRunway.setFont(font(Font.PLAIN, 12));
        txtRunway.setForeground(MUTED);
        txtRunway.setBorder(null);

        lblBudget = new JLabel("0");
        lblBudget.setFont(font(Font.BOLD, 24));
        lblBudget.setForeground(WHITE);

        lblStatBudget = statValueLabel();
        lblStatTotal = statValueLabel();
        lblStatRemaining = statValueLabel();
        lblStatCount = statValueLabel();
        lblStatDays = statValueLabel();

        btnSetBudget = new PillButton("Set Budget", PillButton.Kind.PRIMARY);
        btnRunOptimizer = new PillButton("Optimize Budget", PillButton.Kind.GOLD);
        btnAdd = new PillButton("Add Expense", PillButton.Kind.PRIMARY);
        btnUpdate = new PillButton("Update Selected", PillButton.Kind.DARK);
        btnRemove = new PillButton("Remove Selected", PillButton.Kind.DANGER);
        btnRemoveAll = new PillButton("Remove All", PillButton.Kind.DANGER);
        btnOptimize = new PillButton("Optimize Budget", PillButton.Kind.PRIMARY);
        btnSummary = new PillButton("View Budget Summary", PillButton.Kind.DARK);
        btnWindow = new PillButton("Spending Window", PillButton.Kind.DARK);
        btnPair = new PillButton("Find Pair by Cost", PillButton.Kind.DARK);
        btnPrint = new PillButton("Print Report", PillButton.Kind.GOLD);
        btnExit = new PillButton("Exit", PillButton.Kind.GHOST);

        result = new JTextArea("Choose an action above and the result will appear here.");
        result.setEditable(false);
        result.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        result.setOpaque(false);
        result.setForeground(new Color(0xE8EEF3));
        result.setCaretColor(WHITE);
        result.setBorder(new EmptyBorder(2, 2, 2, 2));

        statusLabel = new JLabel("Ready");
        statusLabel.setIcon(new DotIcon(GREEN));
        statusLabel.setIconTextGap(10);
        statusLabel.setFont(font(Font.PLAIN, 13));
        statusLabel.setForeground(INK);

        hero = new HeroPanel();
        gauge = new GaugePanel();
        chart = new PriorityChart();

        buildTable();

        // Layout
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PAPER);
        setContentPane(root);

        root.add(buildSidebar(), BorderLayout.WEST);

        cards = new CardLayout();
        pages = new JPanel(cards);
        pages.setOpaque(false);
        pages.add(buildDashboardPage(), "dashboard");
        pages.add(buildExpensesPage(), "expenses");
        pages.add(buildOptimizerPage(), "optimizer");

        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(WHITE);
        status.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, LINE), new EmptyBorder(10, 28, 10, 28)));
        status.add(statusLabel, BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(PAPER);
        main.add(pages, BorderLayout.CENTER);
        main.add(status, BorderLayout.SOUTH);
        root.add(main, BorderLayout.CENTER);

        navButtons.get("dashboard").setSelected(true);
        cards.show(pages, "dashboard");

        // Connections
        btnSetBudget.addActionListener(e -> doSetBudget());
        btnRunOptimizer.addActionListener(e -> {
            showPage("optimizer");
            doOptimize();
        });
        btnAdd.addActionListener(e -> doAdd());
        btnUpdate.addActionListener(e -> doUpdate());
        btnRemove.addActionListener(e -> doRemove());
        btnRemoveAll.addActionListener(e -> doRemoveAll());
        btnOptimize.addActionListener(e -> doOptimize());
        btnSummary.addActionListener(e -> doSummary());
        btnWindow.addActionListener(e -> doSlidingWindow());
        btnPair.addActionListener(e -> doFindPair());
        btnPrint.addActionListener(e -> doPrint());
        btnExit.addActionListener(e -> doExit());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });

        // Press Enter in a box = same as its button
        editBudget.addActionListener(e -> doSetBudget());
        editPriority.addActionListener(e -> doAdd());
    }

    private void buildTable() {
        tableModel = new DefaultTableModel(new Object[] {"#", "Item Name", "Cost", "Priority"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = aa(g);
                    g2.setFont(font(Font.PLAIN, 14));
                    g2.setColor(MUTED);
                    FontMetrics fm = g2.getFontMetrics();
                    String msg = "No expenses yet. Add your first one above.";
                    g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                    g2.dispose();
                }
            }
        };
        table.setFont(BASE_FONT);
        table.setForeground(INK);
        table.setBackground(WHITE);
        table.setGridColor(new Color(0xEBEFEC));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setRowHeight(38);
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

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(100, 36));
        header.setBackground(NAVY);

        table.setDefaultRenderer(Object.class, new CellRenderer());
    }

    private JPanel buildSidebar() {
        JPanel s = new Sidebar();

        // brand
        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(26, 22, 22, 16));
        JPanel names = new JPanel(new GridLayout(2, 1, 0, 0));
        names.setOpaque(false);
        JLabel n1 = new JLabel("Student");
        n1.setFont(font(Font.BOLD, 20));
        n1.setForeground(WHITE);
        JLabel n2 = new JLabel("Budget Optimizer");
        n2.setFont(font(Font.BOLD, 13));
        n2.setForeground(GOLD);
        names.add(n1);
        names.add(n2);
        brand.add(new Logo(), BorderLayout.WEST);
        brand.add(names, BorderLayout.CENTER);

        // navigation
        ButtonGroup group = new ButtonGroup();
        JPanel nav = new JPanel(new GridLayout(0, 1, 0, 6));
        nav.setOpaque(false);
        addNav(group, nav, "dashboard", "Dashboard", 0);
        addNav(group, nav, "expenses", "Expenses", 1);
        addNav(group, nav, "optimizer", "Optimizer", 2);

        JPanel navHolder = new JPanel(new BorderLayout());
        navHolder.setOpaque(false);
        navHolder.setBorder(new EmptyBorder(4, 14, 0, 14));
        navHolder.add(nav, BorderLayout.NORTH);

        // bottom: budget + exit (sits on the green shape)
        JPanel budgetBox = new JPanel(new GridLayout(2, 1, 0, 0));
        budgetBox.setOpaque(false);
        JLabel cap = new JLabel("Current budget");
        cap.setFont(font(Font.PLAIN, 13));
        cap.setForeground(new Color(255, 255, 255, 220));
        budgetBox.add(cap);
        budgetBox.add(lblBudget);

        JPanel south = new JPanel(new GridLayout(0, 1, 0, 14));
        south.setOpaque(false);
        south.setBorder(new EmptyBorder(0, 22, 24, 22));
        south.add(budgetBox);
        south.add(btnPrint);
        south.add(btnExit);

        s.add(brand, BorderLayout.NORTH);
        s.add(navHolder, BorderLayout.CENTER);
        s.add(south, BorderLayout.SOUTH);
        return s;
    }

    private JPanel buildDashboardPage() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(24, 28, 14, 28));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0;
        c.insets = new Insets(0, 0, 18, 0);
        p.add(hero, c);

        JPanel stats = new JPanel(new GridLayout(1, 5, 12, 0));
        stats.setOpaque(false);
        stats.add(statCard("Budget", lblStatBudget, "available to spend", GREEN));
        stats.add(statCard("Total expenses", lblStatTotal, "value of every item", GOLD));
        stats.add(statCard("Remaining", lblStatRemaining, "before optimizing", NAVY));
        stats.add(statCard("Items", lblStatCount, "on your list", GREEN_DK));
        stats.add(statCard("Days left", lblStatDays, "at your daily spend", GREEN));
        c.gridy = 1;
        c.insets = new Insets(0, 0, 18, 0);
        p.add(stats, c);

        // bottom row
        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setOpaque(false);

        Card budgetCard = new Card(new GridBagLayout(), WHITE, GOLD);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTH;
        g.gridy = 0;
        g.insets = new Insets(0, 0, 12, 0);
        budgetCard.add(cardHeader("Set your budget", "Your allowance or savings"), g);
        g.gridy = 1;
        g.insets = new Insets(0, 0, 10, 0);
        budgetCard.add(editBudget, g);
        g.gridy = 2;
        budgetCard.add(btnSetBudget, g);
        g.gridy = 3;
        g.insets = new Insets(4, 0, 6, 0);
        budgetCard.add(labeled("Daily spending (how long will it last?)", editDaily), g);
        g.gridy = 4;
        g.insets = new Insets(0, 0, 0, 0);
        budgetCard.add(txtRunway, g);
        g.gridy = 5;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        budgetCard.add(filler, g);
        g.gridy = 6;
        g.weighty = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 0);
        budgetCard.add(btnRunOptimizer, g);

        Card gaugeCard = new Card(new BorderLayout(0, 8), WHITE, null);
        gaugeCard.add(cardHeader("Budget usage", "Total expenses against your budget"), BorderLayout.NORTH);
        gaugeCard.add(gauge, BorderLayout.CENTER);

        Card chartCard = new Card(new BorderLayout(0, 8), WHITE, null);
        chartCard.add(cardHeader("Priority spread", "Needs (8-10), important (5-7), nice to have (1-4)"), BorderLayout.NORTH);
        chartCard.add(chart, BorderLayout.CENTER);

        GridBagConstraints b = new GridBagConstraints();
        b.fill = GridBagConstraints.BOTH;
        b.weighty = 1;
        b.gridy = 0;
        b.gridx = 0;
        b.weightx = 1;
        b.insets = new Insets(0, 0, 0, 16);
        bottom.add(budgetCard, b);
        b.gridx = 1;
        bottom.add(gaugeCard, b);
        b.gridx = 2;
        b.weightx = 1.7;
        b.insets = new Insets(0, 0, 0, 0);
        bottom.add(chartCard, b);

        c.gridy = 2;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(bottom, c);
        return p;
    }

    private JPanel buildExpensesPage() {
        JPanel p = new JPanel(new BorderLayout(0, 18));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(24, 28, 14, 28));
        p.add(pageHeader("Expenses", "Add your school and daily expenses: food, fare, projects, load and more."),
                BorderLayout.NORTH);

        // form card
        Card form = new Card(new GridBagLayout(), WHITE, GREEN);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;
        g.insets = new Insets(0, 0, 14, 12);
        g.gridx = 0;
        g.weightx = 3;
        form.add(labeled("Item name", editName), g);
        g.gridx = 1;
        g.weightx = 1;
        form.add(labeled("Cost", editCost), g);
        g.gridx = 2;
        g.weightx = 1;
        g.insets = new Insets(0, 0, 14, 0);
        form.add(labeled("Priority (1-10)", editPriority), g);

        JPanel btnRow = new JPanel();
        btnRow.setOpaque(false);
        btnRow.setLayout(new BoxLayout(btnRow, BoxLayout.X_AXIS));
        btnRow.add(btnAdd);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(btnUpdate);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(btnRemove);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(btnRemoveAll);
        btnRow.add(Box.createHorizontalGlue());
        JLabel tip = new JLabel("Select a row in the table to edit or remove it.");
        tip.setFont(font(Font.PLAIN, 12));
        tip.setForeground(MUTED);
        btnRow.add(tip);

        g.gridx = 0;
        g.gridy = 1;
        g.gridwidth = 3;
        g.weightx = 1;
        g.insets = new Insets(0, 0, 0, 0);
        form.add(btnRow, g);

        // table card
        Card tableCard = new Card(new BorderLayout(0, 12), WHITE, null);
        tableCard.add(cardHeader("Expense list",
                "Priority: 8-10 need  |  5-7 important  |  1-4 nice to have"), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(LINE, 1));
        sp.getViewport().setBackground(WHITE);
        tableCard.add(sp, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(form, BorderLayout.NORTH);
        body.add(tableCard, BorderLayout.CENTER);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildOptimizerPage() {
        JPanel p = new JPanel(new BorderLayout(0, 18));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(24, 28, 14, 28));
        p.add(pageHeader("Optimizer", "Find the best expenses for your budget, or explore your spending."),
                BorderLayout.NORTH);

        JPanel actions = new JPanel(new GridLayout(1, 4, 16, 0));
        actions.setOpaque(false);
        actions.add(actionCard("Optimize Budget",
                "Picks the expenses with the highest total priority that still fit your budget.",
                btnOptimize, GREEN));
        actions.add(actionCard("Budget Summary",
                "Shows your budget, the total value of all expenses and what is left.",
                btnSummary, GOLD));
        actions.add(actionCard("Spending Window",
                "Sliding window: checks every group of expenses in a row and finds your heaviest and lightest stretch of spending.",
                btnWindow, NAVY));
        actions.add(actionCard("Find Pair by Cost",
                "Finds two expenses that add up to an amount you choose.",
                btnPair, GREEN_DK));

        Card console = new Card(new BorderLayout(0, 10), NAVY, GREEN);
        JLabel ch = new JLabel("Results");
        ch.setFont(font(Font.BOLD, 16));
        ch.setForeground(GOLD);
        JScrollPane rs = new JScrollPane(result);
        rs.setBorder(null);
        rs.setOpaque(false);
        rs.getViewport().setOpaque(false);
        console.add(ch, BorderLayout.NORTH);
        console.add(rs, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(actions, BorderLayout.NORTH);
        body.add(console, BorderLayout.CENTER);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
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