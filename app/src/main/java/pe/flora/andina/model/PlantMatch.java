package pe.flora.andina.model;

/** Una especie candidata; score es el puntaje del motor, entre 0 y 1. */
public record PlantMatch(String scientific, String common, float score) {}
