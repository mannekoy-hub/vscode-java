import javax.swing.*;

public class BinarySearch {
    public static void main(String[] args) {

        int size = Integer.parseInt(JOptionPane.showInputDialog("Enter a size: "));

        int num[] = new int[size];

        for (int i = 0; i < size; i++) {
            num[i] = Integer.parseInt(JOptionPane.showInputDialog("Enter  Number " + (i + 1) + ": "));
        }

        System.out.println("========= Unsorted Array: =================");
        for (int i = 0; i < size; i++) {
            System.out.print(num[i] + " ");
        }
        System.out.println(" ");
        System.out.println("===========================================");

        for (int i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - i - 1; j++) {
                if (num[j] > num[j + 1]) {
                    int numtemp = num[j];
                    num[j] = num[j + 1];
                    num[j + 1] = numtemp;
                }

            }
        }

        System.out.println("========= Sorted Array: =================");
        for (int x = 0; x < size; x++) {
            System.out.print(num[x] + " ");
        }
        System.out.println(" ");
        System.out.println("===========================================");

        int Inputnumber = Integer.parseInt(JOptionPane.showInputDialog("Enter a Number to find: "));

        int first = 0;
        int last = size - 1;
        int mid;
        boolean found = false;
        int location = -1;

        for (; first <= last;) {
            mid = (first + last) / 2;

            if (Inputnumber > num[mid]) {
                first = mid + 1;
            } else if (Inputnumber < num[mid]) {
                last = mid - 1;
            } else {
                found = true;
                location = mid;
                break;
            }
        }

        if (found) {
            System.out.println("Number " + Inputnumber + " is Found at Index " + location);
        } else {
            System.out.println("Number " + Inputnumber + "is not found");
        }

        int largest = num[0];
        for (int i = 0; i < size; i++) {
            if (num[i] > largest) {
                largest = num[i];
            }
        }

        System.out.println("The largest number: " + largest);

    }
}
