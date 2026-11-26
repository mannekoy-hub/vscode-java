import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoardingDatabase {

    private static final String URL = "jdbc:sqlite:boarding.db";

    private BoardingDatabase() {
    }

    private static Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA busy_timeout = 5000"); // wait if the other app is writing
        }
        return c;
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
                    + "id INTEGER PRIMARY KEY CHECK(id = 1), "
                    + "rent REAL, water REAL, rate REAL, due_day INTEGER, beds INTEGER)");
            st.execute("CREATE TABLE IF NOT EXISTS boarders ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL, room TEXT NOT NULL, pin TEXT NOT NULL, "
                    + "rent REAL, move_in TEXT, move_out TEXT, active INTEGER DEFAULT 1)");
            st.execute("CREATE TABLE IF NOT EXISTS masks ("
                    + "boarder_id INTEGER, year INTEGER, "
                    + "rent_mask INTEGER DEFAULT 0, water_mask INTEGER DEFAULT 0, elec_mask INTEGER DEFAULT 0, "
                    + "PRIMARY KEY (boarder_id, year))");
            st.execute("CREATE TABLE IF NOT EXISTS readings ("
                    + "boarder_id INTEGER, year INTEGER, month INTEGER, prev REAL, present REAL, rate REAL, "
                    + "PRIMARY KEY (boarder_id, year, month))");
            st.execute("CREATE TABLE IF NOT EXISTS notifs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "boarder_id INTEGER, year INTEGER, month INTEGER, kind TEXT, msg TEXT, due TEXT, "
                    + "sent INTEGER DEFAULT 0, UNIQUE (boarder_id, year, month, kind))");
            st.execute("INSERT OR IGNORE INTO settings VALUES (1, 2000, 100, 0, 5, 2)");
        }
    }

    // ----------------------------- Settings ---------------------------------

    // column is only ever "rent", "water", "rate", "due_day" or "beds" (never user input)
    public static double loadSetting(String column) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("SELECT " + column + " FROM settings WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public static void saveSetting(String column, double value) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("UPDATE settings SET " + column + " = ? WHERE id = 1")) {
            ps.setDouble(1, value);
            ps.executeUpdate();
        }
    }

    // ----------------------------- Boarders ---------------------------------

    // Returns all boarders (with their paid-month bitmasks and meter readings)
    public static List<Shared.Boarder> loadBoarders() throws SQLException {
        List<Shared.Boarder> list = new ArrayList<>();
        Map<Integer, Shared.Boarder> byId = new HashMap<>();
        try (Connection c = connect(); Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT * FROM boarders ORDER BY id")) {
                while (rs.next()) {
                    Shared.Boarder b = new Shared.Boarder();
                    b.id = rs.getInt("id");
                    b.name = rs.getString("name");
                    b.room = rs.getString("room");
                    b.pin = rs.getString("pin");
                    b.rent = rs.getDouble("rent");
                    b.moveIn = LocalDate.parse(rs.getString("move_in"));
                    String mo = rs.getString("move_out");
                    b.moveOut = (mo == null || mo.isEmpty()) ? null : LocalDate.parse(mo);
                    b.active = rs.getInt("active") == 1;
                    list.add(b);
                    byId.put(b.id, b);
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT * FROM masks")) {
                while (rs.next()) {
                    Shared.Boarder b = byId.get(rs.getInt("boarder_id"));
                    if (b != null) {
                        b.masks.put(rs.getInt("year"),
                                new int[]{rs.getInt("rent_mask"), rs.getInt("water_mask"), rs.getInt("elec_mask")});
                    }
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT * FROM readings")) {
                while (rs.next()) {
                    Shared.Boarder b = byId.get(rs.getInt("boarder_id"));
                    if (b != null) {
                        b.readings.put(rs.getInt("year") * 100 + rs.getInt("month"),
                                new double[]{rs.getDouble("prev"), rs.getDouble("present"), rs.getDouble("rate")});
                    }
                }
            }
        }
        return list;
    }

    // Saves a new boarder and stores the generated id in b.id
    public static void insertBoarder(Shared.Boarder b) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO boarders (name, room, pin, rent, move_in, move_out, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, b);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    b.id = keys.getInt(1);
                }
            }
        }
    }

    // Updates the row that has the same id as b
    public static void updateBoarder(Shared.Boarder b) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE boarders SET name = ?, room = ?, pin = ?, rent = ?, move_in = ?, move_out = ?, active = ? WHERE id = ?")) {
            fill(ps, b);
            ps.setInt(8, b.id);
            ps.executeUpdate();
        }
    }

    private static void fill(PreparedStatement ps, Shared.Boarder b) throws SQLException {
        ps.setString(1, b.name);
        ps.setString(2, b.room);
        ps.setString(3, b.pin);
        ps.setDouble(4, b.rent);
        ps.setString(5, b.moveIn.toString());
        ps.setString(6, b.moveOut == null ? "" : b.moveOut.toString());
        ps.setInt(7, b.active ? 1 : 0);
    }

    // ----------------------- Paid months (bitmask) --------------------------

    // mk = {rentMask, waterMask, elecMask}
    public static void saveMasks(int boarderId, int year, int[] mk) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT OR REPLACE INTO masks (boarder_id, year, rent_mask, water_mask, elec_mask) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, boarderId);
            ps.setInt(2, year);
            ps.setInt(3, mk[0]);
            ps.setInt(4, mk[1]);
            ps.setInt(5, mk[2]);
            ps.executeUpdate();
        }
    }

    // --------------------------- Meter readings -----------------------------

    public static void saveReading(int boarderId, int year, int month, double prev, double present, double rate)
            throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT OR REPLACE INTO readings (boarder_id, year, month, prev, present, rate) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, boarderId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            ps.setDouble(4, prev);
            ps.setDouble(5, present);
            ps.setDouble(6, rate);
            ps.executeUpdate();
        }
    }

    // --------------------------- Notifications ------------------------------

    // Returns all notifications in the order they were added
    public static List<Shared.Notif> loadNotifs() throws SQLException {
        List<Shared.Notif> list = new ArrayList<>();
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM notifs ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Shared.Notif n = new Shared.Notif();
                n.id = rs.getInt("id");
                n.bid = rs.getInt("boarder_id");
                n.year = rs.getInt("year");
                n.month = rs.getInt("month");
                n.kind = rs.getString("kind");
                n.msg = rs.getString("msg") == null ? "" : rs.getString("msg");
                n.due = LocalDateTime.parse(rs.getString("due"));
                n.sent = rs.getInt("sent");
                list.add(n);
            }
        }
        return list;
    }

    // Saves a new notification and stores the generated id in n.id
    public static void insertNotif(Shared.Notif n) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT OR IGNORE INTO notifs (boarder_id, year, month, kind, msg, due, sent) VALUES (?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, n.bid);
            ps.setInt(2, n.year);
            ps.setInt(3, n.month);
            ps.setString(4, n.kind);
            ps.setString(5, n.msg);
            ps.setString(6, n.due.toString());
            ps.setInt(7, n.sent);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    n.id = keys.getInt(1);
                }
            }
        }
    }

    // sent: 0 pending, 1 delivered, 2 cancelled (already paid)
    public static void updateNotif(Shared.Notif n) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("UPDATE notifs SET msg = ?, sent = ? WHERE id = ?")) {
            ps.setString(1, n.msg);
            ps.setInt(2, n.sent);
            ps.setInt(3, n.id);
            ps.executeUpdate();
        }
    }

    // ------------------------------ Reset -----------------------------------

    public static void clearAll() throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM notifs");
                st.executeUpdate("DELETE FROM readings");
                st.executeUpdate("DELETE FROM masks");
                st.executeUpdate("DELETE FROM boarders");
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}