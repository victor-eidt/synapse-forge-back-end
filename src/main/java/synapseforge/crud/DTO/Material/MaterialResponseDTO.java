package synapseforge.crud.DTO.Material;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MaterialResponseDTO {

    private String id;
    private String nome;
    private String tipo;
    private Double densidadeGcm3;
    private BigDecimal precoPorGrama;
    private Boolean ativo;
}
