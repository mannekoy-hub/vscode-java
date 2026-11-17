package Hash_code;

import javax.swing.*;

public class Hashing {
    public static void main(String[] args) {
        int size = 10;
        int[] hashtable = new int[size];

        for (int i = 0; i < size; i++) {
            hashtable[i] = -1;
        }

        int Loop = Integer.parseInt(JOptionPane.showInputDialog("Enter Loops: "));

        for (int i = 0; i < Loop; i++) {
            int number = Integer.parseInt(JOptionPane.showInputDialog("Enter numbers " + (i + 1) + ": "));

            int index = number % size;

            for (; hashtable[index] != -1; index = (index + 1) % size) {

            }

            hashtable[index] = number;
        }

        System.out.println("Hash Table");
        System.out.println("Index\tValue");

        for (int i = 0; i < size; i++) {
            System.out.println(i + "\t" + hashtable[i]);
        }

    }
}
