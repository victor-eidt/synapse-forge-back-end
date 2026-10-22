package synapseforge.crud.DTO.Mistura;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Cor da mistura com volume e custo calculados")
public class ItemMisturaResponseDTO {

    @Schema(description = "ID da cor (tinta)", example = "6704a1c2e4b0f81a2c3d4e04")
    private String corId;
    @Schema(description = "Nome da cor", example = "Vermelho Queimado")
    private String nome;
    @Schema(description = "Fornecedor da tinta", example = "Coral Tintas")
    private String fornecedor;
    @Schema(description = "Cor em hexadecimal (#RRGGBB)", example = "#963A28")
    private String hex;
    @Schema(description = "Proporção na mistura, em %", example = "60")
    private Double proporcao;
    @Schema(description = "Volume desta cor, em mL", example = "120")
    private Integer volumeMl;
    @Schema(description = "Custo desta cor na mistura, em R$", example = "33.60")
    private Double custo;
}
