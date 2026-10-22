package synapseforge.crud.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de tentativas por IP nas rotas públicas de autenticação: segura força bruta de senha
 * e o disparo de e-mails (cadastro, esqueci-senha) pela conta SMTP do sistema.
 * <p>
 * Fica em memória: vale por instância e zera quando a aplicação reinicia. Atrás de um proxy o
 * IP real só chega com server.forward-headers-strategy configurado; sem isso todos os
 * usuários dividem o IP do proxy.
 * <p>
 * Não é @Component de propósito: entra só na cadeia do Spring Security, depois do filtro de
 * CORS, para o 429 chegar ao navegador com os cabeçalhos de CORS.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    record Limite(int maximo, Duration janela) {
    }

    private record Janela(Instant inicio, int contagem) {
    }

    // Login aceita mais tentativas (erro de digitação); as rotas que mandam e-mail, menos
    static final Map<String, Limite> LIMITES = Map.of(
            "/auth/login", new Limite(10, Duration.ofMinutes(1)),
            "/auth/cadastro", new Limite(5, Duration.ofMinutes(15)),
            "/auth/cadastro-gerente", new Limite(5, Duration.ofMinutes(15)),
            "/auth/esqueci-senha", new Limite(5, Duration.ofMinutes(15)),
            "/auth/redefinir-senha", new Limite(10, Duration.ofMinutes(15))
    );

    private static final Duration MAIOR_JANELA = Duration.ofMinutes(15);
    private static final int LIMPAR_ACIMA_DE = 1_000;

    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimitFilter() {
        this(Clock.systemUTC());
    }

    RateLimitFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !LIMITES.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String rota = request.getRequestURI();
        long esperaSegundos = registrar(request.getRemoteAddr() + " " + rota, LIMITES.get(rota), clock.instant());

        if (esperaSegundos > 0) {
            log.warn("Limite de tentativas atingido em {}", rota);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(esperaSegundos));
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Muitas tentativas. Tente de novo em alguns minutos.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Conta a tentativa. Devolve 0 se pode seguir, ou quantos segundos faltam para liberar. */
    long registrar(String chave, Limite limite, Instant agora) {
        if (janelas.size() > LIMPAR_ACIMA_DE) {
            janelas.values().removeIf(j -> !j.inicio().plus(MAIOR_JANELA).isAfter(agora));
        }

        Janela janela = janelas.compute(chave, (k, atual) ->
                atual == null || !atual.inicio().plus(limite.janela()).isAfter(agora)
                        ? new Janela(agora, 1)
                        : new Janela(atual.inicio(), atual.contagem() + 1));

        if (janela.contagem() <= limite.maximo()) {
            return 0;
        }
        return Math.max(1, Duration.between(agora, janela.inicio().plus(limite.janela())).toSeconds());
    }
}
