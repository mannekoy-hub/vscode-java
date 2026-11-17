
 class InnerLargestnum {

    void findThe_Largest_num(){


        int num[] = {10,20,50,100,60};

        int largest = num[0];

        for(int i = 0 ; i < num.length ; i++){
              System.out.print(num[i] + " ");

              if (num[i] > largest) {
                  largest = num[i];
              }
        }
          System.out.println(" ");
          System.out.println("Largest number: " + largest);
    }
}
public class Largestnum {
    public static void main(String[] args) {
      
        InnerLargestnum L = new InnerLargestnum();
        L.findThe_Largest_num();

    }
}
