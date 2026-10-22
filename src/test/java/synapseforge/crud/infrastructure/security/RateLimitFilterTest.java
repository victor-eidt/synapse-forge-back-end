package synapseforge.crud.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private static final Instant AGORA = Instant.parse("2026-10-22T12:00:00Z");

    private final RateLimitFilter filter = new RateLimitFilter(Clock.fixed(AGORA, ZoneOffset.UTC));

    private MockHttpServletResponse post(String rota, String ip, FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", rota);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void loginBloqueiaDepoisDoLimiteComRetryAfter() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            assertEquals(200, post("/auth/login", "10.0.0.1", chain).getStatus());
        }
        MockHttpServletResponse bloqueada = post("/auth/login", "10.0.0.1", chain);

        assertEquals(429, bloqueada.getStatus());
        assertEquals("60", bloqueada.getHeader("Retry-After"));
        verify(chain, times(10)).doFilter(any(), any());
    }

    @Test
    void contaSeparadoPorIpEPorRota() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < 5; i++) {
            post("/auth/esqueci-senha", "10.0.0.1", chain);
        }

        assertEquals(429, post("/auth/esqueci-senha", "10.0.0.1", chain).getStatus());
        assertEquals(200, post("/auth/esqueci-senha", "10.0.0.2", chain).getStatus());
        assertEquals(200, post("/auth/login", "10.0.0.1", chain).getStatus());
    }

    @Test
    void rotasForaDaListaEOutrosMetodosNaoContam() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < 50; i++) {
            assertEquals(200, post("/pedidos", "10.0.0.1", chain).getStatus());
        }

        MockHttpServletRequest preflight = new MockHttpServletRequest("OPTIONS", "/auth/login");
        for (int i = 0; i < 50; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(preflight, response, chain);
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void janelaLiberaDepoisDoPrazo() {
        RateLimitFilter.Limite limite = new RateLimitFilter.Limite(2, Duration.ofMinutes(1));

        assertEquals(0, filter.registrar("ip rota", limite, AGORA));
        assertEquals(0, filter.registrar("ip rota", limite, AGORA.plusSeconds(10)));
        assertEquals(50, filter.registrar("ip rota", limite, AGORA.plusSeconds(10)));
        assertEquals(0, filter.registrar("ip rota", limite, AGORA.plusSeconds(60)));
    }
}
