import javax.swing.*;

public class Marv2{
    public static void main (String[] args) {

    	int size = Integer.parseInt(JOptionPane.showInputDialog("Enter an array size: "));

    	int numberlist[] = new int[size];

    	for(int i = 0 ; i < size ; i++){
    	        numberlist[i] = Integer.parseInt(JOptionPane.showInputDialog("Enter an array number " + (i + 1) + ": "));
    	}

           System.out.println ("ARRAY:");
    		for(int i = 0 ; i < size ; i++){
    			System.out.print(numberlist[i] + " ");
    	}

    	   System.out.print("\n" + "" + "\n" + "\n");

    	   int first = 0;
    	   int last = size - 1;
    	   int mid;
    	   boolean found = false;    
    	   int location = -1;
    try{
    	   int Inputnumber = Integer.parseInt(JOptionPane.showInputDialog("Enter a number to find: "));

    	   for(;first <= last;){
    	   	   mid = (first+last)/2;

    	   	   	if(Inputnumber > numberlist[mid]){
    	   	   		first = mid + 1;
    	   	   	}
    	   	   	else if(Inputnumber < numberlist[mid]){
    	   	   		last = mid - 1;
    	   	   	}
    	   	   	else{
    	   	   		found = true;
    	   	   	    location = mid;
    	   	   		break;
    	   	   	}
    	   }

    	   if(found){
    	   	  System.out.println ("Number " + Inputnumber + " is Found at Index " + location);
    	   }
    	   else{
    	   	 System.out.println ("Number " + Inputnumber + " is Not found inside the array");
    	   }


}
catch(NumberFormatException e){
	   System.out.println ("Invalid input, Please enter a number");
}
    }


}
