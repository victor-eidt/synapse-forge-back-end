package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.User.LoginDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;
import synapseforge.crud.infrastructure.security.JwtService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder encoder;
    private final JwtService jwtService;

    // 🔐 CADASTRO
    public UserResponseDTO cadastro(UserRequestDTO dto) {

        // 1. Verifica se email já existe
        repository.findByEmail(dto.getEmail())
                .ifPresent(u -> {
                    throw new RuntimeException("Email já cadastrado");
                
    public void esqueciSenha(String email) {
        repository.findByEmail(email).ifPresent(user -> {
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpira(java.time.LocalDateTime.now().plusHours(1));
            repository.save(user);
            System.out.println("LINK: http://localhost:3000/redefinir-senha?token=" + token + "&email=" + email);
        });
    }

    public void redefinirSenha(String email, String token, String novaSenha) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!token.equals(user.getResetToken())) {
            throw new RuntimeException("Token inválido");
        }

        if (user.getResetTokenExpira().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        user.setSenha(encoder.encode(novaSenha));
        user.setResetToken(null);
        user.setResetTokenExpira(null);
        user.setTentativasLogin(0);
        user.setBloqueadoAte(null);

        repository.save(user);
    }

});

        // 2. Criptografa senha
        String senhaCriptografada = encoder.encode(dto.getSenha());

        // 3. Cria objeto User
        User user = new User();
        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setSenha(senhaCriptografada);
        user.setCpf(dto.getCpf());
        user.setTelefone(dto.getTelefone());
        user.setRole(dto.getRole());
        user.setAtivo(true);
        user.setCriadoEm(LocalDateTime.now());
        user.setTentativasLogin(0);

        // 4. Salva no banco
        repository.save(user);

        // 5. Retorna DTO (sem senha)
        return new UserResponseDTO(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getRole().name()
        );
    
    public void esqueciSenha(String email) {
        repository.findByEmail(email).ifPresent(user -> {
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpira(java.time.LocalDateTime.now().plusHours(1));
            repository.save(user);
            System.out.println("LINK: http://localhost:3000/redefinir-senha?token=" + token + "&email=" + email);
        });
    }

    public void redefinirSenha(String email, String token, String novaSenha) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!token.equals(user.getResetToken())) {
            throw new RuntimeException("Token inválido");
        }

        if (user.getResetTokenExpira().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        user.setSenha(encoder.encode(novaSenha));
        user.setResetToken(null);
        user.setResetTokenExpira(null);
        user.setTentativasLogin(0);
        user.setBloqueadoAte(null);

        repository.save(user);
    }

}

    // 🔐 LOGIN
    public String login(LoginDTO dto) {

        // 1. Busca usuário por email
        User user = repository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Verifica se está bloqueado
        if (user.getBloqueadoEm() != null &&
                user.getBloqueadoEm().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Conta bloqueada temporariamente");
        
    public void esqueciSenha(String email) {
        repository.findByEmail(email).ifPresent(user -> {
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpira(java.time.LocalDateTime.now().plusHours(1));
            repository.save(user);
            System.out.println("LINK: http://localhost:3000/redefinir-senha?token=" + token + "&email=" + email);
        });
    }

    public void redefinirSenha(String email, String token, String novaSenha) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!token.equals(user.getResetToken())) {
            throw new RuntimeException("Token inválido");
        }

        if (user.getResetTokenExpira().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        user.setSenha(encoder.encode(novaSenha));
        user.setResetToken(null);
        user.setResetTokenExpira(null);
        user.setTentativasLogin(0);
        user.setBloqueadoAte(null);

        repository.save(user);
    }

}

        // 3. Verifica senha
        if (user.getBloqueadoAte() != null && user.getBloqueadoAte().isAfter(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Conta bloqueada temporariamente");
        }

        if (!encoder.matches(dto.getSenha(), user.getSenha())) {

            user.setTentativasLogin(user.getTentativasLogin() + 1);

            // 4. Bloqueia após 5 tentativas
            if (user.getTentativasLogin() >= 5) {
                user.setBloqueadoEm(LocalDateTime.now().plusMinutes(15));
            
    public void esqueciSenha(String email) {
        repository.findByEmail(email).ifPresent(user -> {
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpira(java.time.LocalDateTime.now().plusHours(1));
            repository.save(user);
            System.out.println("LINK: http://localhost:3000/redefinir-senha?token=" + token + "&email=" + email);
        });
    }

    public void redefinirSenha(String email, String token, String novaSenha) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!token.equals(user.getResetToken())) {
            throw new RuntimeException("Token inválido");
        }

        if (user.getResetTokenExpira().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        user.setSenha(encoder.encode(novaSenha));
        user.setResetToken(null);
        user.setResetTokenExpira(null);
        user.setTentativasLogin(0);
        user.setBloqueadoAte(null);

        repository.save(user);
    }

}

            repository.save(user);
            throw new RuntimeException("Senha incorreta");
        
    public void esqueciSenha(String email) {
        repository.findByEmail(email).ifPresent(user -> {
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpira(java.time.LocalDateTime.now().plusHours(1));
            repository.save(user);
            System.out.println("LINK: http://localhost:3000/redefinir-senha?token=" + token + "&email=" + email);
        });
    }

    public void redefinirSenha(String email, String token, String novaSenha) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!token.equals(user.getResetToken())) {
            throw new RuntimeException("Token inválido");
        }

        if (user.getResetTokenExpira().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        user.setSenha(encoder.encode(novaSenha));
        user.setResetToken(null);
        user.setResetTokenExpira(null);
        user.setTentativasLogin(0);
        user.setBloqueadoAte(null);

        repository.save(user);
    }

}

        // 5. Reset tentativas
        user.setTentativasLogin(0);
        user.setBloqueadoEm(null);
        repository.save(user);


        return jwtService.generateToken(user.getId());
    
    public void esqueciSenha(String email) {
        repository.findByEmail(email).ifPresent(user -> {
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpira(java.time.LocalDateTime.now().plusHours(1));
            repository.save(user);
            System.out.println("LINK: http://localhost:3000/redefinir-senha?token=" + token + "&email=" + email);
        });
    }

    public void redefinirSenha(String email, String token, String novaSenha) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!token.equals(user.getResetToken())) {
            throw new RuntimeException("Token inválido");
        }

        if (user.getResetTokenExpira().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        user.setSenha(encoder.encode(novaSenha));
        user.setResetToken(null);
        user.setResetTokenExpira(null);
        user.setTentativasLogin(0);
        user.setBloqueadoAte(null);

        repository.save(user);
    }

}

    public void esqueciSenha(String email) {
        repository.findByEmail(email).ifPresent(user -> {
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpira(java.time.LocalDateTime.now().plusHours(1));
            repository.save(user);
            System.out.println("LINK: http://localhost:3000/redefinir-senha?token=" + token + "&email=" + email);
        });
    }

    public void redefinirSenha(String email, String token, String novaSenha) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!token.equals(user.getResetToken())) {
            throw new RuntimeException("Token inválido");
        }

        if (user.getResetTokenExpira().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        user.setSenha(encoder.encode(novaSenha));
        user.setResetToken(null);
        user.setResetTokenExpira(null);
        user.setTentativasLogin(0);
        user.setBloqueadoAte(null);

        repository.save(user);
    }

}