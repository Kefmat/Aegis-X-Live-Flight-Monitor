package engine;

import model.TelemetryData;
import model.FlightPhase;

/**
 * Ansvarlig for å dekode rå-tekst fra UDP-pakker til TelemetryData-objekter.
 * Forventet format: "PHS:BOOST;SPD:1900;ALT:12000;TEMP:45;X:150.0;Y:200.0"
 */
public class Parser {
    
    public static TelemetryData parseRawString(String raw) {
        try {
            String[] parts = raw.split(";");
            
            // Henter ut fasen (f.eks. PHS:BOOST -> BOOST)
            String phaseStr = parts[0].split(":")[1];
            FlightPhase phase;
            try {
                phase = FlightPhase.valueOf(phaseStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Hvis fasen er ukjent, setter vi en trygg standard
                phase = FlightPhase.PRE_LAUNCH;
            }

            // Dekoder de numeriske verdiene
            double spd = Double.parseDouble(parts[1].split(":")[1]);
            double alt = Double.parseDouble(parts[2].split(":")[1]);
            double temp = Double.parseDouble(parts[3].split(":")[1]);
            
            // Dekoder de nye koordinatene for Geofencing
            double x = 0.0;
            double y = 0.0;
            if (parts.length >= 6) {
                x = Double.parseDouble(parts[4].split(":")[1]);
                y = Double.parseDouble(parts[5].split(":")[1]);
            }
            
            // Returnerer komplett objekt med posisjonsdata
            return new TelemetryData(phase, spd, alt, temp, x, y);
        } catch (Exception e) {
            System.err.println("[PARSER ERROR] Ugyldig format: " + raw);
            return null;
        }
    }
}