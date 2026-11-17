package Hash_code;

import java.util.Hashtable;

public class Hashmain {
    public static void main(String[] args) {

        Hashtable<Integer, String> table = new Hashtable<>(10);

        table.put(100, "Bob1");
        table.put(123, "Bob2");
        table.put(321, "Bob3");
        table.put(555, "Bob4");
        table.put(777, "Bob5");

        /*
         * How remove:
         * table.remove(key);
         * Example: table.remove(777);
         * - Bob5 is remove
         */

        table.remove(777);

        for (Integer key : table.keySet()) {
            System.out.println(key + "\t" + table.get(key));
        }

    }
}
