package synapseforge.crud.DTO.Pedido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.time.LocalDate;

@Getter
@Setter
public class PedidoRequestDTO {

    // ID do cliente existente no sistema.
    // Pode ficar null caso o pedido não seja vinculado a nenhum cliente.
    private String clienteId;

    @NotBlank
    private String cliente;

    @NotBlank
    private String projeto;

    private String descricao;

    @NotNull
    private LocalDate prazo;

    private StatusPedido status;
}