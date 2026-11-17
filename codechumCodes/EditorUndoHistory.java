import javax.swing.JOptionPane;

class StringStack {
    private static final int MAX_SIZE = 100;
    private final String[] data = new String[MAX_SIZE];
    private int top = -1;

    public void push(String element) {
        if (top == MAX_SIZE - 1) {
            return;
        }
        data[++top] = element;
    }

    public String pop() {
        if (top == -1) {
            return "";
        }
        return data[top--];
    }

    public String peek() {
        if (top == -1) {
            return "";
        }
        return data[top];
    }

    public boolean isEmpty() {
        return top == -1;
    }
}

public class EditorUndoHistory {
    public static void main(String[] args) {
        StringStack history = new StringStack();

        int n = Integer.parseInt(
            JOptionPane.showInputDialog("Enter number of actions:")
        );

        for (int i = 0; i < n; i++) {
            String action = JOptionPane.showInputDialog("Enter action:");
            history.push(action);
        }

        int undos = Integer.parseInt(
            JOptionPane.showInputDialog("Enter number of undos:")
        );

        for (int i = 0; i < undos; i++) {
            if (history.isEmpty()) {
                break;
            }
            history.pop();
        }

        if (history.isEmpty()) {
            System.out.println("Current action: None");
        } else {
            System.out.println("Current action: " + history.peek());
        }
    }
}