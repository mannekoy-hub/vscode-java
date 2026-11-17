import javax.swing.*;

public class Bisearch {
    public static void main(String[] args) {

        int size = Integer.parseInt(
            JOptionPane.showInputDialog("Enter size of array:")
        );

        int[] list = new int[size];

        for (int i = 0; i < size; i++) {
            list[i] = Integer.parseInt(JOptionPane.showInputDialog("Enter number " + (i + 1) + ":"));
        }

        System.out.println("====ARRAY:================");
        for (int i = 0; i < size; i++) {
            System.out.print(list[i] + " ");
        }
        System.out.println(" ");
        System.out.println("=========================== ");
        System.out.println(" ");

        for(int i = 0; i < size - 1; i++){
              for(int j = 0; j < size - i -1; j++){
                         if (list[j] > list[j + 1]) {
                            int templist = list[j];
                            list[j] = list[j + 1];
                            list[j + 1] = templist; 
                         }
              }
        }

        System.out.println("=====SORTED ARRAY: ===============");
        for (int i = 0; i < size; i++) {
            System.out.print(list[i] + " ");
        }
        System.out.println(" ");
        System.out.println("=============================");
    



        int search = Integer.parseInt(
            JOptionPane.showInputDialog("Enter number to find:")
        );


        int first = 0;
        int last = size - 1;
        int location = -1;

        while (first <= last) {

            int mid = (first + last) / 2;

            if (search == list[mid]) {
                location = mid;
                break;
            }
            else if (search > list[mid]) {
                first = mid + 1;
            }
            else {
                last = mid - 1;
            }
        }

    
        if (location != -1) {
            System.out.println( "Number " + search + " is found at index " + location);
        }
        else {
            System.out.println("Number " + search + " is NOT found");
        }
    }
}
