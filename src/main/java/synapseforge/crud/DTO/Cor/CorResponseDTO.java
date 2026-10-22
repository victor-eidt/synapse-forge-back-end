package synapseforge.crud.DTO.Cor;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.Acabamento;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "Cor (tinta) da paleta da equipe")
public class CorResponseDTO {

    @Schema(description = "ID da cor", example = "6704a1c2e4b0f81a2c3d4e04")
    private String id;
    @Schema(description = "Nome da cor", example = "Vermelho Queimado")
    private String nome;
    @Schema(description = "Fornecedor da tinta", example = "Coral Tintas")
    private String fornecedor;
    @Schema(description = "Código da tinta no fornecedor", example = "CT-204")
    private String codigo;
    @Schema(description = "Cor em hexadecimal (#RRGGBB)", example = "#963A28")
    private String hex;
    @Schema(description = "Acabamento da tinta", example = "FOSCO")
    private Acabamento acabamento;
    @Schema(description = "Estoque atual da tinta, em mL", example = "450")
    private Integer estoqueMl;
    @Schema(description = "Estoque mínimo da tinta, em mL", example = "200")
    private Integer estoqueMinimoMl;
    @Schema(description = "Custo da tinta por mL, em R$", example = "0.28")
    private Double custoMl;
    @Schema(description = "Data e hora de criação", example = "2026-10-07T14:32:10")
    private LocalDateTime criadoEm;
    @Schema(description = "Data e hora da última atualização", example = "2026-10-08T09:15:42")
    private LocalDateTime atualizadoEm;
}
