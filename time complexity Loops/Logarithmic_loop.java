public class Logarithmic_loop{
public static void main(String[] args) {

// A loop where the problem gets smaller by factor each iteration, commonly by half. - O (log n)
    
    //MULTIPLY:
       System.out.println("==== MULTIPLLY: ============");
   for(int i = 1 ; i <=1000 ; i*=2){
        System.out.println(i + " ");
   }

   //DIVIDE
    System.out.println(" ");

   System.out.println("==== DIVIDE: ============");
   for(int i = 1000 ; i >= 1 ; i/=2){
        System.out.println(i + " ");
   }

}
}