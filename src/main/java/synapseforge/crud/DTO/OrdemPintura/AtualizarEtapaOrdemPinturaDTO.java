package synapseforge.crud.DTO.OrdemPintura;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.EtapaOrdemPintura;

@Getter
@Setter
@Schema(description = "Nova etapa da ordem de pintura (quadro kanban)")
public class AtualizarEtapaOrdemPinturaDTO {

    @NotNull
    @Schema(description = "Etapa da ordem", example = "EM_PINTURA")
    private EtapaOrdemPintura etapa;
}
