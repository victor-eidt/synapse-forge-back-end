package synapseforge.crud.DTO.Estoque;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SaldoInsumoResponseDTO {

    private TipoInsumo tipoInsumo;
    private String insumoId;
    private String nome;
    private UnidadeMedida unidade;
    private BigDecimal saldo;
    private BigDecimal estoqueMinimo;
    private Boolean emAlerta;
}
