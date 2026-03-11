package engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Verktøy for Post-Mission Analysis (PMA).
 * Leser Black Box-data og beregner statistikk for hele flyvningen.
 */
public class FlightAnalyzer {

    public static void analyze(String logPath) {
        double maxSpeed = 0;
        double maxAltitude = 0;
        double totalTemp = 0;
        int count = 0;
        String startPhase = "";
        String endPhase = "";

        System.out.println("\n[PMA] Starter analyse av Black Box: " + logPath);
        System.out.println("------------------------------------------------------------");

        try (BufferedReader reader = new BufferedReader(new FileReader(logPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue; // Hopper over kommentarer

                try {
                    // Enkel parsing av JSON-lignende streng (siden vi ikke bruker eksterne biblioteker)
                    double spd = Double.parseDouble(extractValue(line, "spd"));
                    double alt = Double.parseDouble(extractValue(line, "alt"));
                    double temp = Double.parseDouble(extractValue(line, "temp"));
                    String phase = extractString(line, "phase");

                    if (count == 0) startPhase = phase;
                    endPhase = phase;

                    if (spd > maxSpeed) maxSpeed = spd;
                    if (alt > maxAltitude) maxAltitude = alt;
                    totalTemp += temp;
                    count++;
                } catch (Exception e) {
                    // Hopper over korrupte linjer
                }
            }

            if (count > 0) {
                renderReport(count, maxSpeed, maxAltitude, totalTemp / count, startPhase, endPhase);
            } else {
                System.out.println("[!] Ingen gyldige data funnet i Black Box.");
            }

        } catch (IOException e) {
            System.err.println("[PMA ERROR] Kunne ikke lese logg: " + e.getMessage());
        }
    }

    private static void renderReport(int samples, double maxSpd, double maxAlt, double avgTemp, String start, String end) {
        System.out.printf("   DATAPUNKTER ANALYSERT:  %d\n", samples);
        System.out.printf("   MAKSIMAL HASTIGHET:     %.2f km/t\n", maxSpd);
        System.out.printf("   MAKSIMAL HØYDE:         %.2f m\n", maxAlt);
        System.out.printf("   GJENNOMSNITTSTEMP:      %.2f °C\n", avgTemp);
        System.out.println("   MISJONSPROFIL:          Fra " + start + " til " + end);
        System.out.println("------------------------------------------------------------");
        System.out.println("[PMA] Analyse fullført.");
    }

    // Hjelpemetoder for enkel streng-parsing
    private static String extractValue(String line, String key) {
        int start = line.indexOf("\"" + key + "\":") + key.length() + 3;
        int end = line.indexOf(",", start);
        if (end == -1) end = line.indexOf("}", start);
        return line.substring(start, end).trim();
    }

    private static String extractString(String line, String key) {
        int start = line.indexOf("\"" + key + "\":\"") + key.length() + 4;
        int end = line.indexOf("\"", start);
        return line.substring(start, end);
    }
}