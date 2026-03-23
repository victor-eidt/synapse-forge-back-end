package com.synapseforge.service;

import com.synapseforge.dto.*;
import com.synapseforge.model.User;
import com.synapseforge.repository.UserRepository;
import com.synapseforge.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserResponseDTO cadastro(UserRequestDTO dto) {
        repository.findByEmail(dto.getEmail())
                .ifPresent(u -> { throw new RuntimeException("Email já cadastrado"); });

        String senhaHash = encoder.encode(dto.getSenha());

        User user = User.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senha(senhaHash)
                .cpf(dto.getCpf())
                .telefone(dto.getTelefone())
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(user);

        return UserResponseDTO.builder()
                .id(user.getId())
                .nome(user.getNome())
                .email(user.getEmail())
                .cpf(user.getCpf())
                .telefone(user.getTelefone())
                .build();
    }

    public String login(LoginDTO dto) {
        User user = repository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getBloqueadoAte() != null &&
                user.getBloqueadoAte().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Conta bloqueada");
        }

        if (!encoder.matches(dto.getSenha(), user.getSenha())) {
            user.setTentativasLogin(user.getTentativasLogin() + 1);
            if (user.getTentativasLogin() >= 5) {
                user.setBloqueadoAte(LocalDateTime.now().plusMinutes(15));
            }
            repository.save(user);
            throw new RuntimeException("Senha incorreta");
        }

        user.setTentativasLogin(0);
        user.setBloqueadoAte(null);
        repository.save(user);

        return jwtService.generateToken(user.getId());
    }
}
