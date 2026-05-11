package synapseforge.crud.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService service;

    @PostMapping
    public UserResponseDTO criar(@RequestBody  @Valid UserRequestDTO dto) {

        User user = service.toEntity(dto);      // DTO → Entity
        User salvo = service.criar(user);       // salva
        return service.toResponseDTO(salvo);    // Entity → DTO
    }

    @GetMapping
    public List<UserResponseDTO> listar() {
        return service.listar()
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public UserResponseDTO buscar(@PathVariable String id) {
        User user = service.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return service.toResponseDTO(user);
    }

    @PutMapping("/{id}")
    public UserResponseDTO atualizar(@PathVariable String id, @RequestBody UserRequestDTO dto) {

        User atualizado = service.atualizar(id, dto);

        return service.toResponseDTO(atualizado);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable String id) {
        service.deletar(id);
    }

    @PostMapping("/batch")
    public List<UserResponseDTO> criarVarios(@RequestBody @Valid List<UserRequestDTO> dtos) {

        List<User> users = dtos.stream()
                .map(service::toEntity)
                .toList();

        List<User> salvos = service.criarVarios(users);

        return salvos.stream()
                .map(service::toResponseDTO)
                .toList();
    }

    @GetMapping("/search")
    public List<UserResponseDTO> buscarPorNome(@RequestParam String nome) {
        return service.buscarPorNome(nome)
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }

    @PostMapping("/{id}/solicitar-mudanca-email")
    public Map<String, String> solicitarMudancaEmail(@PathVariable String id, @RequestBody Map<String, String> body) {
        return service.solicitarMudancaEmail(id, body.get("novoEmail"));
    }

    @GetMapping("/confirmar-mudanca-email/{token}")
    public Map<String, String> confirmarMudancaEmail(@PathVariable String token) {
        service.confirmarMudancaEmail(token);
        return Map.of("mensagem", "Email alterado com sucesso!");
    }

}