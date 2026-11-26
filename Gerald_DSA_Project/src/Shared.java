import java.io.*;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

public class Shared {
    static final String[] MONTHS = {"January","February","March","April","May","June","July",
            "August","September","October","November","December"};
    static final String[] KEYS = {"Rent", "Water", "Electricity"};
    static final String PESO = "\u20B1";

    // ------------------------------------------------------------ data model
    static class Boarder implements Serializable {
        int id; String name, room, pin; double rent; LocalDate moveIn, moveOut; boolean active = true;
        Map<Integer, int[]> masks = new HashMap<>();          // year -> {rentMask, waterMask, elecMask}
        Map<Integer, double[]> readings = new HashMap<>();    // year*100+month -> {prev, present, rate}
    }
    static class Notif implements Serializable {
        int id, bid, year, month, sent; String kind, msg = ""; LocalDateTime due;   // sent: 0 pending, 1 delivered, 2 cancelled
    }
    static class Data implements Serializable {
        double rent = 2000, water = 100, rate = 0; int dueDay = 5, beds = 2, nextId = 1;
        List<Boarder> boarders = new ArrayList<>(); List<Notif> notifs = new ArrayList<>();
    }
    static Data d = new Data();

    // ------------------------------------------- SQLite (see BoardingDatabase.java)
    interface Db { void run() throws SQLException; }
    static void db(Db x) {
        try { x.run(); } catch (SQLException e) { throw new RuntimeException("Database error: " + e.getMessage(), e); }
    }
    /** Call once at startup (creates the tables if needed). */
    static void init() { db(BoardingDatabase::init); }

    /** Reload everything from the database into memory. */
    static synchronized void load() {
        try {
            Data n = new Data();
            n.rent = BoardingDatabase.loadSetting("rent"); n.water = BoardingDatabase.loadSetting("water"); n.rate = BoardingDatabase.loadSetting("rate");
            n.dueDay = (int) BoardingDatabase.loadSetting("due_day"); n.beds = (int) BoardingDatabase.loadSetting("beds");
            n.boarders = BoardingDatabase.loadBoarders(); n.notifs = BoardingDatabase.loadNotifs();
            d = n;
        } catch (SQLException e) { System.err.println("Database error: " + e.getMessage()); }
    }
    static void saveSettings(double rent, double water, double rate, int dueDay, int beds) {
        db(() -> {
            BoardingDatabase.saveSetting("rent", rent); BoardingDatabase.saveSetting("water", water); BoardingDatabase.saveSetting("rate", rate);
            BoardingDatabase.saveSetting("due_day", dueDay); BoardingDatabase.saveSetting("beds", beds);
        });
    }
    /** ADMIN only: deletes ALL boarders, payments, meter readings and notifications (the settings are kept). Cannot be undone. */
    static void resetAll() { db(BoardingDatabase::clearAll); load(); }
    static String peso(double v) { return String.format(PESO + "%,.2f", v); }
    static Boarder find(int id) { for (Boarder b : d.boarders) if (b.id == id) return b; return null; }

    // ---------------------------------- ALGORITHM 1: BITMASK (paid months)
    // Bit (m-1) of an int = 1 when month m is paid. One mask each for rent / water / electricity.
    static int bit(int m) { return 1 << (m - 1); }
    static boolean isPaid(int mask, int m) { return (mask & bit(m)) != 0; }
    static int mark(int mask, int m) { return mask | bit(m); }
    static int countPaid(int mask) { return Integer.bitCount(mask); }
    static int[] masks(Boarder b, int y) { return b.masks.computeIfAbsent(y, k -> new int[3]); }

    // ------------- ALGORITHM 2: DIFFERENCE MAP + PREFIX SUM (room occupancy)
    /** Every stay [moveIn, moveOut] adds +1 on its first day and -1 on the day after its last day
     *  (a TreeMap keeps these change points sorted by date). A running prefix sum over the sorted
     *  points gives how many beds are used on any day. The new boarder fits only if the busiest
     *  day from their move-in onward still has a free bed. */
    static boolean roomHasSpace(String room, LocalDate moveIn) {
        TreeMap<Long, Integer> diff = new TreeMap<>();
        long open = Long.MAX_VALUE / 2;
        for (Boarder b : d.boarders)
            if (b.active && b.room.equalsIgnoreCase(room)) {
                long lo = b.moveIn.toEpochDay(), hi = b.moveOut == null ? open : b.moveOut.toEpochDay();
                diff.merge(lo, 1, Integer::sum);          // +1 when the stay starts
                diff.merge(hi + 1, -1, Integer::sum);     // -1 the day after it ends
            }
        long q = moveIn.toEpochDay();
        int run = 0, peak = 0;                            // run = prefix sum, peak = busiest day from q onward
        for (Map.Entry<Long, Integer> e : diff.entrySet()) {
            run += e.getValue();
            if (e.getKey() <= q) peak = run;              // occupancy on the move-in day itself
            else peak = Math.max(peak, run);              // occupancy changes later on
        }
        return peak < d.beds;
    }

    // ------------------------------------------------------------ boarders
    static String addBoarder(String name, String room, String pin, double rent, String moveIn) {
        load();
        if (name.isEmpty() || room.isEmpty() || pin.isEmpty()) return "Fill in name, room and PIN.";
        LocalDate mi;
        try { mi = LocalDate.parse(moveIn); } catch (Exception e) { return "Move-in date must be YYYY-MM-DD."; }
        for (Boarder b : d.boarders) if (b.active && b.name.equalsIgnoreCase(name)) return "A boarder with this name already exists.";
        if (!roomHasSpace(room, mi)) return "Room " + room + " is full for that date.";
        Boarder b = new Boarder(); b.name = name; b.room = room; b.pin = pin; b.rent = rent; b.moveIn = mi;
        db(() -> BoardingDatabase.insertBoarder(b)); d.boarders.add(b); return null;
    }
    /** Edit one cell of a boarder: col 0 name, 1 room, 2 PIN, 3 rent, 4 move-in. Returns error text or null. */
    static String updateBoarder(int id, int col, String v) {
        load(); Boarder b = find(id); if (b == null) return "Boarder not found.";
        if (v.isEmpty()) return "This field cannot be empty.";
        switch (col) {
            case 0:
                for (Boarder o : d.boarders) if (o.active && o.id != id && o.name.equalsIgnoreCase(v)) return "A boarder with this name already exists.";
                b.name = v; break;
            case 1: case 4: {
                String room = col == 1 ? v : b.room; LocalDate mi = b.moveIn;
                if (col == 4) { try { mi = LocalDate.parse(v); } catch (Exception e) { return "Move-in date must be YYYY-MM-DD."; } }
                b.active = false;                                   // leave self out of the occupancy check
                boolean ok = roomHasSpace(room, mi);
                b.active = true;
                if (!ok) return "Room " + room + " is full for that date.";
                b.room = room; b.moveIn = mi; break;
            }
            case 2: b.pin = v; break;
            case 3:
                try { double r = Double.parseDouble(v); if (r < 0) return "Rent cannot be negative."; b.rent = r; }
                catch (NumberFormatException e) { return "Rent must be a number."; }
                break;
        }
        db(() -> BoardingDatabase.updateBoarder(b)); return null;
    }

  
    static boolean showsIn(Boarder b, int y, int m) {
        if (b.active) return true;
        if (b.moveIn == null || b.moveOut == null) return false;
        LocalDate last = YearMonth.of(y, m).atEndOfMonth();
        return !b.moveIn.isAfter(last) && last.isBefore(b.moveOut.withDayOfMonth(1));
    }


    static void moveOut(int id) {
        load(); Boarder b = find(id); if (b == null) return;
        b.active = false; b.moveOut = LocalDate.now();
        int fromY = b.moveOut.getYear(), fromM = b.moveOut.getMonthValue();
        List<Integer> changedYears = new ArrayList<>();
        for (Map.Entry<Integer, int[]> e : b.masks.entrySet()) {              // clear paid marks from the move-out month onward
            int yr = e.getKey(); if (yr < fromY) continue;
            int[] mk = e.getValue(); boolean ch = false;
            for (int k = 0; k < 3; k++)
                for (int mo = (yr == fromY ? fromM : 1); mo <= 12; mo++)
                    if (isPaid(mk[k], mo)) { mk[k] &= ~bit(mo); ch = true; }
            if (ch) changedYears.add(yr);
        }
        List<Notif> cancel = new ArrayList<>();
        for (Notif n : d.notifs)                                              // cancel reminders / payment messages from that month onward
            if (n.bid == id && n.sent != 2 && n.year * 100 + n.month >= fromY * 100 + fromM) { n.sent = 2; cancel.add(n); }
        db(() -> {
            BoardingDatabase.updateBoarder(b);
            for (int yr : changedYears) BoardingDatabase.saveMasks(id, yr, b.masks.get(yr));
            for (Notif n : cancel) BoardingDatabase.updateNotif(n);
        });
    }

    static Boarder login(String name, String pin) {
        load();
        for (Boarder b : d.boarders) if (b.active && b.name.equalsIgnoreCase(name) && b.pin.equals(pin)) return b;
        return null;
    }

    // ------------------------------------------------------------- billing
    // Uses the real length of the month (28/29/30/31) instead of capping the due day at 28.
    static LocalDate dueDate(int y, int m) { return LocalDate.of(y, m, Math.min(d.dueDay, YearMonth.of(y, m).lengthOfMonth())); }

    static double prevReading(Boarder b, int y, int m) {       // starting reading is 0
        int best = -1, now = y * 100 + m;
        for (int k : b.readings.keySet()) if (k < now && k > best) best = k;
        return best < 0 ? 0 : b.readings.get(best)[1];
    }


    static double rentShare(Boarder b, int y, int m) {
        List<Boarder> occ = occupants(b, y, m); double price = b.rent;
        for (Boarder o : occ) price = Math.max(price, o.rent);
        return split(Math.round(price * 100), occ, b) / 100.0;
    }
    /** Everyone sharing b's room in that month (b included), sorted by id. */
    static List<Boarder> occupants(Boarder b, int y, int m) {
        LocalDate last = YearMonth.of(y, m).atEndOfMonth();
        List<Boarder> occ = new ArrayList<>(); boolean found = false;
        for (Boarder o : d.boarders) {
            if (o.id == b.id) { found = true; }
            else {
                if (o.room == null || b.room == null || !o.room.equalsIgnoreCase(b.room) || o.moveIn == null) continue;
                if (o.moveIn.isAfter(last)) continue;                              // not moved in yet
                if (!o.active && (o.moveOut == null || !last.isBefore(o.moveOut.withDayOfMonth(1)))) continue;   // moved out: only counts for months BEFORE the month they left
            }
            occ.add(o);
        }
        if (!found) occ.add(b);
        occ.sort(Comparator.comparingInt((Boarder x) -> x.id));
        return occ;
    }
    /** Splits an amount (in centavos) equally between occ and returns b's part in centavos; leftover centavos go to the lowest ids. */
    static long split(long cents, List<Boarder> occ, Boarder b) {
        int n = occ.size(), rank = 0;
        for (int i = 0; i < n; i++) if (occ.get(i).id == b.id) rank = i;
        return cents / n + (rank < cents % n ? 1 : 0);
    }
    /** One electric meter per room: the reading of that month that anyone in the room submitted (the highest one if they differ), or null. */
    static double[] roomReading(List<Boarder> occ, int y, int m) {
        double[] best = null;
        for (Boarder o : occ) { double[] r = o.readings.get(y * 100 + m); if (r != null && (best == null || r[1] > best[1])) best = r; }
        return best;
    }
    /** The room's previous meter reading (starting reading is 0). */
    static double roomPrev(List<Boarder> occ, int y, int m) {
        double p = 0;
        for (Boarder o : occ) p = Math.max(p, prevReading(o, y, m));
        return p;
    }

    static class Bill {
        double rent, water, prev, total; Double elec, present, kwh; boolean[] paid = new boolean[3];
        List<Integer> unpaid = new ArrayList<>(); boolean full; String status;
        double amt(int k) { return k == 0 ? rent : k == 1 ? water : (elec == null ? Double.NaN : elec); }
    }
    static Bill bill(Boarder b, int y, int m) {
        List<Boarder> occ = occupants(b, y, m);
        Bill r = new Bill(); r.rent = rentShare(b, y, m); r.water = d.water; r.prev = roomPrev(occ, y, m);
        double[] rd = roomReading(occ, y, m);
        if (rd != null) { r.prev = rd[0]; r.present = rd[1]; r.kwh = rd[1] - rd[0]; r.elec = split(Math.round(r.kwh * rd[2] * 100), occ, b) / 100.0; }
        int[] mk = masks(b, y);
        for (int k = 0; k < 3; k++) { r.paid[k] = isPaid(mk[k], m); if (!r.paid[k]) r.unpaid.add(k); }
        r.total = r.rent + r.water + (r.elec == null ? 0 : r.elec);
        r.full = r.unpaid.isEmpty();
        r.status = r.full ? "Paid" : (LocalDate.now().isAfter(dueDate(y, m)) ? "Overdue" : "Unpaid");
        return r;
    }
    /** Boarder submits only the present reading; previous is carried over automatically. */
    static String submitReading(int id, int y, int m, double present) {
        load(); Boarder b = find(id);
        if (d.rate <= 0) return "Admin has not set the electricity rate yet.";
        List<Boarder> occ = occupants(b, y, m);                
        double[] have = roomReading(occ, y, m);                // shared room: the first reading of the month wins, roommates cannot change it
        if (have != null && occ.size() > 1)
            return "Your roommate already submitted " + have[1] + " (" + (have[1] - have[0]) + " kWh) for this month.";
        for (Boarder o : occ) if (isPaid(masks(o, y)[2], m)) return "Electricity for this month is already paid.";
        final double prev = roomPrev(occ, y, m);
        if (present < prev) return "Present reading cannot be lower than previous (" + prev + ").";
        for (Boarder o : occ) {
            final int oid = o.id;
            o.readings.put(y * 100 + m, new double[]{prev, present, d.rate});
            db(() -> BoardingDatabase.saveReading(oid, y, m, prev, present, d.rate));
        }
        return null;
    }
   
    static void pay(int id, int y, int m, boolean[] items) {
        load(); Boarder b = find(id);
        boolean undone = false;
        for (int k = 0; k < 3; k++) {
            int[] mk = masks(b, y);
            boolean now = isPaid(mk[k], m);
            if (items[k] == now) continue;                                        
            if (items[k]) {                                                     
                if (k == 2 && roomReading(occupants(b, y, m), y, m) == null) continue;
                mk[k] = mark(mk[k], m);
                Notif n = new Notif(); n.bid = id; n.year = y; n.month = m; n.kind = "paid" + k; n.sent = 1; n.due = LocalDateTime.now();
                n.msg = "Payment received: " + KEYS[k] + " for " + MONTHS[m - 1] + " " + y + ". Thank you!";
                d.notifs.add(n);
                db(() -> { BoardingDatabase.saveMasks(id, y, mk); BoardingDatabase.insertNotif(n); });
            } else {                                                             
                undone = true;
                mk[k] &= ~bit(m);
                final String kind = "paid" + k;
                List<Notif> hide = new ArrayList<>();
                for (Notif n : d.notifs)
                    if (n.bid == id && n.year == y && n.month == m && n.kind.equals(kind) && n.sent == 1) { n.sent = 2; hide.add(n); }
                db(() -> { BoardingDatabase.saveMasks(id, y, mk); for (Notif n : hide) BoardingDatabase.updateNotif(n); });
            }
        }
        if (undone && !bill(b, y, m).full) {                                     
            Notif last = null; LocalDateTime nowT = LocalDateTime.now();
            for (Notif n : d.notifs)
                if (n.bid == id && n.year == y && n.month == m && !n.kind.startsWith("paid") && !n.due.isAfter(nowT)
                        && (last == null || n.due.isAfter(last.due))) last = n;
            if (last != null && last.sent != 0) { last.sent = 0; final Notif re = last; db(() -> BoardingDatabase.updateNotif(re)); }
        }
    }

    // ------------------------- ALGORITHM 3: ROUND ROBIN (notifications)
    static boolean schedule(int y, int m) {
        boolean changed = false; LocalDateTime due = dueDate(y, m).atStartOfDay();
        String[] kinds = {"rem3", "rem1", "overdue"}; LocalDateTime[] when = {due.minusDays(3), due.minusDays(1), due.plusDays(1)};
        for (Boarder b : d.boarders) {
            if (!b.active) continue;
            for (int i = 0; i < 3; i++) {
                boolean has = false;
                for (Notif n : d.notifs) if (n.bid == b.id && n.year == y && n.month == m && n.kind.equals(kinds[i])) { has = true; break; }
                if (!has) { Notif n = new Notif(); n.bid = b.id; n.year = y; n.month = m; n.kind = kinds[i]; n.due = when[i]; d.notifs.add(n); db(() -> BoardingDatabase.insertNotif(n)); changed = true; }
            }
        }
        return changed;
    }
  
    static boolean deliverDue() {
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, List<Notif>> perBoarder = new LinkedHashMap<>();
        for (Notif n : d.notifs)
            if (n.sent == 0 && !n.due.isAfter(now))
                perBoarder.computeIfAbsent(n.bid, k -> new ArrayList<>()).add(n);

        ArrayDeque<ArrayDeque<Notif>> ring = new ArrayDeque<>();
        for (List<Notif> list : perBoarder.values()) {
            list.sort(Comparator.comparing((Notif n) -> n.due));          // oldest reminder first
            ring.add(new ArrayDeque<>(list));
        }
        boolean changed = false;
        while (!ring.isEmpty()) {
            ArrayDeque<Notif> q = ring.poll();                             // boarder whose turn it is
            deliverOne(q.poll()); changed = true;                          // serve one notification
            if (!q.isEmpty()) ring.add(q);                                 // back of the ring for the next round
        }
        return changed;
    }
    /** Sends (or cancels, if already paid) a single notification. */
    static void deliverOne(Notif n) {
        Boarder b = find(n.bid);
        Bill bl = bill(b, n.year, n.month);
        if (bl.full) { n.sent = 2; db(() -> BoardingDatabase.updateNotif(n)); return; }                 // already paid -> cancel reminder
        StringBuilder s = new StringBuilder();
        for (int k : bl.unpaid) s.append(s.length() > 0 ? ", " : "").append(KEYS[k]).append(" (")
                .append(Double.isNaN(bl.amt(k)) ? "reading needed" : peso(bl.amt(k))).append(")");
        String pre = n.kind.equals("rem3") ? "Reminder (3 days left)" : n.kind.equals("rem1") ? "Reminder (due tomorrow)" : "OVERDUE";
        n.msg = pre + ": " + MONTHS[n.month - 1] + " bill, due " + dueDate(n.year, n.month) + ". Unpaid: " + s;
        n.sent = 1; db(() -> BoardingDatabase.updateNotif(n));
    }
    /** Reload shared data, schedule + deliver notifications. */
    static void tick(int y, int m) { load(); schedule(y, m); deliverDue(); }
}