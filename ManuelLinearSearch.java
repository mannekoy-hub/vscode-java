import javax.swing.*;
public class ManuelLinearSearch {
	public static void main (String[] args) {

		 int size = Integer.parseInt(JOptionPane.showInputDialog("Enter a size: "));

		 int num[] = new int [size];

		 for(int i = 0 ; i < size ; i++){
		 	 num[i] = Integer.parseInt(JOptionPane.showInputDialog("Enter a Number" +  (i + 1) + ": "));
		 }

		 for(int i = 0 ; i < size ; i++){
		 	  System.out.print(num[i] + " ");
		 }
		 System.out.println (" ");


		 boolean found = false;
		 int location = -1;

	try{
       int InputNumber = Integer.parseInt(JOptionPane.showInputDialog("Enter a number to find: "));

         for(int i = 0; i < size; i++){
      	   if(InputNumber == num[i]){
      	   	   found = true;
      	   	   location = i;
      	   	   break;
      	   }
           }

      if(found){
      	   System.out.println ("Number " + InputNumber+ " is found at index " + location);
      }
      else{
      	System.out.println ("Number Not found.");
      }
	 }


		  catch(NumberFormatException e){
		 	   System.out.println ("Invalid input, Please enter a number!.");
		 }



}
}

