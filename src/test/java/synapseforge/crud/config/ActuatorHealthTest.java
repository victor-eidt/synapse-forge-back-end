package synapseforge.crud.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;
import synapseforge.crud.infrastructure.security.JwtService;

/**
 * Health check usado pelo Render (SYN-89): público, só com o status geral, e nenhum outro
 * endpoint do Actuator exposto, nem para usuário logado.
 * <p>
 * Contexto completo (com o Mongo embutido) de propósito: o que importa aqui é a configuração
 * real do application.properties, não um slice montado à mão.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "de.flapdoodle.mongodb.embedded.version=7.0.0",
        "app.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tMzItYnl0ZXMhISE="
})
class ActuatorHealthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void health_semToken_devolveSoOStatusGeral() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void outrosEndpointsDoActuator_semToken_exigemLogin() throws Exception {
        mockMvc.perform(get("/actuator")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/health/mongo")).andExpect(status().isUnauthorized());
    }

    @Test
    void outrosEndpointsDoActuator_mesmoLogado_naoSaoServidos() throws Exception {
        // gerente@teste.com vem do DataInitializer (seed ligado por padrão)
        User gerente = userRepository.findByEmail("gerente@teste.com").orElseThrow();
        String token = "Bearer " + jwtService.generateToken(gerente.getId(), gerente.getRole());

        // Sem endpoint, a rota cai no NoResourceFoundException. Hoje o GlobalExceptionHandler
        // transforma isso em 500 (não em 404); o que este teste garante é que nada é servido.
        for (String rota : new String[]{"/actuator/env", "/actuator/beans", "/actuator/configprops",
                "/actuator/heapdump", "/actuator/mappings"}) {
            mockMvc.perform(get(rota).header("Authorization", token))
                    .andExpect(result -> assertThat(result.getResolvedException())
                            .as("rota %s não deveria existir", rota)
                            .isInstanceOf(NoResourceFoundException.class))
                    .andExpect(content().string(not(containsString("propertySources"))));
        }
    }
}
