package Hash_code;

import java.util.Hashtable;

import javax.swing.*;

public class Hashprac {
    public static void main(String[] args) {

        // int tableSize = Integer.parseInt(JOptionPane.showInputDialog("Enter a table
        // Size: "));

        Hashtable<Integer, String> table = new Hashtable<>(10);

        int Loop = Integer.parseInt(JOptionPane.showInputDialog("Enter a Loop Size: "));
        if (Loop > 10) {
            System.out.println("Loop Size out of bounds");
            return;
        }

        for (int i = 0; i < Loop; i++) {
            int keys = Integer.parseInt(JOptionPane.showInputDialog("Enter a Keys: "));
            String values = (JOptionPane.showInputDialog("Enter a Values: "));
            table.put(keys, values);

        }

        for (Integer key : table.keySet()) {
            System.out.println(key.hashCode() % 10 + "\t" + key + ": " + table.get(key));
        }
    }
}
