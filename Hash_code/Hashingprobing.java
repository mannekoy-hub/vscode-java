package Hash_code;

import javax.swing.JOptionPane;

import org.omg.CORBA.IntHolder;

public class Hashingprobing {
    public static void main(String[] args) {

        int size = 5;
        int hashtable[] = new int[size];

        for (int i = 0; i < size; i++) {
            hashtable[i] = -1;
        }

        int loops = Integer.parseInt(JOptionPane.showInputDialog("Enter number to Loop"));
        if (loops > size) {
            System.out.println("Loop out of bounds");
            JOptionPane.showMessageDialog(null, "Loop out of bounds");
            return;
        }

        for (int i = 0; i < loops; i++) {
            int numbers = Integer.parseInt(JOptionPane.showInputDialog("Enter number " + (i + 1) + ": "));

            int index = numbers % size;

            for (; hashtable[index] != -1; index = (index + 1) % size) {
            }
            hashtable[index] = numbers;
        }

        System.out.println("Hash Probing");
        System.out.println("Index\tValues");

        for (int i = 0; i < size; i++) {
            System.out.println(i + "\t" + hashtable[i]);
        }
    }
}
