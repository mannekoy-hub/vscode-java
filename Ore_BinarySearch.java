
import javax.swing.*;
import java.util.Arrays;

public class Ore_BinarySearch{
    public static void main(String[] args) {

        int size = Integer.parseInt(
            JOptionPane.showInputDialog("Enter a Size: ")
        );

        int numlist[] = new int[size];

        for(int i = 0; i < size; i++) {
            numlist[i] = Integer.parseInt(
                JOptionPane.showInputDialog(
                    "Enter an Array Number " + (i + 1) + ": "
                )
            );
        }

        System.out.println("===== ORIGINAL ARRAY: =================");
        for(int i = 0; i < size; i++) {
            System.out.print(numlist[i] + " ");
        }

        System.out.println(" ");
        System.out.println("========================================");
        System.out.println(" ");

      
        Arrays.sort(numlist);

        System.out.println("===== SORTED ARRAY: ====================");
        for(int i = 0; i < size; i++) {
            System.out.print(numlist[i] + " ");
        }

        System.out.println(" ");
        System.out.println("========================================");
        System.out.println(" ");

        try {
            int inputNum = Integer.parseInt(
                JOptionPane.showInputDialog("Enter a Number to find: ")
            );

            int first = 0;
            int last = size - 1;
            int mid;
            boolean found = false;
            int location = -1;

            for(; first <= last;) {

                mid = (first + last) / 2;

                if(inputNum > numlist[mid]) {
                    first = mid + 1;
                }
                else if(inputNum < numlist[mid]) {
                    last = mid - 1;
                }
                else {
                    found = true;
                    location = mid;
                    break;
                }
            }

            if(found) {
                System.out.println(
                    "Number " + inputNum +
                    " is found at Index " + location
                );
            }
            else {
                System.out.println(
                    "Number " + inputNum + " Not found"
                );
            }
        }
        catch(NumberFormatException e) {
            System.out.println(
                "Input invalid, Please enter a number"
            );
        }
    }
}