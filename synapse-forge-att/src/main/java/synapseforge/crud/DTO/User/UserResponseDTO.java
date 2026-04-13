package synapseforge.crud.DTO.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponseDTO {

    private String id;
    private String nome;
    private String email;
    private String role;
    private String cpf;
    private String telefone;

}
