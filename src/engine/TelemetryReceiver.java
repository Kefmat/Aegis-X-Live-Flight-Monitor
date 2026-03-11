package engine;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import model.TelemetryData;
import model.FlightPhase;

/**
 * Håndterer nettverkskommunikasjon via UDP i en egen tråd.
 * Lytter etter innkommende telemetripakker og validerer dem mot systemkrav.
 * Inkluderer FTS, Network Health Monitor og Black Box-opptak.
 */
public class TelemetryReceiver implements Runnable {
    private int port;
    private volatile boolean running; 
    private ConfigLoader config;
    private LogManager logger;
    private BlackBoxProvider blackBox; // Ny provider for rådata-lagring
    
    // FTS-variabler
    private int violationCount = 0;
    private final int MAX_VIOLATIONS = 5;
    private boolean ftsTriggered = false;

    // Heartbeat/Link-variabler
    private long lastPacketTime = 0;
    private final long LINK_TIMEOUT = 3000; 

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
                        // Lagrer data i Black Box umiddelbart etter parsing
                        blackBox.record(data);
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

    private void checkLinkStatus() {
        if (lastPacketTime > 0 && (System.currentTimeMillis() - lastPacketTime) > LINK_TIMEOUT) {
            renderLostLinkDashboard();
        }
    }

    private void processData(TelemetryData data, DatagramSocket socket, InetAddress remoteAddr, int remotePort) {
        String status = "NOMINAL";
        String speedAlert = "[ OK ]";
        String tempAlert = "[ OK ]";
        String phaseAlert = "[ OK ]";
        boolean currentViolation = false;

        if (data.speed < config.minSpeed) {
            status = "ALERT";
            speedAlert = String.format("[ VIOLATION: < %.1f ]", config.minSpeed);
            currentViolation = true;
        }
        
        if (data.temperature > config.maxTemp) {
            status = "ALERT";
            tempAlert = String.format("[ VIOLATION: > %.1f ]", config.maxTemp);
            currentViolation = true;
        }

        if (data.phase == FlightPhase.PRE_LAUNCH && data.speed > 1.0) {
            status = "ALERT";
            phaseAlert = "[ ILLEGAL MOVEMENT ]";
            currentViolation = true;
        }

        if (currentViolation && !ftsTriggered) {
            violationCount++;
            logger.logViolation("FTS-AUTO-CHECK", data.speed, config.minSpeed);
            
            if (violationCount >= MAX_VIOLATIONS) {
                sendTermination(socket, remoteAddr, remotePort);
            }
        }

        renderDashboard(data, status, speedAlert, tempAlert, phaseAlert);
    }

    private void sendTermination(DatagramSocket socket, InetAddress addr, int port) {
        try {
            String cmd = "CMD:TERMINATE";
            byte[] buf = cmd.getBytes();
            socket.send(new DatagramPacket(buf, buf.length, addr, port));
            ftsTriggered = true;
        } catch (Exception e) {
            System.err.println("[FTS ERROR] Kunne ikke sende kommando: " + e.getMessage());
        }
    }

    private void renderDashboard(TelemetryData data, String status, String spd, String tmp, String phs) {
        System.out.print("\033[H\033[2J");  
        System.out.flush();

        long latency = System.currentTimeMillis() - lastPacketTime;

        System.out.println("============================================================");
        System.out.println("           AEGIS-X MISSION CONTROL DASHBOARD                ");
        System.out.println("============================================================");
        System.out.println(" LINK: [ STABLE (" + latency + "ms) ]        FTS: [ " + (ftsTriggered ? "ABORTED" : "ACTIVE") + " ]");
        System.out.println(" STATUS: [ " + (ftsTriggered ? "ABORTED" : status) + " ]          PHASE: [ " + data.phase + " ]");
        System.out.println(" RECORDING: [ ACTIVE ] -> logs/flight_data.jsonl");
        System.out.println("------------------------------------------------------------");
        System.out.printf("   SPEED:   %.2f km/t  %s\n", data.speed, spd);
        System.out.printf("   ALT:     %.2f m     [ NOMINAL ]\n", data.altitude);
        System.out.printf("   TEMP:    %.2f °C    %s\n", data.temperature, tmp);
        if (!phs.equals("[ OK ]")) System.out.println("   PHASE ERR: " + phs);
        System.out.println("------------------------------------------------------------");
        System.out.println(" COMMANDS: 'stop', 'status', 'abort', 'reload'");
        System.out.println("============================================================");
        System.out.print("> ");
    }

    private void renderLostLinkDashboard() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        System.out.println("============================================================");
        System.out.println("           AEGIS-X MISSION CONTROL DASHBOARD                ");
        System.out.println("============================================================");
        System.out.println(" STATUS: [ !!! LOST LINK !!! ]      PHASE: [ UNKNOWN ]");
        System.out.println(" WARNING: Ingen telemetri mottatt på > " + (LINK_TIMEOUT/1000) + "s");
        System.out.println(" BLACK BOX: Recording paused.");
        System.out.println("------------------------------------------------------------");
        System.out.println(" HANDLING: Sjekk missil-sender eller nettverkstilkobling.");
        System.out.println("------------------------------------------------------------");
        System.out.println(" COMMANDS: 'stop', 'status', 'abort', 'reload'");
        System.out.println("============================================================");
        System.out.print("> ");
    }
    
    public void triggerManualAbort() {
        this.violationCount = MAX_VIOLATIONS; 
    }

    public void stop() { this.running = false; }
}