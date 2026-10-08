package synapseforge.crud.DTO.Estoque;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.TipoInsumo;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Schema(description = "Média de consumo semanal de um insumo")
public class ConsumoMedioSemanalResponseDTO {

    @Schema(description = "Tipo do insumo: MATERIAL (filamento/resina) ou COR (tinta)", example = "MATERIAL")
    private TipoInsumo tipoInsumo;
    @Schema(description = "ID do material ou da cor, conforme tipoInsumo", example = "6704a1c2e4b0f81a2c3d4e03")
    private String insumoId;
    @Schema(description = "Quantidade de semanas usadas na média", example = "4")
    private Integer semanasConsideradas;
    @Schema(description = "Consumo médio por semana", example = "316.400")
    private BigDecimal mediaSemanal;
}
