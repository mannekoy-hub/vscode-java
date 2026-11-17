import javax.swing.JOptionPane;

class IntStack {
    private static final int MAX_SIZE = 100;
    private final int[] data = new int[MAX_SIZE];
    private int topIndex = -1;

    public void push(int element) {
        if (topIndex == MAX_SIZE - 1) {
            return;
        }
        data[++topIndex] = element;
    }

    public int pop() {
        if (topIndex == -1) {
            return 0;
        }
        return data[topIndex--];
    }

    public boolean isEmpty() {
        return topIndex == -1;
    }
}

public class PrefixExpressionEvaluator {
    public static void main(String[] args) {
        IntStack stack = new IntStack();

        String prefix = JOptionPane.showInputDialog(
            "Enter prefix expression:"
        );

        if (prefix == null) {
            return; // User cancelled the input dialog
        }

        // Scan from right to left, skipping spaces.
        for (int i = prefix.length() - 1; i >= 0; i--) {
            char c = prefix.charAt(i);

            if (Character.isWhitespace(c)) {
                continue;
            }

            if (Character.isDigit(c)) {
                stack.push(c - '0');
            } else {
                int left = stack.pop();
                int right = stack.pop();
                int result = 0;

                switch (c) {
                    case '+':
                        result = left + right;
                        break;
                    case '-':
                        result = left - right;
                        break;
                    case '*':
                        result = left * right;
                        break;
                    case '/':
                        result = left / right;
                        break;
                    default:
                        System.out.println("Unknown operator: " + c);
                        return;
                }

                stack.push(result);
            }
        }

        System.out.println("Evaluated Result: " + stack.pop());
    }
}
