package synapseforge.crud.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtFilterTest {

    private final JwtService jwtService = new JwtService(JwtServiceTest.SEGREDO_TESTE);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtFilter filter = new JwtFilter(jwtService, userRepository);

    // Outros testes deixam Authentication no SecurityContextHolder da mesma thread
    @BeforeEach
    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private Authentication filtrar(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private User usuario(Role role, boolean ativo) {
        User user = new User();
        user.setId("u-1");
        user.setRole(role);
        user.setAtivo(ativo);
        return user;
    }

    @Test
    void papelVemDoBancoENaoDoToken() throws Exception {
        // Token emitido quando era ADMIN; hoje o cadastro diz CLIENTE
        String token = jwtService.generateToken("u-1", Role.ADMIN);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(usuario(Role.CLIENTE, true)));

        Authentication auth = filtrar(token);

        assertEquals("u-1", auth.getPrincipal());
        assertEquals("ROLE_CLIENTE",
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElseThrow());
    }

    @Test
    void usuarioApagadoNaoAutentica() throws Exception {
        String token = jwtService.generateToken("u-1", Role.GERENTE);
        when(userRepository.findById("u-1")).thenReturn(Optional.empty());

        assertNull(filtrar(token));
    }

    @Test
    void usuarioDesativadoNaoAutentica() throws Exception {
        String token = jwtService.generateToken("u-1", Role.TECNICO);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(usuario(Role.TECNICO, false)));

        assertNull(filtrar(token));
    }

    @Test
    void tokenInvalidoNaoConsultaOBanco() throws Exception {
        assertNull(filtrar("token-invalido"));
        verifyNoInteractions(userRepository);
    }
}
