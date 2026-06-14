package synapseforge.crud.DTO.OrdemPintura;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.EtapaOrdemPintura;

@Getter
@Setter
public class AtualizarEtapaOrdemPinturaDTO {

    @NotNull
    private EtapaOrdemPintura etapa;
}
