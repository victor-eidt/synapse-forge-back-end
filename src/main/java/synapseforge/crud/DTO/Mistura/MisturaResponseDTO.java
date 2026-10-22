package synapseforge.crud.DTO.Mistura;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "Receita de mistura com cor resultante e custo")
public class MisturaResponseDTO {

    @Schema(description = "ID da mistura", example = "6704a1c2e4b0f81a2c3d4e0c")
    private String id;
    @Schema(description = "Nome da mistura", example = "Laranja Pôr do Sol")
    private String nome;
    @Schema(description = "Cores da mistura")
    private List<ItemMisturaResponseDTO> itens;
    @Schema(description = "Volume total da mistura, em mL", example = "200")
    private Integer volumeMl;
    @Schema(description = "Cor resultante estimada (#RRGGBB)", example = "#D9693B")
    private String hexResultado;
    @Schema(description = "Custo estimado da mistura, em R$", example = "52.40")
    private Double custoEstimado;
    @Schema(description = "Data e hora de criação", example = "2026-10-07T14:32:10")
    private LocalDateTime criadoEm;
    @Schema(description = "Data e hora da última atualização", example = "2026-10-08T09:15:42")
    private LocalDateTime atualizadoEm;
}
