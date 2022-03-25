package src;

public class Position {
    public int x;
    public int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Position(int[] pos) {
        this.x = pos[0];
        this.y = pos[1];
    }

    @Override
    public int hashCode() {
        return 100 * y + x;
    }

    @Override
    public boolean equals(Object obj) {
        Position p = (Position) obj;
        if (this.x == p.x && this.y == p.y) return true;
        return false;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    public static Position gotInR(Position pos, int r) {
        int x = pos.x;
        int y = pos.y;
        switch (r) {
            case 0:
                y = y - 1;
                break;
            case 1:
                x = x + 1;
                y =y - 1;
                break;
            case 2:
                x = x + 1;
                break;
            case 3:
                x= x + 1;
                y = y + 1;
                break;
            case 4:
                y = y + 1;
                break;
            case 5:
                x = x - 1;
                y = y + 1;
                break;
            case 6:
                x = x - 1;
                break;
            case 7:
                x = x - 1;
                y = y - 1;
                break;
        }
        return new Position(x,y);
    }
}
