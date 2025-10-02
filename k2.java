import java.util.*;
import java.util.concurrent.*;

interface State {
    void update(RocketContext ctx);
    String name();
}

class PreLaunch implements State {
    public void update(RocketContext ctx) {}
    public String name() { return "Pre-Launch"; }
}

class Stage1 implements State {
    public void update(RocketContext ctx) {
        ctx.consumeFuel(2);
        ctx.increaseAltitude(10);
        ctx.increaseSpeed(1000);
        if (ctx.getFuel() <= 40 || ctx.getAltitude() >= 120) {
            ctx.setState(new Stage2());
            ctx.info("Stage 1 complete. Separating stage. Entering Stage 2.");
        }
    }
    public String name() { return "Stage 1"; }
}

class Stage2 implements State {
    public void update(RocketContext ctx) {
        ctx.consumeFuel(1);
        ctx.increaseAltitude(5);
        ctx.increaseSpeed(400);
        if (ctx.getFuel() <= 5) {
            ctx.setState(new Failed());
            ctx.error("Mission Failed due to insufficient fuel.");
        }
        if (ctx.getAltitude() >= 400) {
            ctx.setState(new Orbit());
            ctx.info("Orbit achieved! Mission Successful.");
        }
    }
    public String name() { return "Stage 2"; }
}

class Orbit implements State {
    public void update(RocketContext ctx) {}
    public String name() { return "Orbit"; }
}

class Failed implements State {
    public void update(RocketContext ctx) {}
    public String name() { return "Failed"; }
}

class RocketContext {
    private State state = new PreLaunch();
    private int fuel = 100;
    private double altitude = 0;
    private double speed = 0;
    private int time = 0;
    private final List<RocketObserver> observers = new ArrayList<>();

    public void setState(State s) { state = s; }
    public void addObserver(RocketObserver o) { observers.add(o); }
    public void tick() {
        state.update(this);
        time++;
        notifyUpdate();
    }
    public void fastForward(int sec) {
        for (int i = 0; i < sec; i++) {
            if (state instanceof Orbit || state instanceof Failed) break;
            tick();
        }
    }
    public void launch() {
        if (state instanceof PreLaunch) {
            setState(new Stage1());
            info("Launch initiated.");
        }
    }
    public void checks() { info("All systems are 'Go' for launch."); }
    public void consumeFuel(int amt) { fuel = Math.max(0, fuel - amt); }
    public void increaseAltitude(double amt) { altitude += amt; }
    public void increaseSpeed(double amt) { speed += amt; }
    public int getFuel() { return fuel; }
    public double getAltitude() { return altitude; }
    public double getSpeed() { return speed; }
    public int getTime() { return time; }
    public String getStage() { return state.name(); }
    private void notifyUpdate() {
        for (RocketObserver o : observers) o.update(this);
    }
    public void info(String msg) {
        for (RocketObserver o : observers) o.info(msg);
    }
    public void error(String msg) {
        for (RocketObserver o : observers) o.error(msg);
    }
}

interface RocketObserver {
    void update(RocketContext ctx);
    void info(String msg);
    void error(String msg);
}

class ConsoleObserver implements RocketObserver {
    public void update(RocketContext ctx) {
        System.out.printf("Stage: %s, Fuel: %d%%, Altitude: %.1f km, Speed: %.0f km/h%n",
                ctx.getStage(), ctx.getFuel(), ctx.getAltitude(), ctx.getSpeed());
    }
    public void info(String msg) { System.out.println(msg); }
    public void error(String msg) { System.out.println(msg); }
}

interface Command {
    void execute(String[] args, RocketContext ctx);
}

class StartChecks implements Command {
    public void execute(String[] args, RocketContext ctx) { ctx.checks(); }
}

class Launch implements Command {
    public void execute(String[] args, RocketContext ctx) { ctx.launch(); }
}

class FastForward implements Command {
    public void execute(String[] args, RocketContext ctx) {
        if (args.length < 2) throw new IllegalArgumentException("fast_forward <seconds>");
        ctx.fastForward(Integer.parseInt(args[1]));
    }
}

class Tick implements Command {
    public void execute(String[] args, RocketContext ctx) { ctx.tick(); }
}

class Exit implements Command {
    public void execute(String[] args, RocketContext ctx) { System.exit(0); }
}

class CommandRouter {
    private final Map<String, Command> cmds = new HashMap<>();
    public void register(String key, Command cmd) { cmds.put(key, cmd); }
    public void handle(String input, RocketContext ctx) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) return;
        Command c = cmds.get(parts[0]);
        if (c == null) throw new IllegalArgumentException("Unknown command: " + parts[0]);
        c.execute(parts, ctx);
    }
}

public class RocketSimulator {
    public static void main(String[] args) {
        RocketContext rocket = new RocketContext();
        rocket.addObserver(new ConsoleObserver());

        CommandRouter router = new CommandRouter();
        router.register("start_checks", new StartChecks());
        router.register("launch", new Launch());
        router.register("fast_forward", new FastForward());
        router.register("tick", new Tick());
        router.register("exit", new Exit());

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(rocket::tick, 1, 1, TimeUnit.SECONDS);

        Scanner sc = new Scanner(System.in);
        System.out.println("Commands: start_checks, launch, fast_forward X, tick, exit");
        while (true) {
            try {
                String line = sc.nextLine();
                router.handle(line, rocket);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
