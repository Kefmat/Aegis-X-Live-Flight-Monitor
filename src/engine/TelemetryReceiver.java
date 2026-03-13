package engine;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.Deque;
import model.TelemetryData;
import model.FlightPhase;

/**
 * Håndterer nettverkskommunikasjon via UDP i en egen tråd.
 * Lytter etter innkommende telemetripakker og validerer dem mot systemkrav (inkl. Geofence).
 * Inkluderer FTS, Network Health Monitor, Black Box-opptak og Remote Command-støtte.
 */
public class TelemetryReceiver implements Runnable {
    private int port;
    private volatile boolean running; 
    private ConfigLoader config;
    private LogManager logger;
    private BlackBoxProvider blackBox;
    
    // FTS-variabler
    private int violationCount = 0;
    private final int MAX_VIOLATIONS = 5;
    private boolean ftsTriggered = false;

    // Heartbeat/Link-variabler
    private long lastPacketTime = 0;
    private final long LINK_TIMEOUT = 3000; 

    // Visualisering - Lagrer de siste 20 hastighetsmålingene for grafen
    private Deque<Double> speedHistory = new ArrayDeque<>();
    private final int MAX_HISTORY = 20;

    /**
     * Oppretter en ny mottaker for telemetri med Black Box-støtte.
     * @param port Nettverksporten det skal lyttes på.
     * @param config Konfigurasjonsobjektet som inneholder grenseverdier.
     * @param logger Logghåndterer for avviksrapportering.
     * @param blackBox Opptaksmekanisme for alle flydata.
     */
    public TelemetryReceiver(int port, ConfigLoader config, LogManager logger, BlackBoxProvider blackBox) {
        this.port = port;
        this.config = config;
        this.logger = logger;
        this.blackBox = blackBox;
    }

    @Override
    public void run() {
        this.running = true;
        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.setSoTimeout(500); 
            
            System.out.println("[NETWORK] Mottaker-tråd startet på port " + port);
            byte[] buffer = new byte[1024];

            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    lastPacketTime = System.currentTimeMillis();

                    String received = new String(packet.getData(), 0, packet.getLength());
                    TelemetryData data = Parser.parseRawString(received);
                    
                    if (data != null) {
                        blackBox.record(data);
                        updateHistory(data.speed);
                        processData(data, socket, packet.getAddress(), packet.getPort());
                    }
                } catch (SocketTimeoutException e) {
                    checkLinkStatus();
                }
            }
        } catch (Exception e) {
            System.err.println("[NETWORK ERROR] " + e.getMessage());
        }
        System.out.println("[NETWORK] Mottaker-tråd avsluttet.");
    }

    private void updateHistory(double speed) {
        if (speedHistory.size() >= MAX_HISTORY) {
            speedHistory.pollFirst();
        }
        speedHistory.addLast(speed);
    }

    public void sendRemoteCommand(String cmdType, String value) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName("localhost");
            String fullCmd = "CMD:" + cmdType.toUpperCase() + ":" + value;
            byte[] buf = fullCmd.getBytes();
            
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 5001); 
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("[REMOTE ERROR] Kunne ikke sende kommando: " + e.getMessage());
        }
    }

    private void checkLinkStatus() {
        if (lastPacketTime > 0 && (System.currentTimeMillis() - lastPacketTime) > LINK_TIMEOUT) {
            renderLostLinkDashboard();
        }
    }

    private void processData(TelemetryData data, DatagramSocket socket, InetAddress remoteAddr, int remotePort) {
        String status = "NOMINAL";
        String speedAlert = "[ OK ]";
        String tempAlert = "[ OK ]";
        String altAlert = "[ OK ]";
        String geoAlert = "[ OK ]";
        boolean currentViolation = false;

        // Validering mot XML-krav
        if (data.speed < config.minSpeed) {
            speedAlert = "[ LOW SPEED ]";
            currentViolation = true;
        }
        if (data.temperature > config.maxTemp) {
            tempAlert = "[ OVERHEAT ]";
            currentViolation = true;
        }
        if (data.altitude > config.maxAltitude) {
            altAlert = "[ CEILING BREACH ]";
            currentViolation = true;
        }
        // Geofencing sjekk
        if (Math.abs(data.x) > config.maxX || Math.abs(data.y) > config.maxY) {
            geoAlert = "[ GEOFENCE BREACH ]";
            currentViolation = true;
        }

        if (currentViolation && !ftsTriggered) {
            violationCount++;
            if (violationCount >= MAX_VIOLATIONS) {
                sendTermination(socket, remoteAddr, remotePort);
                status = "TERMINATING";
            } else {
                status = "ALERT";
            }
        }

        renderDashboard(data, status, speedAlert, tempAlert, altAlert, geoAlert);
    }

    private void sendTermination(DatagramSocket socket, InetAddress addr, int port) {
        try {
            String cmd = "CMD:TERMINATE";
            byte[] buf = cmd.getBytes();
            socket.send(new DatagramPacket(buf, buf.length, addr, port));
            ftsTriggered = true;
        } catch (Exception e) {
            System.err.println("[FTS ERROR] " + e.getMessage());
        }
    }

    private void renderDashboard(TelemetryData data, String status, String spd, String tmp, String alt, String geo) {
        System.out.print("\033[H\033[2J");  
        System.out.flush();

        long latency = System.currentTimeMillis() - lastPacketTime;

        System.out.println("============================================================");
        System.out.println("           AEGIS-X MISSION CONTROL DASHBOARD                ");
        System.out.println("============================================================");
        System.out.println(" LINK: [ " + latency + "ms ]    FTS: [ " + (ftsTriggered ? "ABORTED" : "ACTIVE") + " ]    STATUS: [ " + status + " ]");
        System.out.println("------------------------------------------------------------");
        
        renderTrendGraph();
        renderRadarDisplay(data.x, data.y);
        
        System.out.println("------------------------------------------------------------");
        System.out.printf("   SPEED:   %-15.2f %s\n", data.speed, spd);
        System.out.printf("   ALT:     %-15.2f %s\n", data.altitude, alt);
        System.out.printf("   TEMP:    %-15.2f %s\n", data.temperature, tmp);
        System.out.printf("   POS:     X:%-7.1f Y:%-7.1f %s\n", data.x, data.y, geo);
        System.out.println("------------------------------------------------------------");
        System.out.println(" COMMANDS: stop, abort, set-throttle <val>, analyze");
        System.out.println("============================================================");
        System.out.print("> ");
    }

    private void renderTrendGraph() {
        System.out.print(" SPEED TREND: ");
        for (Double s : speedHistory) {
            if (s > 2000) System.out.print("▲");
            else if (s > 1000) System.out.print("■");
            else System.out.print("·");
        }
        System.out.println();
    }

    /**
     * Tegner et enkelt ASCII-radar display som viser missilet (+) i forhold til grensene.
     */
    private void renderRadarDisplay(double x, double y) {
        int range = 10; // Radar radius i karakterer
        System.out.println(" GEOGRAPHIC MONITOR (Radar):");
        
        // Normaliserer posisjonen til radar-skalaen
        int normX = (int) (x / config.maxX * range);
        int normY = (int) (y / config.maxY * range);

        for (int i = -5; i <= 5; i++) {
            System.out.print("   ");
            for (int j = -15; j <= 15; j++) {
                if (i == 0 && j == 0) System.out.print("o"); // Launchpad center
                else if (i == -normY / 2 && j == normX) System.out.print("+"); // Missilet
                else if (Math.abs(i) == 5 || Math.abs(j) == 15) System.out.print("#"); // Geofence grense
                else System.out.print(" ");
            }
            System.out.println();
        }
    }

    private void renderLostLinkDashboard() {
        System.out.print("\033[H\033[2J\033[flush]");
        System.out.println("!!! LOST LINK !!! CHECK TRANSMITTER");
        System.out.print("> ");
    }
    
    public void triggerManualAbort() { this.violationCount = MAX_VIOLATIONS; }
    public void stop() { this.running = false; }
}