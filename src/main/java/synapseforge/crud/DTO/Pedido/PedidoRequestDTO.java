package synapseforge.crud.DTO.Pedido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PedidoRequestDTO {

    @NotBlank
    private String cliente;

    @NotBlank
    private String projeto;

    private String descricao;

    @NotNull
    private LocalDate prazo;
}
