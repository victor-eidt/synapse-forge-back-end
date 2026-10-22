package synapseforge.crud.DTO.Orcamento;

import java.time.LocalDate;

/**
 * Filtros da busca paginada de orçamentos. Campos nulos ou vazios não filtram.
 * `de` e `ate` se referem à data de criação do orçamento e são inclusivas.
 */
public record FiltroOrcamentoDTO(
        SituacaoOrcamento situacao,
        String cliente,
        String projeto,
        LocalDate de,
        LocalDate ate
) {
}
