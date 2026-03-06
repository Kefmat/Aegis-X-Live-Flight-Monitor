package engine;

import model.TelemetryData;

public class Parser {
    // Forventer format: "SPD:1900;ALT:12000;TEMP:45"
    public static TelemetryData parseRawString(String raw) {
        try {
            String[] parts = raw.split(";");
            double spd = Double.parseDouble(parts[0].split(":")[1]);
            double alt = Double.parseDouble(parts[1].split(":")[1]);
            double temp = Double.parseDouble(parts[2].split(":")[1]);
            
            return new TelemetryData(spd, alt, temp);
        } catch (Exception e) {
            System.err.println("[PARSER ERROR] Ugyldig format: " + raw);
            return null;
        }
    }
}