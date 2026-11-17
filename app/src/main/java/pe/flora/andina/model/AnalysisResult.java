package pe.flora.andina.model;

/** Resultado preparado para la pantalla, con id de foto o error de almacenamiento. */
public record AnalysisResult(RecognitionResult result, String id, String error) {}
