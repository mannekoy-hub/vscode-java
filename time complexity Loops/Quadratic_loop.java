public class Quadratic_loop {
    public static void main(String[] args) {

// Usually a loop inside a loop. - O(n^2)
        
        for(int i = 0 ; i < 10 ; i++){
             for(int j = 0 ; j < 10 ; j++){
                   System.out.print(i + " ");
             }
        }
    }
}
