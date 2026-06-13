package synapseforge.crud.DTO.Pedido;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class PedidoResponseDTO {

    private String id;
    private String cliente;
    private String projeto;
    private String descricao;
    private StatusPedido status;
    private LocalDate prazo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    // GridFS ids for uploaded files
    private String objeto3DFileId;
    private List<String> imagensReferenciaFileIds;
    private List<String> imagensReferenciaIds;
}
