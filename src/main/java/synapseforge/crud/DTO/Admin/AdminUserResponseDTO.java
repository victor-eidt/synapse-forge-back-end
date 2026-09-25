package synapseforge.crud.DTO.Admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminUserResponseDTO {

    private String id;
    private String nome;
    private String email;
    private String cpf;
    private String telefone;
    private String role;
    private String equipeId;
    private String funcaoVisual;
    private boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}

