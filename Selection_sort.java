import javax.swing.*;
public class Selection_sort{
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

    for(int i = 0; i < size - 1; i++ ){
         int minIndex = i; 

         for(int j = i + 1 ; j < size; j++ ){  
             if (num[j] < num[minIndex] ) {
                minIndex = j;
             }
         }

             int numtemp = num[i];
             num[i] = num[minIndex];
             num[minIndex] = numtemp;
         
    }

   System.out.println("== Sorted array: ======== ");
    for(int i = 0; i < size ; i++ ){
        System.out.print(num[i] + " ");
    }  
    }
}