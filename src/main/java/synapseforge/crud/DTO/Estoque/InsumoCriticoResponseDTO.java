package synapseforge.crud.DTO.Estoque;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class InsumoCriticoResponseDTO {

    private TipoInsumo tipoInsumo;
    private String insumoId;
    private String nome;
    private UnidadeMedida unidade;
    private BigDecimal saldo;
    private BigDecimal estoqueMinimo;
    private BigDecimal consumoMedioDiario;

    // null quando não houve consumo recente: cobertura indeterminada, ordenado por último
    private BigDecimal diasCobertura;
}
