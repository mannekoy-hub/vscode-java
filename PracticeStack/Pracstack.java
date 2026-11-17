package PracticeStack;

import javax.swing.JOptionPane;

class intStack {
    private int maxSize = 100;
    private int data[] = new int[maxSize];
    private int top = -1;

    public void push(int element) {
        if (top == maxSize - 1) {
            return;
        }

        data[++top] = element;

    }

    public int pop() {
        if (top == -1) {
            return 0;
        }
        return data[top--];
    }

    public int display() {
        if (top == -1) {
            return 0;
        }
        return data[top];
    }

    public boolean isEmpty() {
        return top == -1;
    }

}

public class Pracstack {
    public static void main(String[] args) {

        intStack stacks = new intStack();

        int loopSize = Integer.parseInt(JOptionPane.showInputDialog("Enter a Loop size: "));

        for (int i = 0; i < loopSize; i++) {

            int value = Integer.parseInt(JOptionPane.showInputDialog("Enter a number " + (i + 1) + ": "));
            System.out.println("element " + (i + 1) + ": " + value);
            stacks.push(value);
        }

        System.out.println(" ");

        System.out.println("Top index: " + stacks.display());

        int numbertoPOP = Integer.parseInt(JOptionPane.showInputDialog("Enter a Loop size: "));

        for (int i = 0; i < numbertoPOP; i++) {
            stacks.pop();
        }

        if (stacks.isEmpty()) {
            System.out.println("Empty stack");
        } else {
            System.out.println("New top index: " + stacks.display());
        }

    }
}
