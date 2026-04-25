package synapseforge.crud.DTO.Pedido;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
}
