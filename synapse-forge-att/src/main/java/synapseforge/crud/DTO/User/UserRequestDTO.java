package synapseforge.crud.DTO.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 digitos numericos")
    private String cpf;

    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve conter 10 ou 11 digitos numericos")
    private String telefone;

}
