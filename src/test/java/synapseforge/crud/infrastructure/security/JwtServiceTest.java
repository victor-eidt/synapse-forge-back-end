package synapseforge.crud.infrastructure.security;

import org.junit.jupiter.api.Test;
import synapseforge.crud.infrastructure.entity.Role;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService service = new JwtService();

    @Test
    void deveGerarTokenComIdEPerfilExtraiveis() {
        String token = service.generateToken("user-1", Role.ADMIN);

        assertEquals("user-1", service.extractUserId(token));
        assertEquals("ADMIN", service.extractRole(token));
    }

    @Test
    void deveRejeitarTokenInvalido() {
        assertThrows(RuntimeException.class, () -> service.extractUserId("token-invalido"));
        assertThrows(RuntimeException.class, () -> service.extractRole("token-invalido"));
    }
}
