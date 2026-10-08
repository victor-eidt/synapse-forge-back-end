package synapseforge.crud.DTO.User;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Credenciais de login")
public class LoginDTO {

    @Schema(description = "E-mail cadastrado", example = "gerente@teste.com")
    private String email;
    @Schema(description = "Senha", example = "1234")
    private String senha;
}