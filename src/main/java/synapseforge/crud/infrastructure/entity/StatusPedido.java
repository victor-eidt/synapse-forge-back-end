package synapseforge.crud.infrastructure.entity;

import java.util.List;

public enum StatusPedido {
    MODELAGEM,
    IMPRESSAO,
    PINTURA,
    ACABAMENTO,
    FINALIZADO,
    CANCELADO;

    // CANCELADO fica fora do fluxo navegável: é estado terminal, não etapa de produção
    public static final List<StatusPedido> ETAPAS_PRODUCAO =
            List.of(MODELAGEM, IMPRESSAO, PINTURA, ACABAMENTO, FINALIZADO);

    public static int indiceEtapaProducao(StatusPedido etapa) {
        return ETAPAS_PRODUCAO.indexOf(etapa);
    }
}
