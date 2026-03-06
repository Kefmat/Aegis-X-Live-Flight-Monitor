package engine;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import model.TelemetryData;

public class TelemetryReceiver {
    private int port;
    private boolean running;

    public TelemetryReceiver(int port) {
        this.port = port;
    }

    public void start() {
        this.running = true;
        
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("[NETWORK] Lytter på UDP port " + port + "...");
            
            byte[] buffer = new byte[1024];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Blokkerer til data mottas

                String received = new String(packet.getData(), 0, packet.getLength());
                
                // Bruker Parseren vi lagde tidligere
                TelemetryData data = Parser.parseRawString(received);
                
                if (data != null) {
                    processData(data);
                }
            }
        } catch (Exception e) {
            System.err.println("[NETWORK ERROR] " + e.getMessage());
        }
    }

    private void processData(TelemetryData data) {
        System.out.println("[LIVE] " + data.toString());
        
        // Sanntids sjekk mot grenseverdier
        if (data.speed < 1800) {
            System.out.println(">>> ALERT: Under minimumshastighet (1800 km/t)!");
        }
        if (data.temperature > 85) {
            System.out.println(">>> ALERT: Kritisk temperatur overskredet!");
        }
    }

    public void stop() {
        this.running = false;
    }
}