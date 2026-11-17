import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
public class WordFrequenzyAnalyzer {
    public static void main(String[] args) {
     String p_graph = JOptionPane.showInputDialog("Enter a Paragraph: ");
    
      p_graph = p_graph.toLowerCase().replace("[^a-z\\s]", "");

      String[] words = p_graph.split("\\s+");

      Map<String, Integer> freq = new HashMap<>();
      for(String word : words){
        freq.put(word, freq.getOrDefault(word, 0) + 1 );
      }

         List<Map.Entry<String, Integer>> sortedlist = new ArrayList<>(freq.entrySet());
         sortedlist.sort((a, b) -> {
              if (!a.getValue().equals(b.getValue())) {
                return b.getValue() - a.getValue();
              }
              return a.getKey().compareTo(b.getKey());
            });

            for(Map.Entry<String, Integer> entry : sortedlist){
                System.out.println(entry.getKey() + " - " + entry.getValue());
            }




    }
}
