package synapseforge.crud.DTO.OrdemPintura;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.EtapaOrdemPintura;
import synapseforge.crud.infrastructure.entity.PrioridadeOrdemPintura;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "Ordem de pintura com dados do pedido e da cor")
public class OrdemPinturaResponseDTO {
    @Schema(description = "ID da ordem", example = "6704a1c2e4b0f81a2c3d4e08")
    private String id;
    @Schema(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01")
    private String pedidoId;
    @Schema(description = "Projeto do pedido", example = "Miniatura do Dragão Vermelho")
    private String pedidoProjeto;
    @Schema(description = "Cliente do pedido", example = "Mariana Costa")
    private String pedidoCliente;
    @Schema(description = "ID da cor (tinta)", example = "6704a1c2e4b0f81a2c3d4e04")
    private String corId;
    @Schema(description = "Nome da cor", example = "Vermelho Queimado")
    private String corNome;
    @Schema(description = "Cor em hexadecimal", example = "#963A28")
    private String corHex;
    @Schema(description = "Acabamento da tinta", example = "FOSCO")
    private String acabamento;
    @Schema(description = "ID do técnico responsável", example = "6704a1c2e4b0f81a2c3d4e05")
    private String tecnicoId;
    @Schema(description = "Nome do técnico responsável", example = "Carlos Mendes")
    private String tecnicoNome;
    @Schema(description = "Prioridade da ordem", example = "ALTA")
    private PrioridadeOrdemPintura prioridade;
    @Schema(description = "Data de entrega (yyyy-MM-dd)", example = "2026-10-20")
    private LocalDate prazo;
    @Schema(description = "Etapa da ordem", example = "EM_PINTURA")
    private EtapaOrdemPintura etapa;
    @ArraySchema(arraySchema = @Schema(description = "Imagens de referência do pedido em base64"), schema = @Schema(example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="))
    private List<String> referenciasVisuais;
    @Schema(description = "Data e hora de criação", example = "2026-10-07T14:32:10")
    private LocalDateTime criadoEm;
    @Schema(description = "Data e hora da última atualização", example = "2026-10-08T09:15:42")
    private LocalDateTime atualizadoEm;
}
