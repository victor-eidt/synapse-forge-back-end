package synapseforge.crud.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import synapseforge.crud.infrastructure.entity.Role;

import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // Só para testes: 32 bytes em Base64
    static final String SEGREDO_TESTE = Base64.getEncoder()
            .encodeToString("segredo-de-teste-com-32-bytes!!!".getBytes());

    private final JwtService service = new JwtService(SEGREDO_TESTE);

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

    @Test
    void naoSobeSemSegredoConfigurado() {
        assertThrows(IllegalStateException.class, () -> new JwtService(""));
        assertThrows(IllegalStateException.class, () -> new JwtService("   "));
        assertThrows(IllegalStateException.class, () -> new JwtService(null));
    }

    @Test
    void naoSobeComSegredoCurtoOuForaDeBase64() {
        String curto = Base64.getEncoder().encodeToString("so-16-bytes-aqui".getBytes());

        assertThrows(IllegalStateException.class, () -> new JwtService(curto));
        assertThrows(IllegalStateException.class, () -> new JwtService("não é base64!"));
    }

    @Test
    void deveRejeitarTokenAssinadoComOutroSegredo() {
        String outroSegredo = Base64.getEncoder()
                .encodeToString("outro-segredo-qualquer-32-bytes!".getBytes());
        String token = new JwtService(outroSegredo).generateToken("user-1", Role.ADMIN);

        assertThrows(RuntimeException.class, () -> service.extractUserId(token));
    }

    @Test
    void deveRejeitarTokenForjadoComOSegredoAntigoDoRepositorio() {
        // O segredo que ficou público no histórico do git não pode mais assinar nada válido
        String forjado = Jwts.builder()
                .setSubject("user-1")
                .claim("role", "ADMIN")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("minha-chave-super-secreta-para-o-tcc".getBytes()),
                        SignatureAlgorithm.HS256)
                .compact();

        assertThrows(RuntimeException.class, () -> service.extractUserId(forjado));
    }
}
