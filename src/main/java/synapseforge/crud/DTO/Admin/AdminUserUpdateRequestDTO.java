package synapseforge.crud.DTO.Admin;

import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.Role;

@Getter
@Setter
public class AdminUserUpdateRequestDTO {

    private String nome;
    private String email;
    private String cpf;
    private String telefone;
    private Role role;
    private String equipeId;
    private String funcaoVisual;
    private Boolean ativo;
    private String senha;
}