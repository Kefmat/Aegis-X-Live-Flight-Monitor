package sim;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import model.FlightPhase;

/**
 * Simulerer et missil i flukt ved å sende UDP-pakker med telemetri.
 * Går gjennom fasene PRE_LAUNCH, BOOST og SUSTAIN for å teste bakkestasjonens logikk.
 */
public class MissileSimulator {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 5000;
        
        // Startverdier
        double speed = 0.0;
        double altitude = 0.0;
        double temp = 15.0;
        FlightPhase currentPhase;

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(host);
            System.out.println("[SIMULATOR] Starter sekvens mot " + host + ":" + port);

            for (int i = 0; i < 100; i++) {
                // Bestemmer fase og simulerer data basert på fremdrift
                if (i < 10) {
                    currentPhase = FlightPhase.PRE_LAUNCH;
                    // Står stille på rampa
                } else if (i < 40) {
                    currentPhase = FlightPhase.BOOST;
                    speed += 115.5;    // Akselerasjon
                    altitude += 450.0; // Stigning
                    temp += 2.2;       // Friksjonsvarme
                } else {
                    currentPhase = FlightPhase.SUSTAIN;
                    speed = 1850.0 + (Math.random() * 15); // Holder seg over kravet (1800)
                    altitude += 25.0;  // Flater ut stigningen
                    temp = 48.0 + (Math.random() * 1.5);   // Stabil temperatur
                }

                // Format: PHS:VALUE;SPD:VALUE;ALT:VALUE;TEMP:VALUE
                String message = String.format("PHS:%s;SPD:%.2f;ALT:%.2f;TEMP:%.2f", 
                                                currentPhase, speed, altitude, temp);
                
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
                socket.send(packet);

                System.out.println("[SIM] Sendt: " + message);
                
                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.err.println("[SIM ERROR] " + e.getMessage());
        }
    }
}