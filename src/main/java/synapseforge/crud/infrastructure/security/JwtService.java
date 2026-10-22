package synapseforge.crud.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import synapseforge.crud.infrastructure.entity.Role;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    // HS256 exige chave de pelo menos 256 bits
    static final int TAMANHO_MINIMO_BYTES = 32;

    private final Key key;

    /**
     * O segredo vem de app.jwt.secret (variável APP_JWT_SECRET ou application-secrets.properties),
     * em Base64. Sem ele a aplicação não sobe: um segredo fixo no código público permitiria a
     * qualquer um forjar tokens, inclusive de ADMIN.
     */
    public JwtService(@Value("${app.jwt.secret:}") String segredoBase64) {
        this.key = Keys.hmacShaKeyFor(decodificar(segredoBase64));
    }

    private static byte[] decodificar(String segredoBase64) {
        if (segredoBase64 == null || segredoBase64.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret não configurado. Gere um valor com `openssl rand -base64 32` e "
                            + "defina APP_JWT_SECRET ou app.jwt.secret em application-secrets.properties.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(segredoBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "app.jwt.secret precisa estar em Base64 (gere com `openssl rand -base64 32`).");
        }
        if (bytes.length < TAMANHO_MINIMO_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret precisa ter pelo menos " + TAMANHO_MINIMO_BYTES
                            + " bytes (gere com `openssl rand -base64 32`).");
        }
        return bytes;
    }

    public String extractRole(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public String generateToken(String userId, Role role) {

        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUserId(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
