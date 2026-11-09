package synapseforge.crud.infrastructure.entity;

/**
 * Situação do envio do pedido ao cliente. Independe da etapa de produção
 * ({@link StatusPedido}): um pedido FINALIZADO pode ainda estar PREPARANDO o envio.
 */
public enum StatusEnvio {
    PREPARANDO,
    POSTADO,
    EM_TRANSITO,
    ENTREGUE
}
