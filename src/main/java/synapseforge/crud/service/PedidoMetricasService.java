package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.BooleanOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.aggregation.DataTypeOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Pedido.MaterialPedidoMetricaDTO;
import synapseforge.crud.DTO.Pedido.PedidoMesMetricaDTO;
import synapseforge.crud.DTO.Pedido.PedidoStatusMetricaDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

// Mesmo padrão do EstoqueMetricasService: o $match pela equipe é sempre o primeiro estágio
// (usa os índices de PedidoIndexInitializer) e usuário sem equipe recebe lista vazia.
// O período opcional (de/ate) filtra pela data de criação do pedido, no mesmo $match.
@Service
@RequiredArgsConstructor
public class PedidoMetricasService {

    // prazo vazio nunca conta como atrasado: no $ifNull ele vira uma data bem no futuro
    private static final Date PRAZO_INDEFINIDO = Date.from(LocalDate.of(9999, 12, 31)
            .atStartOfDay(ZoneId.systemDefault()).toInstant());

    private final MongoTemplate mongoTemplate;
    private final EquipeContexto equipeContexto;

    public List<PedidoStatusMetricaDTO> pedidosPorStatus(LocalDate de, LocalDate ate, String usuarioId) {
        validarPeriodo(de, ate);
        Optional<String> equipe = equipeContexto.equipeDe(usuarioId);
        if (equipe.isEmpty()) {
            return List.of();
        }
        TypedAggregation<Pedido> aggregation = Aggregation.newAggregation(Pedido.class,
                Aggregation.match(daEquipeNoPeriodo(equipe.get(), de, ate)),
                Aggregation.project("status")
                        .and(precoFinalDecimal()).as("valor")
                        .and(ConditionalOperators.when(estaAtrasado(hoje())).then(1).otherwise(0)).as("atrasado"),
                Aggregation.group("status")
                        .count().as("quantidade")
                        .sum("valor").as("valorTotal")
                        .sum("atrasado").as("atrasados"),
                Aggregation.project("quantidade", "valorTotal", "atrasados").and("_id").as("status"));

        // no banco o status é texto, então a ordem do fluxo (MODELAGEM → FINALIZADO) é aplicada aqui
        List<PedidoStatusMetricaDTO> porStatus = new ArrayList<>(
                mongoTemplate.aggregate(aggregation, PedidoStatusMetricaDTO.class).getMappedResults());
        porStatus.sort(Comparator.comparing(PedidoStatusMetricaDTO::getStatus,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return porStatus;
    }

    public List<MaterialPedidoMetricaDTO> materiaisMaisUsados(int limite, LocalDate de, LocalDate ate,
                                                              String usuarioId) {
        if (limite <= 0) {
            throw new IllegalArgumentException("Limite deve ser positivo");
        }
        validarPeriodo(de, ate);
        Optional<String> equipe = equipeContexto.equipeDe(usuarioId);
        if (equipe.isEmpty()) {
            return List.of();
        }
        // sem tipo: os campos criados no meio do pipeline (material, materialObjectId) não existem em Pedido
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(daEquipeNoPeriodo(equipe.get(), de, ate)
                        .and("materialId").ne(null)
                        .and("status").ne(StatusPedido.CANCELADO.name())),
                Aggregation.project("materialId").and(precoFinalDecimal()).as("valor"),
                Aggregation.group("materialId")
                        .count().as("quantidadePedidos")
                        .sum("valor").as("valorTotal"),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "quantidadePedidos", "valorTotal")),
                // limita antes do $lookup: só os N primeiros vão buscar o nome em "materiais"
                Aggregation.limit(limite),
                // Pedido.materialId é texto e o _id de materiais é ObjectId: converte para o join bater
                // (id inválido vira null em vez de derrubar a consulta inteira)
                Aggregation.addFields().addFieldWithValue("materialObjectId",
                        ConvertOperators.Convert.convertValueOf("_id").to("objectId").onErrorReturn(null)).build(),
                Aggregation.lookup("materiais", "materialObjectId", "_id", "material"),
                // material excluído: o pedido continua contando, só fica sem nome
                Aggregation.unwind("material", true),
                Aggregation.project("quantidadePedidos", "valorTotal")
                        .and("_id").as("materialId")
                        .and("material.nome").as("nome"));

        return mongoTemplate.aggregate(aggregation, "pedidos", MaterialPedidoMetricaDTO.class).getMappedResults();
    }

    /**
     * Pedidos criados por mês: não cancelados (quantidade e valor) e cancelados à parte.
     * Mês sem pedido não aparece; a ordem é cronológica (texto yyyy-MM ordena igual à data).
     */
    public List<PedidoMesMetricaDTO> pedidosPorMes(LocalDate de, LocalDate ate, String usuarioId) {
        validarPeriodo(de, ate);
        Optional<String> equipe = equipeContexto.equipeDe(usuarioId);
        if (equipe.isEmpty()) {
            return List.of();
        }
        AggregationExpression cancelado = ComparisonOperators.valueOf("status")
                .equalToValue(StatusPedido.CANCELADO.name());
        TypedAggregation<Pedido> aggregation = Aggregation.newAggregation(Pedido.class,
                Aggregation.match(daEquipeNoPeriodo(equipe.get(), de, ate)),
                Aggregation.project()
                        // criadoEm é gravado convertendo o LocalDateTime pelo fuso da JVM; o mês precisa
                        // sair no mesmo fuso (em UTC, um pedido de 31/08 às 22h cairia em setembro)
                        .and(DateOperators.dateOf("criadoEm")
                                .withTimezone(DateOperators.Timezone.valueOf(ZoneId.systemDefault().getId()))
                                .toString("%Y-%m")).as("mes")
                        .and(ConditionalOperators.when(cancelado).then(0).otherwise(1)).as("ativo")
                        .and(ConditionalOperators.when(cancelado).then(1).otherwise(0)).as("cancelado")
                        // cancelado não entra no valor: o campo some ($$REMOVE) e o $sum ignora
                        .and(ConditionalOperators.when(cancelado).then(Aggregation.REMOVE)
                                .otherwiseValueOf(precoFinalDecimal())).as("valor"),
                Aggregation.group("mes")
                        .sum("ativo").as("quantidade")
                        .sum("cancelado").as("cancelados")
                        .sum("valor").as("valorTotal")
                        .sum(ConditionalOperators.when(temValor("valor")).then(1).otherwise(0)).as("pedidosComPreco"),
                // o $sum de nenhum preço dá 0; sem pedido com preço no mês o valorTotal sai null
                Aggregation.project("quantidade", "cancelados")
                        .and("_id").as("mes")
                        .and(ConditionalOperators.when(ComparisonOperators.valueOf("pedidosComPreco").greaterThanValue(0))
                                .thenValueOf("valorTotal")
                                .otherwise(Aggregation.REMOVE)).as("valorTotal"),
                Aggregation.sort(Sort.Direction.ASC, "mes"));

        return mongoTemplate.aggregate(aggregation, PedidoMesMetricaDTO.class).getMappedResults();
    }

    private void validarPeriodo(LocalDate de, LocalDate ate) {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new IllegalArgumentException("A data inicial deve ser anterior ou igual à final");
        }
    }

    /**
     * Pedidos da equipe criados no período. As duas datas são opcionais (sem uma delas o período
     * fica aberto daquele lado) e "ate" inclui o dia inteiro: o limite é a meia-noite do dia seguinte.
     */
    private Criteria daEquipeNoPeriodo(String equipeId, LocalDate de, LocalDate ate) {
        Criteria criteria = Criteria.where("equipeId").is(equipeId);
        if (de != null || ate != null) {
            Criteria criadoEm = criteria.and("criadoEm");
            if (de != null) {
                criadoEm.gte(de.atStartOfDay());
            }
            if (ate != null) {
                criadoEm.lt(ate.plusDays(1).atStartOfDay());
            }
        }
        return criteria;
    }

    /**
     * BigDecimal é gravado como String pelo Spring Data (o schema aceita string ou decimal);
     * $toDecimal converte no banco para o $sum funcionar. Pedido sem preço vira null e o $sum ignora.
     */
    private AggregationExpression precoFinalDecimal() {
        return ConvertOperators.valueOf("precoFinal").convertToDecimal();
    }

    // $toDecimal devolve null para pedido sem preço e o campo some para cancelado; só preço vira "decimal"
    private AggregationExpression temValor(String campo) {
        return ComparisonOperators.Eq.valueOf(DataTypeOperators.Type.typeOf(campo)).equalToValue("decimal");
    }

    // atrasado = prazo antes de hoje e pedido ainda em produção (nem FINALIZADO nem CANCELADO)
    private AggregationExpression estaAtrasado(Date hoje) {
        return BooleanOperators.And.and(
                ComparisonOperators.Lt.valueOf(ConditionalOperators.ifNull("prazo").then(PRAZO_INDEFINIDO))
                        .lessThanValue(hoje),
                ComparisonOperators.valueOf("status").notEqualToValue(StatusPedido.FINALIZADO.name()),
                ComparisonOperators.valueOf("status").notEqualToValue(StatusPedido.CANCELADO.name()));
    }

    // LocalDate é gravado como meia-noite no fuso da JVM; hoje precisa estar na mesma base para comparar
    private Date hoje() {
        return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
