import javax.swing.JOptionPane;

class StringStack {
    private static final int MAX_SIZE = 100;
    private final String[] data = new String[MAX_SIZE];
    private int topIndex = -1;

    public void push(String element) {
        if (topIndex < MAX_SIZE - 1) {
            data[++topIndex] = element;
        }
    }

    public String pop() {
        if (topIndex >= 0) {
            return data[topIndex--];
        }
        return " ";
    }

    public boolean isEmpty() {
        return topIndex == -1;
    }
}

public class WordSequenceReverser {
    public static void main(String[] args) {
        StringStack stack = new StringStack();

        String sentence = JOptionPane.showInputDialog("Enter a sentence:");
        if (sentence == null) {
            return; // User cancelled the dialog
        }

        StringBuilder word = new StringBuilder();

        for (int i = 0; i < sentence.length(); i++) {
            char current = sentence.charAt(i);

            if (current == ' ') {
                if (word.length() > 0) {
                    stack.push(word.toString());
                    word.setLength(0);
                }
            } else {
                word.append(current);
            }
        }

        if (word.length() > 0) {
            stack.push(word.toString());
        }

        System.out.println("Reversed words:");
        while (!stack.isEmpty()) {
            System.out.println(stack.pop());
        }
    }
}
