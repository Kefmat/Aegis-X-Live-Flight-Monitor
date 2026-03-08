package model;

/**
 * Representerer et datasett med telemetri fra missilet.
 * Inkluderer nå FlightPhase for å støtte fase-spesifikk validering.
 */
public class TelemetryData {
    public double speed;
    public double altitude;
    public double temperature;
    public FlightPhase phase;

    public TelemetryData(double speed, double altitude, double temperature, FlightPhase phase) {
        this.speed = speed;
        this.altitude = altitude;
        this.temperature = temperature;
        this.phase = phase;
    }

    @Override
    public String toString() {
        return String.format("[%s] Fart: %.1f km/t, Høyde: %.1f m, Temp: %.1f°C", 
                              phase, speed, altitude, temperature);
    }
}