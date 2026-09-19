package synapseforge.crud.DTO.Estoque;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class CustoPedidoResponseDTO {

    private String pedidoId;
    private BigDecimal custoTotal;
    private List<CustoEtapaMetricaDTO> porEtapa;
}
