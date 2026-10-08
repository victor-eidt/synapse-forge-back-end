package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.User.LoginDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.service.AuthService;

import java.util.Map;

@Tag(name = "Autenticação", description = "Cadastro, login (JWT), confirmação de e-mail e recuperação de senha. Rotas públicas.")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    // =========================================================
    // CADASTRO DE CLIENTE
    // =========================================================

    @Operation(summary = "Cadastrar cliente", description = "Cria um usuário CLIENTE e envia o e-mail de confirmação. O login só é liberado após confirmar.")
    @ApiResponse(responseCode = "200", description = "Conta criada; aguardando confirmação do e-mail", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"mensagem\": \"Conta criada! Verifique seu email para confirmar o acesso.\"}")))
    @PostMapping("/cadastro")
    public Map<String, String> cadastro(
            @RequestBody UserRequestDTO dto
    ) {

        return service.cadastro(dto);
    }

    // =========================================================
    // CADASTRO DE GERENTE
    // =========================================================

    @Operation(summary = "Cadastrar gerente", description = "Cria um usuário GERENTE e a equipe (oficina) dele, com o nome informado em nomeEquipe.")
    @ApiResponse(responseCode = "200", description = "Gerente e equipe criados; aguardando confirmação do e-mail", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"mensagem\": \"Conta criada! Verifique seu email para confirmar o acesso.\"}")))
    @PostMapping("/cadastro-gerente")
    public Map<String, String> cadastroGerente(
            @RequestBody UserRequestDTO dto
    ) {

        return service.cadastroGerente(dto);
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @Operation(summary = "Login", description = "Valida e-mail e senha e devolve o token JWT (access_token) para usar no botão Authorize. Após várias tentativas erradas a conta é bloqueada temporariamente.")
    @ApiResponse(responseCode = "200", description = "Login aceito: token JWT e id do usuário", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"access_token\": \"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2NzA0YTFjMmU0YjBmODFhMmMzZDRlMDEiLCJyb2xlIjoiR0VSRU5URSJ9.Xq3r8Kp2mZ7vT1nH5yL0wQe4sD9fJ6bA2cG8uR1tY0o\", \"user_id\": \"6704a1c2e4b0f81a2c3d4e01\"}")))
    @PostMapping("/login")
    public Map<String, String> login(
            @RequestBody LoginDTO dto
    ) {

        return service.login(dto);
    }

    // =========================================================
    // CONFIRMAR EMAIL
    // =========================================================

    @Operation(summary = "Confirmar e-mail", description = "Confirma o cadastro pelo token enviado por e-mail.")
    @ApiResponse(responseCode = "200", description = "E-mail confirmado; já devolve o token para entrar", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"access_token\": \"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2NzA0YTFjMmU0YjBmODFhMmMzZDRlMDEiLCJyb2xlIjoiR0VSRU5URSJ9.Xq3r8Kp2mZ7vT1nH5yL0wQe4sD9fJ6bA2cG8uR1tY0o\", \"user_id\": \"6704a1c2e4b0f81a2c3d4e05\"}")))
    @GetMapping("/confirmar-email/{token}")
    public Map<String, String> confirmarEmail(
            @Parameter(description = "Token recebido por e-mail", example = "3f9c2b7e-8a41-4d2e-9b6f-1c5d7e8a9b0c") @PathVariable String token
    ) {

        return service.confirmarEmail(token);
    }

    // =========================================================
    // ESQUECI A SENHA
    // =========================================================

    @Operation(summary = "Esqueci minha senha", description = "Envia um e-mail com o link de redefinição. Responde igual mesmo se o e-mail não existir, para não revelar contas.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "E-mail da conta", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"email\": \"mariana.costa@email.com\"}")))
    @ApiResponse(responseCode = "200", description = "Mensagem genérica (igual mesmo se o e-mail não existir)", content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "Se o email existir, você receberá instruções")))
    @PostMapping("/esqueci-senha")
    public String esqueciSenha(
            @RequestBody Map<String, String> body
    ) {

        service.esqueciSenha(
                body.get("email")
        );

        return "Se o email existir, você receberá instruções";
    }

    // =========================================================
    // REDEFINIR SENHA
    // =========================================================

    @Operation(summary = "Redefinir senha", description = "Troca a senha usando o token do e-mail (válido por 1 hora).")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "E-mail, token recebido e nova senha", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"email\": \"mariana.costa@email.com\", \"token\": \"3f9c2b7e-8a41-4d2e-9b6f-1c5d7e8a9b0c\", \"novaSenha\": \"NovaSenha@2026\"}")))
    @ApiResponse(responseCode = "200", description = "Senha trocada", content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "Senha redefinida com sucesso")))
    @PostMapping("/redefinir-senha")
    public String redefinirSenha(
            @RequestBody Map<String, String> body
    ) {

        service.redefinirSenha(
                body.get("email"),
                body.get("token"),
                body.get("novaSenha")
        );

        return "Senha redefinida com sucesso";
    }
}