import javax.swing.JOptionPane;

class Node {
    String data;
    Node next;

    Node(String value) {
        data = value;
        next = null;
    }
}

class Stack {
    private Node topNode;

    public void push(String element) {
        Node newNode = new Node(element);
        newNode.next = topNode;
        topNode = newNode;
    }

    public String pop() {
        if (isEmpty()) {
            return "";
        }

        String value = topNode.data;
        topNode = topNode.next;
        return value;
    }

    public String peek() {
        if (isEmpty()) {
            return "";
        }
        return topNode.data;
    }

    public boolean isEmpty() {
        return topNode == null;
    }
}

public class Browserhistory {
    public static void main(String[] args) {
        Stack history = new Stack();

        int n = Integer.parseInt(
                JOptionPane.showInputDialog("Enter number of pages to visit:"));

        for (int i = 1; i <= n; i++) {
            String title = JOptionPane.showInputDialog("Enter page title " + i + ":");
            history.push(title);
        }

        int m = Integer.parseInt( JOptionPane.showInputDialog("Enter number of back-button presses:"));

        for (int i = 0; i < m; i++) {
            if (!history.isEmpty()) {
                String removed = history.pop();
                System.out.println("Back to: " + removed);
            } else {
                System.out.println("Back to: nothing");
            }
        }

        if (!history.isEmpty()) {
            System.out.println("Now viewing: " + history.peek());
        } else {
            System.out.println("Now viewing: nothing");
        }
    }
}
