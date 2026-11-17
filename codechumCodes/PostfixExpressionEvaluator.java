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

public class PostfixExpressionEvaluator {
    public static void main(String[] args) {
        IntStack stack = new IntStack();

        String postfix = JOptionPane.showInputDialog(
            "Enter postfix expression:"
        );

        if (postfix == null) {
            return; // User cancelled the input dialog
        }

        // Scan from left to right, skipping spaces.
        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);

            if (Character.isWhitespace(c)) {
                continue;
            }

            if (Character.isDigit(c)) {
                stack.push(c - '0');
            } else {
                // In postfix, the first pop is the right operand.
                int right = stack.pop();
                int left = stack.pop();
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
                        if (right == 0) {
                            System.out.print("Error: Division by zero\n");
                            return;
                        }
                        result = left / right;
                        break;
                    default:
                        System.out.print("Unknown operator: " + c + "\n");
                        return;
                }

                stack.push(result);
            }
        }

        System.out.print("Evaluated Result: " + stack.pop() + "\n");
    }
}
