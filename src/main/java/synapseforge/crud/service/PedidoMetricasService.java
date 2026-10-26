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
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Pedido.MaterialPedidoMetricaDTO;
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
@Service
@RequiredArgsConstructor
public class PedidoMetricasService {

    // prazo vazio nunca conta como atrasado: no $ifNull ele vira uma data bem no futuro
    private static final Date PRAZO_INDEFINIDO = Date.from(LocalDate.of(9999, 12, 31)
            .atStartOfDay(ZoneId.systemDefault()).toInstant());

    private final MongoTemplate mongoTemplate;
    private final EquipeContexto equipeContexto;

    public List<PedidoStatusMetricaDTO> pedidosPorStatus(String usuarioId) {
        Optional<String> equipe = equipeContexto.equipeDe(usuarioId);
        if (equipe.isEmpty()) {
            return List.of();
        }
        TypedAggregation<Pedido> aggregation = Aggregation.newAggregation(Pedido.class,
                Aggregation.match(Criteria.where("equipeId").is(equipe.get())),
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

    public List<MaterialPedidoMetricaDTO> materiaisMaisUsados(int limite, String usuarioId) {
        if (limite <= 0) {
            throw new IllegalArgumentException("Limite deve ser positivo");
        }
        Optional<String> equipe = equipeContexto.equipeDe(usuarioId);
        if (equipe.isEmpty()) {
            return List.of();
        }
        // sem tipo: os campos criados no meio do pipeline (material, materialObjectId) não existem em Pedido
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("equipeId").is(equipe.get())
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
     * BigDecimal é gravado como String pelo Spring Data (o schema aceita string ou decimal);
     * $toDecimal converte no banco para o $sum funcionar. Pedido sem preço vira null e o $sum ignora.
     */
    private AggregationExpression precoFinalDecimal() {
        return ConvertOperators.valueOf("precoFinal").convertToDecimal();
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
