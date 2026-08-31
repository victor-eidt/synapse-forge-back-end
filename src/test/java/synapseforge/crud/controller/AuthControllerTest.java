package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.User.LoginDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.service.AuthService;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService service;

    @InjectMocks
    private AuthController controller;

    @Test
    void cadastroDeveDelegarParaService() {
        UserRequestDTO dto = new UserRequestDTO();
        Map<String, String> expected = Map.of("mensagem", "ok");
        when(service.cadastro(dto)).thenReturn(expected);

        assertEquals(expected, controller.cadastro(dto));
        verify(service).cadastro(dto);
    }

    @Test
    void loginDeveDelegarParaService() {
        LoginDTO dto = new LoginDTO();
        Map<String, String> expected = Map.of("access_token", "jwt");
        when(service.login(dto)).thenReturn(expected);

        assertEquals(expected, controller.login(dto));
        verify(service).login(dto);
    }

    @Test
    void confirmarEmailDeveDelegarParaService() {
        Map<String, String> expected = Map.of("user_id", "u-1");
        when(service.confirmarEmail("abc")).thenReturn(expected);

        assertEquals(expected, controller.confirmarEmail("abc"));
        verify(service).confirmarEmail("abc");
    }

    @Test
    void esqueciSenhaDeveDelegarParaService() {
        Map<String, String> body = Map.of("email", "ana@email.com");

        String response = controller.esqueciSenha(body);

        assertEquals("Se o email existir, você receberá instruções", response);
        verify(service).esqueciSenha("ana@email.com");
    }

    @Test
    void redefinirSenhaDeveDelegarParaService() {
        Map<String, String> body = Map.of(
                "email", "ana@email.com",
                "token", "token-123",
                "novaSenha", "novaSenha"
        );

        String response = controller.redefinirSenha(body);

        assertEquals("Senha redefinida com sucesso", response);
        verify(service).redefinirSenha("ana@email.com", "token-123", "novaSenha");
    }
}
