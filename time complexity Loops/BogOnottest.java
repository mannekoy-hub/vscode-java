
 class InnerBogOnottest {

    static long addup(long n){
        long sum = n * (n + 1)/2;
        return sum;
    }

    void call(){
        System.out.println("DONE!");
    }

    
}

public class BogOnottest { 
    public static void main(String[] args) {

        InnerBogOnottest I = new InnerBogOnottest();

    long put_n = InnerBogOnottest.addup(1000000);
        System.out.println(put_n);

        I.call();

    }
}
