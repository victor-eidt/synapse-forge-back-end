package synapseforge.crud.controller;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.User.LoginDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/cadastro")
    public UserResponseDTO cadastro(@RequestBody @Valid UserRequestDTO dto) {
        return service.cadastro(dto);
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginDTO dto) {
        String response = service.login(dto);
        return Map.of("access_token", response);
    }

    @PostMapping("/esqueci-senha")
    public String esqueciSenha(@RequestBody Map<String, String> body) {
        service.esqueciSenha(body.get("email"));
        return "Se o email existir, você receberá instruções";
    }

    @PostMapping("/redefinir-senha")
    public String redefinirSenha(@RequestBody Map<String, String> body) {
        service.redefinirSenha(
            body.get("email"),
            body.get("token"),
            body.get("novaSenha")
        );
        return "Senha redefinida com sucesso";
    }
}
