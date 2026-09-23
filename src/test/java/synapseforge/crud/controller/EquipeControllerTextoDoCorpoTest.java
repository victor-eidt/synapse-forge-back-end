package synapseforge.crud.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// SYN-102: a função visual chegava como string JSON e era gravada com as aspas.
class EquipeControllerTextoDoCorpoTest {

    @Test
    void tiraAsAspasDeUmaStringJson() {
        assertEquals("Pintor", EquipeController.textoDoCorpo("\"Pintor\""));
    }

    @Test
    void aceitaTextoPuro() {
        assertEquals("Pintor", EquipeController.textoDoCorpo("  Pintor "));
    }

    @Test
    void decodificaEscapesDoJson() {
        assertEquals("Pintor \"sênior\"", EquipeController.textoDoCorpo("\"Pintor \\\"sênior\\\"\""));
    }

    @Test
    void vazioViraNull() {
        assertNull(EquipeController.textoDoCorpo("\"\""));
        assertNull(EquipeController.textoDoCorpo("   "));
        assertNull(EquipeController.textoDoCorpo(null));
    }
}
