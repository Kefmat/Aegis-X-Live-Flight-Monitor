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
     * * @param data Telemetridataene som skal valideres.
     */
    private void processData(TelemetryData data) {
        System.out.println("[LIVE] " + data.toString());
    
        if (data.speed < config.minSpeed) {
            System.out.println(">>> ALERT: Hastighet under krav!");
            logger.logViolation("REQ-NAV-01", data.speed, config.minSpeed);
        }
        if (data.temperature > config.maxTemp) {
            System.out.println(">>> ALERT: Temperatur over krav!");
            logger.logViolation("REQ-THERM-01", data.temperature, config.maxTemp);
        }
    }
    
    /**
     * Stopper nettverksmottaket.
     */
    public void stop() {
        this.running = false;
    }
}