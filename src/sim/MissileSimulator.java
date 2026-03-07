package sim;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Simulerer et missil i flukt ved å sende UDP-pakker med telemetri.
 * Brukes for å teste Aegis-X Ground Station uten reell maskinvare.
 */
public class MissileSimulator {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 5000;
        
        double speed = 1750.0;
        double altitude = 12000.0;
        double temp = 45.0;

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(host);
            System.out.println("[SIMULATOR] Starter sending til " + host + ":" + port);

            while (true) {
                // Simulerer endring i data
                speed += 15.5;
                altitude += 50.0;
                temp += 2.5;  

                // Formaterer strengen nøyaktig slik Parser.java forventer
                String message = String.format("SPD:%.2f;ALT:%.2f;TEMP:%.2f", speed, altitude, temp);
                byte[] buffer = message.getBytes();

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
                socket.send(packet);

                System.out.println("[SIM] Sendt: " + message);
                
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.err.println("[SIM ERROR] " + e.getMessage());
        }
    }
}