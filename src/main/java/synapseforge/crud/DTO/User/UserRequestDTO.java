package synapseforge.crud.DTO.User;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.Role;


@Getter
@Setter
@Schema(description = "Cadastro de usuário")
public class UserRequestDTO {

    @NotBlank
    @Schema(description = "Nome completo", example = "Mariana Costa")
    private String nome;

    @NotBlank
    @Email
    @Schema(description = "E-mail do usuário", example = "mariana.costa@email.com")
    private String email;

    @Schema(description = "Senha de acesso", example = "Senha@2026")
    private String senha;


    @Schema(description = "Papel; no cadastro público é sempre CLIENTE", example = "CLIENTE")
    private Role role;

    @Schema(description = "CPF do usuário", example = "529.982.247-25")
    private String cpf;

    @Schema(description = "Telefone com DDD", example = "(41) 99876-5432")
    private String telefone;

    // Só no cadastro de gerente: nome da loja, criada junto com a conta.
    @Schema(description = "Nome da equipe (só no cadastro de gerente)", example = "Oficina Synapse Curitiba")
    private String nomeEquipe;

}