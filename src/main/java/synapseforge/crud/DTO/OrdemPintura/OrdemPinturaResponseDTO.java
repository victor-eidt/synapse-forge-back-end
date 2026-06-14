package synapseforge.crud.DTO.OrdemPintura;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.EtapaOrdemPintura;
import synapseforge.crud.infrastructure.entity.PrioridadeOrdemPintura;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrdemPinturaResponseDTO {
    private String id;
    private String pedidoId;
    private String pedidoProjeto;
    private String pedidoCliente;
    private String corId;
    private String corNome;
    private String corHex;
    private String acabamento;
    private String tecnicoNome;
    private PrioridadeOrdemPintura prioridade;
    private LocalDate prazo;
    private EtapaOrdemPintura etapa;
    private List<String> referenciasVisuais;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
