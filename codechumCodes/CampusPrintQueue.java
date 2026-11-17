import javax.swing.JOptionPane;

class IntStack {
    private static final int MAX_SIZE = 100;
    private final int[] data = new int[MAX_SIZE];
    private int top = -1;

    public void push(int element) {
        if (top == MAX_SIZE - 1) {
            System.out.println("Stack Overflow");
            return;
        }
        data[++top] = element;
    }

    public int pop() {
        if (top == -1) {
            System.out.println("Stack Underflow");
            return -1;
        }
        return data[top--];
    }

    public int peek() {
        if (top == -1) {
            System.out.println("Stack is empty");
            return -1;
        }
        return data[top];
    }
}

public class CampusPrintQueue {
    public static void main(String[] args) {
        IntStack printQueue = new IntStack();

        int jobId1 = Integer.parseInt(
                JOptionPane.showInputDialog("Enter job ID 1:"));
        int jobId2 = Integer.parseInt(
                JOptionPane.showInputDialog("Enter job ID 2:"));
        int jobId3 = Integer.parseInt(
                JOptionPane.showInputDialog("Enter job ID 3:"));

        printQueue.push(jobId1);
        printQueue.push(jobId2);
        printQueue.push(jobId3);

        System.out.println("Printed Job ID: " + printQueue.pop());
        System.out.println("Next Job ID: " + printQueue.peek());
        System.out.println("Second Printed Job ID: " + printQueue.pop());
    }
}
