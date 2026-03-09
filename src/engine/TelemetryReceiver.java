package engine;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import model.TelemetryData;
import model.FlightPhase;

/**
 * Håndterer nettverkskommunikasjon via UDP i en egen tråd.
 * Lytter etter innkommende telemetripakker og validerer dem mot systemkrav basert på flyfase.
 * Inkluderer nå Flight Termination System (FTS) for toveis-kommunikasjon.
 */
public class TelemetryReceiver implements Runnable {
    private int port;
    private volatile boolean running; 
    private ConfigLoader config;
    private LogManager logger;
    
    // FTS-variabler
    private int violationCount = 0;
    private final int MAX_VIOLATIONS = 5;
    private boolean ftsTriggered = false;

    /**
     * Oppretter en ny mottaker for telemetri.
     * @param port Nettverksporten det skal lyttes på.
     * @param config Konfigurasjonsobjektet som inneholder grenseverdier.
     * @param logger Logghåndterer for avviksrapportering.
     */
    public TelemetryReceiver(int port, ConfigLoader config, LogManager logger) {
        this.port = port;
        this.config = config;
        this.logger = logger;
    }

    @Override
    public void run() {
        this.running = true;
        try (DatagramSocket socket = new DatagramSocket(port)) {
            // Setter timeout slik at socket.receive() ikke blokkerer evig.
            socket.setSoTimeout(1000); 
            
            System.out.println("[NETWORK] Mottaker-tråd startet på port " + port);
            byte[] buffer = new byte[1024];

            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String received = new String(packet.getData(), 0, packet.getLength());
                    TelemetryData data = Parser.parseRawString(received);
                    
                    if (data != null) {
                        // Sender med adresse og port for å kunne svare (FTS)
                        processData(data, socket, packet.getAddress(), packet.getPort());
                    }
                } catch (SocketTimeoutException e) {
                    // Timeout sjekk for trådhåndtering
                }
            }
        } catch (Exception e) {
            System.err.println("[NETWORK ERROR] " + e.getMessage());
        }
        System.out.println("[NETWORK] Mottaker-tråd avsluttet.");
    }

    /**
     * Intern prosessering av mottatte data og sjekk mot grenseverdier.
     * Inkluderer nå FTS-logikk og toveis-sending av terminering-kommando.
     */
    private void processData(TelemetryData data, DatagramSocket socket, InetAddress remoteAddr, int remotePort) {
        System.out.print("\033[H\033[2J");  
        System.out.flush();

        String status = "NOMINAL";
        String speedAlert = "[ OK ]";
        String tempAlert = "[ OK ]";
        String phaseAlert = "[ OK ]";
        boolean currentViolation = false;

        // Validering mot ConfigLoader
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

        // FTS Logikk: Hvis avvik oppdages, øk telleren
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
        System.out.println("============================================================");
        System.out.println("           AEGIS-X MISSION CONTROL DASHBOARD                ");
        System.out.println("============================================================");
        System.out.println(" STATUS: [ " + (ftsTriggered ? "ABORTED" : status) + " ]          PHASE: [ " + data.phase + " ]");
        System.out.println(" FTS COUNTER: " + violationCount + " / " + MAX_VIOLATIONS);
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
    
    public void triggerManualAbort() {
        this.violationCount = MAX_VIOLATIONS; // Vil trigge sendTermination ved neste pakke
    }

    public void stop() { this.running = false; }
}