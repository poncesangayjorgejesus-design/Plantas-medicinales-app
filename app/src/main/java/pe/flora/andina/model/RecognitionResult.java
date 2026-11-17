package pe.flora.andina.model;

import java.util.List;

/** Candidatos devueltos por un análisis y nombre del motor. */
public record RecognitionResult(List<PlantMatch> matches, String engine) {}
