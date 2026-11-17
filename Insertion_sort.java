import javax.swing.*;
public class Insertion_sort {
  public static void main(String[] args) {
            
     int size = Integer.parseInt(JOptionPane.showInputDialog("Enter a size: "));

     int num[] = new int[size];

     for(int i = 0 ; i < size ; i++){
            
        num[i] = Integer.parseInt(JOptionPane.showInputDialog("Enter a number: "));
     }
      System.out.println("==UnSorted array: ======== ");
     for(int i = 0 ; i < size ; i++){
        System.out.print(num[i]+ " ");
     }
     System.out.println(" " + " " + "\n" + "\n");

    for(int i = 1; i < size ; i++){
       int key = num[i];
       int j;

     for(j = i - 1 ; j >= 0 && num[j] > key ; j--){
         num[j+1] = num[j];
     }

     num[j+1] = key;

    }

    
   System.out.println("== Sorted array: ======== ");
    for(int i = 0; i < size ; i++ ){
        System.out.print(num[i] + " ");
    } 

  }  
}
