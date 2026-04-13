package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String nome;
    private String email;
    private String senha;
    private String cpf;
    private String telefone;
    private Role role;

    private int tentativasLogin = 0;

    private boolean ativo;

    private LocalDateTime criadoEm;
    private LocalDateTime bloqueadoEm;
    private LocalDateTime AtualizadoEm;

    private String resetToken;
    private LocalDateTime resetTokenExpira;
}