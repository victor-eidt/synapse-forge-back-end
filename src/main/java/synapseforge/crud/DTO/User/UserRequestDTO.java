package synapseforge.crud.DTO.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.Role;


@Getter
@Setter
public class UserRequestDTO {

    @NotBlank
    private String nome;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String senha;


    private Role role;

    private String cpf;

    private String telefone;

}