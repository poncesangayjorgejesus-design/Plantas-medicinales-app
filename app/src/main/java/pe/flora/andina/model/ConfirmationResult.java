package pe.flora.andina.model;

/** Resultado de actualizar la especie elegida en una consulta existente. */
public record ConfirmationResult(Status status, String species) {
    public enum Status {
        SAVED,
        NOT_FOUND,
        FAILED
    }
}
