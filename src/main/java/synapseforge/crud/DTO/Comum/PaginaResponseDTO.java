package synapseforge.crud.DTO.Comum;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Uma página de resultados. `total` é o total que casa com o filtro (não só
 * o desta página), para o front mostrar "20 de 1.000" e saber se há mais.
 */
@Getter
@AllArgsConstructor
@Schema(description = "Uma página de resultados")
public class PaginaResponseDTO<T> {

    @Schema(description = "Itens desta página")
    private List<T> itens;
    @Schema(description = "Número da página (começa em 0)", example = "0")
    private int pagina;
    @Schema(description = "Quantidade de itens por página", example = "20")
    private int tamanho;
    @Schema(description = "Total de itens que atendem ao filtro", example = "137")
    private long total;
    @Schema(description = "Indica se existe próxima página", example = "true")
    private boolean temMais;
}
