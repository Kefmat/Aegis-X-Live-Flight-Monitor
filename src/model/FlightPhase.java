package model;

/**
 * Definerer de ulike fasene i et missils flyvning.
 */
public enum FlightPhase {
    PRE_LAUNCH,  // Står på bakken, sjekker systemer
    BOOST,       // Kraftig akselerasjon rett etter start
    SUSTAIN,     // Stabil flukt mot målet
    TERMINAL     // Siste fase før treff
}