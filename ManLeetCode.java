import java.util.Arrays;

import javax.swing.*;
class Solution {
    public int[] twoSum(int[] nums, int target) {
 
        
        for(int i = 0 ; i < nums.length ; i++){
              for(int j = i+1 ; j < nums.length ; j++){
                   if (nums[i] + nums[j] == target) {
                    System.out.println("Found the Eqaul:");
                         return new int[]{i,j};
                   }
              }
        }
        System.out.println("Nothing found");
        return new int[]{-1,-1};
    }
}


public class ManLeetCode {
    public static void main(String[] args) {
        
      Solution s = new Solution();


        int Size = Integer.parseInt(JOptionPane.showInputDialog("Enter a Size"));

        int num[] = new int[Size];

        for (int i = 0; i < Size; i++) {
            num[i] = Integer.parseInt(JOptionPane.showInputDialog("Enter a Number " + (i + 1) + ": "));
        }

        
        for (int i = 0; i < Size; i++) {
            System.out.print(num[i] + " ");
        }
        System.out.println(" ");

         int Target = Integer.parseInt(JOptionPane.showInputDialog("Enter a Target"));

        int [] result = s.twoSum(num,Target);

        System.out.println(Arrays.toString(result));

        
}
}