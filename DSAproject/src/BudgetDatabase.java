package DSAproject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BudgetDatabase {

    private static final String URL = "jdbc:sqlite:budget.db";

    private BudgetDatabase() {
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // Creates the tables if they do not exist yet. Call once at startup.
    public static void init() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found. Add the sqlite-jdbc JAR to lib/.", e);
        }

        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS settings ("
                    + "id INTEGER PRIMARY KEY, "
                    + "budget INTEGER NOT NULL DEFAULT 0, "
                    + "daily INTEGER NOT NULL DEFAULT 0)");
            st.execute("CREATE TABLE IF NOT EXISTS expenses ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL, "
                    + "cost INTEGER NOT NULL, "
                    + "priority INTEGER NOT NULL)");
            st.execute("INSERT OR IGNORE INTO settings (id, budget, daily) VALUES (1, 0, 0)");
        }
    }

    // ----------------------------- Settings ---------------------------------

    public static int loadBudget() throws SQLException {
        return loadSetting("budget");
    }

    public static int loadDaily() throws SQLException {
        return loadSetting("daily");
    }

    public static void saveBudget(int budget) throws SQLException {
        saveSetting("budget", budget);
    }

    public static void saveDaily(int daily) throws SQLException {
        saveSetting("daily", daily);
    }

    // column is only ever "budget" or "daily" (never user input)
    private static int loadSetting(String column) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("SELECT " + column + " FROM settings WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void saveSetting(String column, int value) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("UPDATE settings SET " + column + " = ? WHERE id = 1")) {
            ps.setInt(1, value);
            ps.executeUpdate();
        }
    }

    // ----------------------------- Expenses ---------------------------------

    // Returns all expenses in the order they were added
    public static List<BudgetOptimizer.Expense> loadExpenses() throws SQLException {
        List<BudgetOptimizer.Expense> list = new ArrayList<>();
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, name, cost, priority FROM expenses ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BudgetOptimizer.Expense e = new BudgetOptimizer.Expense();
                e.id = rs.getInt("id");
                e.name = rs.getString("name");
                e.cost = rs.getInt("cost");
                e.priority = rs.getInt("priority");
                list.add(e);
            }
        }
        return list;
    }

    // Saves a new expense and stores the generated id in e.id
    public static void insertExpense(BudgetOptimizer.Expense e) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO expenses (name, cost, priority) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.name);
            ps.setInt(2, e.cost);
            ps.setInt(3, e.priority);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    e.id = keys.getInt(1);
                }
            }
        }
    }

    // Updates the row that has the same id as e
    public static void updateExpense(BudgetOptimizer.Expense e) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE expenses SET name = ?, cost = ?, priority = ? WHERE id = ?")) {
            ps.setString(1, e.name);
            ps.setInt(2, e.cost);
            ps.setInt(3, e.priority);
            ps.setInt(4, e.id);
            ps.executeUpdate();
        }
    }

    // Removes every expense (the budget and daily spending are kept)
    public static void deleteAllExpenses() throws SQLException {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM expenses");
        }
    }

    public static void deleteExpense(int id) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM expenses WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}