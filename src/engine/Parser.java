package engine;

import model.TelemetryData;
import model.FlightPhase;

/**
 * Ansvarlig for å dekode rå-tekst fra UDP-pakker til TelemetryData-objekter.
 * Forventet format: "PHS:BOOST;SPD:1900;ALT:12000;TEMP:45"
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
            
            return new TelemetryData(spd, alt, temp, phase);
        } catch (Exception e) {
            System.err.println("[PARSER ERROR] Ugyldig format: " + raw);
            return null;
        }
    }
}