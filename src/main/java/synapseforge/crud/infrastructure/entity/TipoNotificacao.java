package synapseforge.crud.infrastructure.entity;

public enum TipoNotificacao {
    // Legado: avisos antigos de pedido finalizado. Hoje o finalizado é um
    // PEDIDO_ETAPA_ALTERADA com detalhe "FINALIZADO"; mantido para ler o que já está no banco.
    PEDIDO_FINALIZADO,
    // técnico recebeu uma ordem de pintura (nova ou trocada para ele)
    ORDEM_PINTURA_ATRIBUIDA,
    // pedido do cliente mudou de etapa de produção (avançou ou voltou)
    PEDIDO_ETAPA_ALTERADA
}
