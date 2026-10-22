package synapseforge.crud.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;
import synapseforge.crud.infrastructure.security.JwtService;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            try {
                String userId = jwtService.extractUserId(token);

                // Papel e situação vêm do banco, não do token: usuário apagado, desativado ou
                // rebaixado perde o acesso na hora, sem esperar o token vencer.
                userRepository.findById(userId)
                        .filter(User::isAtivo)
                        .ifPresent(user -> {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            user.getId(),
                                            null,
                                            Collections.singletonList(
                                                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                                            )
                                    );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        });

            } catch (Exception e) {
                // Apenas a classe da excecao: a mensagem do JJWT pode conter o proprio token.
                log.debug("Token inválido ({})", e.getClass().getSimpleName());
            }
        }

        filterChain.doFilter(request, response);
    }
}
