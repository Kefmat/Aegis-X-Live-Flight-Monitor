package engine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import model.TelemetryData;

/**
 * Black Box-opptaker for Aegis-X.
 * Lagrer alle telemetripunkter i et JSON-Lines format for Post-Mission Analysis.
 */
public class BlackBoxProvider {
    private String logPath;

    public BlackBoxProvider(String logPath) {
        this.logPath = logPath;
        initializeFile();
    }

    private void initializeFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logPath, false))) {
            writer.write("# AEGIS-X BLACK BOX RECORDING STARTED: " + Instant.now() + "\n");
        } catch (IOException e) {
            System.err.println("[BLACKBOX ERROR] Kunne ikke initiere loggfil: " + e.getMessage());
        }
    }

    /**
     * Skriver et telemetripunkt til loggen med tidsstempel.
     * Bruker JSON-format for å gjøre det enkelt å parse for eksterne verktøy.
     */
    public void record(TelemetryData data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logPath, true))) {
            String entry = String.format(
                "{\"ts\":\"%s\", \"phase\":\"%s\", \"spd\":%.2f, \"alt\":%.2f, \"temp\":%.2f}",
                Instant.now().toString(),
                data.phase,
                data.speed,
                data.altitude,
                data.temperature
            );
            writer.write(entry + "\n");
        } catch (IOException e) {
            // Vi logger feilen til stderr for å ikke forstyrre dashbordet
            System.err.println("[BLACKBOX ERROR] Skrivefeil: " + e.getMessage());
        }
    }
}