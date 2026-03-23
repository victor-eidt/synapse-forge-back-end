package com.synapseforge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String nome;
    private String email;
    private String senha;
    private String cpf;
    private String telefone;
    private boolean ativo = true;
    private int tentativasLogin = 0;
    private LocalDateTime bloqueadoAte;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
