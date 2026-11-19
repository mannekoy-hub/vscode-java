package DSAproject;

public class Knapsack {
    public static void main(String[] args) {
        int[] cost = {2, 3, 4, 5};
        int[] priority = {20,30,50,60};

        int capacity = 7;
        int n = priority.length;

        int[][] dp = new int[n + 1][capacity + 1];

        for(int i = 1; i <= n; i++){
                            //   7
          for(int w = 1; w <= capacity; w++){
                   if (cost[i - 1] <= w) {
                      int include = priority[i-1]+ dp[i-1][w-cost[i-1]];

                      int exclude = dp[i-1][w];

                      dp[i][w] = Math.max(include, exclude);
                    
                   }
                   else{
                     dp[i][w] = dp[i - 1][w];
                   }
          }
        }

        System.out.println("Maximum Value: " + dp[n][capacity]);
    }
    
}

/*my prof it want a GUI design like a fully App ready to market
it has a dashboard.

make the design like the picture on the right but make it unique.
dont make it AI slop.
dont make bugs and errrors
dont change anything just update a GUI 
your the one to decide on the UI/UX of our system App */
