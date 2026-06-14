package synapseforge.crud.DTO.OrdemPintura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.PrioridadeOrdemPintura;

import java.time.LocalDate;

@Getter
@Setter
public class OrdemPinturaRequestDTO {

    @NotBlank
    private String pedidoId;

    @NotBlank
    private String corId;

    @NotBlank
    private String tecnico;

    @NotNull
    private PrioridadeOrdemPintura prioridade;

    @NotNull
    private LocalDate prazo;
}
