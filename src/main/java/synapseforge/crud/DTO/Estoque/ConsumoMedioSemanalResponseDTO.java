package synapseforge.crud.DTO.Estoque;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.TipoInsumo;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ConsumoMedioSemanalResponseDTO {

    private TipoInsumo tipoInsumo;
    private String insumoId;
    private Integer semanasConsideradas;
    private BigDecimal mediaSemanal;
}
