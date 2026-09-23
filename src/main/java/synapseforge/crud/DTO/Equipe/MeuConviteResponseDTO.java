package synapseforge.crud.DTO.Equipe;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Convite pendente do usuário logado (SYN-101) junto com a equipe que convida,
 * para o app mostrar banner, foto e nome antes de a pessoa aceitar.
 */
@Getter
@AllArgsConstructor
public class MeuConviteResponseDTO {

    private ConviteEquipeResponseDTO convite;
    private EquipeResponseDTO equipe;
}
