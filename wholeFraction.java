public class wholeFraction {
    public static void main(String[] args) {
       
        double OG_value = 10.345;
        int whole_num = (int) OG_value;
        double fractional_part = OG_value - whole_num;

       System.out.println("Original value: " + OG_value);
       System.out.println("whole num: " + whole_num);
       System.out.println("fractional_part: " + fractional_part );
    }
    
}
