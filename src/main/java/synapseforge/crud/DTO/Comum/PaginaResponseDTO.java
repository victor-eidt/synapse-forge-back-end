package synapseforge.crud.DTO.Comum;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Uma página de resultados. `total` é o total que casa com o filtro (não só
 * o desta página), para o front mostrar "20 de 1.000" e saber se há mais.
 */
@Getter
@AllArgsConstructor
public class PaginaResponseDTO<T> {

    private List<T> itens;
    private int pagina;
    private int tamanho;
    private long total;
    private boolean temMais;
}
