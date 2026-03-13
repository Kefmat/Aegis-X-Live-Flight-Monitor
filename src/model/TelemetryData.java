package model;

/**
 * Representerer et datasett med telemetri fra missilet.
 * Inkluderer nå FlightPhase for å støtte fase-spesifikk validering,
 * samt X- og Y-koordinater for Geofencing.
 */
public class TelemetryData {
    public double speed;
    public double altitude;
    public double temperature;
    public FlightPhase phase;
    public double x;
    public double y;

    public TelemetryData(FlightPhase phase, double speed, double altitude, double temperature, double x, double y) {
        this.phase = phase;
        this.speed = speed;
        this.altitude = altitude;
        this.temperature = temperature;
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("[%s] Fart: %.1f km/t, Høyde: %.1f m, Temp: %.1f°C, Pos: (X: %.1f, Y: %.1f)", 
                              phase, speed, altitude, temperature, x, y);
    }
}