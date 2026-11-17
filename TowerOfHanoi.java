import javax.swing.JOptionPane;

class Tower_Of_Hanoi {

    void towerofhanoi(int n, char Beg, char Aux, char End, int[] moves) {
        if (n == 1) {
            System.out.println("Move disk 1 from " + Beg + " to " + End);

            moves[0]++;

            return;
        }

        towerofhanoi(n - 1, Beg, End, Aux, moves);
        System.out.println("Move disk " + n + " from " + Beg + " to " + End);

        moves[0]++;

        towerofhanoi(n - 1, Aux, Beg, End, moves);

    }

}

public class TowerOfHanoi {
    public static void main(String[] args) {

        Tower_Of_Hanoi tower = new Tower_Of_Hanoi();

        int disk = Integer.parseInt(JOptionPane.showInputDialog("Enter N: "));
        int[] moves = { 0 };

        tower.towerofhanoi(disk, 'A', 'B', 'C', moves);

        System.out.println("Total moves: " + moves[0]);

    }
}
