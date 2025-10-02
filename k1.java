import java.util.*;

// === Direction as Enum with behavior ===
enum Direction {
    NORTH, EAST, SOUTH, WEST;
    private static final List<Direction> order = List.of(NORTH, EAST, SOUTH, WEST);

    public Direction left() {
        return order.get((order.indexOf(this) + 3) % 4);
    }
    public Direction right() {
        return order.get((order.indexOf(this) + 1) % 4);
    }
    public Position move(Position pos) {
        return switch (this) {
            case NORTH -> pos.translate(0, 1);
            case EAST  -> pos.translate(1, 0);
            case SOUTH -> pos.translate(0, -1);
            case WEST  -> pos.translate(-1, 0);
        };
    }
}

// === Position Value Object ===
class Position {
    private final int x, y;
    public Position(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    public Position translate(int dx, int dy) { return new Position(x + dx, y + dy); }
    public boolean equals(Object o) {
        if (!(o instanceof Position p)) return false;
        return x == p.x && y == p.y;
    }
    public int hashCode() { return Objects.hash(x, y); }
    public String toString() { return "(" + x + ", " + y + ")"; }
}

// === Composite Grid ===
interface Terrain {
    boolean isBlocked(Position pos);
}
class EmptyCell implements Terrain {
    public boolean isBlocked(Position pos) { return false; }
}
class Obstacle implements Terrain {
    private final Position position;
    public Obstacle(Position pos) { this.position = pos; }
    public boolean isBlocked(Position pos) { return position.equals(pos); }
}
class Grid implements Terrain {
    private final int width, height;
    private final List<Terrain> children = new ArrayList<>();
    public Grid(int width, int height) { this.width = width; this.height = height; }
    public void add(Terrain t) { children.add(t); }
    public boolean isBlocked(Position pos) {
        if (pos.getX() < 0 || pos.getX() >= width || pos.getY() < 0 || pos.getY() >= height) return true;
        return children.stream().anyMatch(c -> c.isBlocked(pos));
    }
}

// === Rover ===
class Rover {
    private Position position;
    private Direction direction;
    private final Terrain terrain;

    public Rover(Position start, Direction dir, Terrain terrain) {
        this.position = start; this.direction = dir; this.terrain = terrain;
    }

    public void execute(Command command) { command.apply(this); }
    public void turnLeft() { direction = direction.left(); }
    public void turnRight() { direction = direction.right(); }
    public void move() {
        Position next = direction.move(position);
        if (!terrain.isBlocked(next)) position = next;
    }

    public String report() { return "Rover is at " + position + " facing " + direction; }
}

// === Command Pattern ===
interface Command { void apply(Rover rover); }
class Move implements Command { public void apply(Rover r) { r.move(); } }
class Left implements Command { public void apply(Rover r) { r.turnLeft(); } }
class Right implements Command { public void apply(Rover r) { r.turnRight(); } }

// === Client ===
public class MarsRoverAlt {
    public static void main(String[] args) {
        Grid grid = new Grid(10, 10);
        grid.add(new Obstacle(new Position(2, 2)));
        grid.add(new Obstacle(new Position(3, 5)));

        Rover rover = new Rover(new Position(0, 0), Direction.NORTH, grid);

        Map<Character, Command> commandMap = Map.of(
                'M', new Move(),
                'L', new Left(),
                'R', new Right()
        );

        List<Character> input = List.of('M', 'M', 'R', 'M', 'L', 'M');
        input.forEach(c -> rover.execute(commandMap.get(c)));

        System.out.println(rover.report());
    }
}
