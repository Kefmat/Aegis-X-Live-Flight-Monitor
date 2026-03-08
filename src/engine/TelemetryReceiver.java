package engine;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import model.TelemetryData;

/**
 * Håndterer nettverkskommunikasjon via UDP.
 * Lytter etter innkommende telemetripakker og validerer dem mot systemkrav.
 */
public class TelemetryReceiver {
    private int port;
    private boolean running;
    private ConfigLoader config;
    private LogManager logger;

    /**
     * Oppretter en ny mottaker for telemetri.
     * * @param port Nettverksporten det skal lyttes på.
     * @param config Konfigurasjonsobjektet som inneholder grenseverdier.
     */
    public TelemetryReceiver(int port, ConfigLoader config, LogManager logger) {
        this.port = port;
        this.config = config;
        this.logger = logger;
    }

    /**
     * Starter mottak av UDP-pakker. Denne metoden blokkerer tråden den kjører i.
     */
    public void start() {
        this.running = true;
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("[NETWORK] Lytter på UDP port " + port + "...");
            byte[] buffer = new byte[1024];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                TelemetryData data = Parser.parseRawString(received);
                
                if (data != null) {
                    processData(data);
                }
            }
        } catch (Exception e) {
            System.err.println("[NETWORK ERROR] " + e.getMessage());
        }
    }

    /**
     * Intern prosessering av mottatte data og sjekk mot grenseverdier.
     * Inkluderer nå et dynamisk dashbord som oppdateres i terminalen.
     * * @param data Telemetridataene som skal valideres.
     */
    private void processData(TelemetryData data) {
        // Rens terminalen for en dynamisk visning
        System.out.print("\033[H\033[2J");  
        System.out.flush();

        String status = "NOMINAL";
        String speedAlert = "[ OK ]";
        String tempAlert = "[ OK ]";

        // Validering mot grenseverdier
        if (data.speed < config.minSpeed) {
            status = "ALERT";
            speedAlert = String.format("[ VIOLATION: < %.1f ]", config.minSpeed);
            logger.logViolation("REQ-NAV-01", data.speed, config.minSpeed);
        }
        
        if (data.temperature > config.maxTemp) {
            status = "ALERT";
            tempAlert = String.format("[ VIOLATION: > %.1f ]", config.maxTemp);
            logger.logViolation("REQ-THERM-01", data.temperature, config.maxTemp);
        }

        // Tegner Dashbordet
        System.out.println("============================================================");
        System.out.println("           AEGIS-X MISSION CONTROL DASHBOARD                ");
        System.out.println("============================================================");
        System.out.println(" STATUS: [ " + status + " ]          PORT: " + port);
        System.out.println("------------------------------------------------------------");
        System.out.println(" TELEMETRY DATA:");
        System.out.printf("   SPEED:   %.2f km/t  %s\n", data.speed, speedAlert);
        System.out.printf("   ALT:     %.2f m     [ NOMINAL ]\n", data.altitude);
        System.out.printf("   TEMP:    %.2f °C    %s\n", data.temperature, tempAlert);
        System.out.println("------------------------------------------------------------");
        System.out.println(" LOGGING: Active (logs/ncr_report.csv)");
        System.out.println("============================================================");
        System.out.println(" (Press Ctrl+C to abort mission)");
    }
    
    /**
     * Stopper nettverksmottaket.
     */
    public void stop() {
        this.running = false;
    }
}