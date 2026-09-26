package synapseforge.crud.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import synapseforge.crud.infrastructure.security.JwtFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // =====================================================
                        // ROTAS PÚBLICAS
                        // =====================================================

                        // Swagger / OpenAPI
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()

                        // Autenticação
                        .requestMatchers("/auth/**")
                        .permitAll()

                        // Confirmação de mudança de email
                        .requestMatchers(
                                "/users/confirmar-mudanca-email/**"
                        )
                        .permitAll()

                        // Convites de equipe
                        // O usuário precisa conseguir visualizar,
                        // aceitar ou recusar um convite recebido por email
                        // sem estar autenticado.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/equipes/convites/**"
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/equipes/convites/**"
                        )
                        .permitAll()


                        // =====================================================
                        // PEDIDOS
                        // =====================================================

                        // Todos os usuários autenticados podem visualizar
                        // pedidos.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/pedidos/**"
                        )
                        .hasAnyRole(
                                "CLIENTE",
                                "TECNICO",
                                "GERENTE",
                                "ADMIN"
                        )

                        // Somente Técnico, Gerente e Admin podem criar
                        // pedidos.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/pedidos/**"
                        )
                        .hasAnyRole(
                                "TECNICO",
                                "GERENTE",
                                "ADMIN"
                        )

                        // Somente Técnico, Gerente e Admin podem alterar
                        // pedidos.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/pedidos/**"
                        )
                        .hasAnyRole(
                                "TECNICO",
                                "GERENTE",
                                "ADMIN"
                        )

                        // PATCH de pedidos
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/pedidos/**"
                        )
                        .hasAnyRole(
                                "TECNICO",
                                "GERENTE",
                                "ADMIN"
                        )

                        // Somente Técnico, Gerente e Admin podem excluir
                        // pedidos.
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/pedidos/**"
                        )
                        .hasAnyRole(
                                "TECNICO",
                                "GERENTE",
                                "ADMIN"
                        )


                        // =====================================================
                        // DEMAIS ROTAS
                        // =====================================================

                        // Qualquer outra rota exige autenticação.
                        .anyRequest().authenticated()
                )

                // Sem token válido (ausente, expirado, adulterado) = 401; autenticado sem
                // permissão = 403. Antes os dois viravam 403 e o front não sabia se devia deslogar.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        // Status direto, sem sendError: o sendError encaminha para /error, que
                        // exige autenticação, e o 403 virava 401 (e o front deslogaria).
                        .accessDeniedHandler((request, response, e) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value()))
                )

                // JWT precisa ser executado antes do filtro padrão
                // de autenticação por username/password.
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}