package synapseforge.crud.service.politica;

import org.springframework.stereotype.Component;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Contexto do padrão Strategy: recebe todas as políticas via injeção e as indexa por etapa,
 * de modo que etapas sem política (ex.: MODELAGEM) simplesmente não consomem estoque.
 */
@Component
public class PoliticaConsumoResolver {

    private final Map<StatusPedido, PoliticaConsumo> politicasPorEtapa;

    public PoliticaConsumoResolver(List<PoliticaConsumo> politicas) {
        this.politicasPorEtapa = politicas.stream()
                .collect(Collectors.toUnmodifiableMap(PoliticaConsumo::etapa, Function.identity()));
    }

    public Optional<PoliticaConsumo> paraEtapa(StatusPedido etapa) {
        return Optional.ofNullable(politicasPorEtapa.get(etapa));
    }
}
