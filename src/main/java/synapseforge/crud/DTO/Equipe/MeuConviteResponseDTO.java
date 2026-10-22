package synapseforge.crud.DTO.Equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Convite pendente do usuário logado (SYN-101) junto com a equipe que convida,
 * para o app mostrar banner, foto e nome antes de a pessoa aceitar.
 */
@Getter
@AllArgsConstructor
@Schema(description = "Convite pendente do usuário logado, com os dados da equipe")
public class MeuConviteResponseDTO {

    @Schema(description = "Dados do convite")
    private ConviteEquipeResponseDTO convite;
    @Schema(description = "Equipe que está convidando")
    private EquipeResponseDTO equipe;
}
