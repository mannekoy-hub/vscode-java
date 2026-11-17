package Hash_code;

import java.util.Hashtable;

public class Hashpracs {
    public static void main(String[] args) {

        int size = 55;

        Hashtable<Integer, String> table = new Hashtable<>(size);
        table.put(10, "man1");
        table.put(20, "man2");
        table.put(12, "man3");
        table.put(13, "man4");
        table.put(14, "man5");

        for(Integer key : table.keySet()){
              System.out.println(key.hashCode() % size + "\t" + key + ": " + table.get(key));
        }
    }
}
