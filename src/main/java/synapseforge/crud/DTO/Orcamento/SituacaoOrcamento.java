package synapseforge.crud.DTO.Orcamento;

/**
 * Recorte da listagem de orçamentos: os que ainda esperam decisão do gerente
 * e os que já foram aprovados ou rejeitados (histórico).
 */
public enum SituacaoOrcamento {
    PENDENTES,
    DECIDIDOS
}
