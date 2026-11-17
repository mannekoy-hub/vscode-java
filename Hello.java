import javax.swing.*;
public class Hello{
    public static void main (String [] args){
        String name = JOptionPane.showInputDialog("Input:");
        System.out.println("Hello world " + name);

        
     int num[] = {1,2,3,4,5};

    int sum = 0;
  
   for(int i = 0 ; i < num.length ; i++){
       sum = num[i] + sum;
   }

   System.out.println(sum + " ");

}
}
    
