package synapseforge.crud.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.User.LoginDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    // =========================================================
    // CADASTRO DE CLIENTE
    // =========================================================

    @PostMapping("/cadastro")
    public Map<String, String> cadastro(
            @RequestBody UserRequestDTO dto
    ) {

        return service.cadastro(dto);
    }

    // =========================================================
    // CADASTRO DE GERENTE
    // =========================================================

    @PostMapping("/cadastro-gerente")
    public Map<String, String> cadastroGerente(
            @RequestBody UserRequestDTO dto
    ) {

        return service.cadastroGerente(dto);
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    public Map<String, String> login(
            @RequestBody LoginDTO dto
    ) {

        return service.login(dto);
    }

    // =========================================================
    // CONFIRMAR EMAIL
    // =========================================================

    @GetMapping("/confirmar-email/{token}")
    public Map<String, String> confirmarEmail(
            @PathVariable String token
    ) {

        return service.confirmarEmail(token);
    }

    // =========================================================
    // ESQUECI A SENHA
    // =========================================================

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