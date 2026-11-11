package cl.duoc.duocconecta.prompts.domain;

/**
 * Para qué tipo de tarea sirve un prompt. Es una lista corta y transversal a las carreras: la
 * carrera va aparte, así un prompt de redacción sirve igual a Diseño que a Informática.
 */
public enum CategoriaPrompt {
    CODIGO,
    REDACCION,
    ANALISIS_DATOS,
    DISENO,
    ESTUDIO,
    INVESTIGACION,
    OTRO
}
