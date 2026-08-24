package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Estoque.AlertaEstoqueResponseDTO;
import synapseforge.crud.DTO.Estoque.ConsumoEtapaMetricaDTO;
import synapseforge.crud.DTO.Estoque.ConsumoInsumoMetricaDTO;
import synapseforge.crud.DTO.Estoque.ConsumoMedioSemanalResponseDTO;
import synapseforge.crud.DTO.Estoque.CustoEtapaMetricaDTO;
import synapseforge.crud.DTO.Estoque.CustoPedidoResponseDTO;
import synapseforge.crud.DTO.Estoque.InsumoCriticoResponseDTO;
import synapseforge.crud.infrastructure.entity.MovimentoEstoque;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.TipoMovimento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EstoqueMetricasService {

    private static final int DIAS_JANELA_CONSUMO = 30;

    private final MongoTemplate mongoTemplate;
    private final EstoqueService estoqueService;

    public List<ConsumoInsumoMetricaDTO> consumoPorInsumo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        TypedAggregation<MovimentoEstoque> aggregation = Aggregation.newAggregation(MovimentoEstoque.class,
                Aggregation.match(criterioConsumoNoPeriodo(dataInicio, dataFim)),
                Aggregation.project("tipoInsumo", "insumoId")
                        .and(quantidadeComSinal("quantidade")).as("quantidadeConsumida")
                        .and(quantidadeComSinal("custoTotal")).as("custoConsumido"),
                Aggregation.group("tipoInsumo", "insumoId")
                        .sum("quantidadeConsumida").as("totalConsumido")
                        .sum("custoConsumido").as("custoTotal"),
                Aggregation.project("totalConsumido", "custoTotal")
                        .and("_id.tipoInsumo").as("tipoInsumo")
                        .and("_id.insumoId").as("insumoId"),
                Aggregation.sort(Sort.Direction.DESC, "custoTotal"));

        return mongoTemplate.aggregate(aggregation, ConsumoInsumoMetricaDTO.class).getMappedResults();
    }

    public CustoPedidoResponseDTO custoPorPedido(String pedidoId) {
        TypedAggregation<MovimentoEstoque> aggregation = Aggregation.newAggregation(MovimentoEstoque.class,
                Aggregation.match(Criteria.where("pedidoId").is(pedidoId)
                        .and("tipo").in(TipoMovimento.BAIXA.name(), TipoMovimento.ESTORNO.name())),
                Aggregation.project("etapaOrigem")
                        .and(quantidadeComSinal("custoTotal")).as("custoConsumido"),
                Aggregation.group("etapaOrigem")
                        .sum("custoConsumido").as("custoTotal"),
                Aggregation.project("custoTotal").and("_id").as("etapa"));

        List<CustoEtapaMetricaDTO> porEtapa = mongoTemplate.aggregate(aggregation, CustoEtapaMetricaDTO.class)
                .getMappedResults();
        BigDecimal custoTotal = porEtapa.stream()
                .map(CustoEtapaMetricaDTO::getCustoTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CustoPedidoResponseDTO(pedidoId, custoTotal, porEtapa);
    }

    public List<ConsumoEtapaMetricaDTO> consumoPorEtapa(LocalDateTime dataInicio, LocalDateTime dataFim) {
        TypedAggregation<MovimentoEstoque> aggregation = Aggregation.newAggregation(MovimentoEstoque.class,
                Aggregation.match(criterioConsumoNoPeriodo(dataInicio, dataFim)),
                Aggregation.project("etapaOrigem")
                        .and(quantidadeComSinal("quantidade")).as("quantidadeConsumida")
                        .and(quantidadeComSinal("custoTotal")).as("custoConsumido"),
                Aggregation.group("etapaOrigem")
                        .sum("quantidadeConsumida").as("totalConsumido")
                        .sum("custoConsumido").as("custoTotal"),
                Aggregation.project("totalConsumido", "custoTotal").and("_id").as("etapa"),
                Aggregation.sort(Sort.Direction.DESC, "custoTotal"));

        return mongoTemplate.aggregate(aggregation, ConsumoEtapaMetricaDTO.class).getMappedResults();
    }

    public ConsumoMedioSemanalResponseDTO consumoMedioSemanal(TipoInsumo tipoInsumo, String insumoId, int semanas) {
        if (semanas <= 0) {
            throw new IllegalArgumentException("Número de semanas deve ser positivo");
        }
        LocalDateTime inicio = LocalDateTime.now().minusWeeks(semanas);
        TypedAggregation<MovimentoEstoque> aggregation = Aggregation.newAggregation(MovimentoEstoque.class,
                Aggregation.match(Criteria.where("criadoEm").gte(inicio)
                        .and("tipo").in(TipoMovimento.BAIXA.name(), TipoMovimento.ESTORNO.name())
                        .and("tipoInsumo").is(tipoInsumo.name())
                        .and("insumoId").is(insumoId)),
                Aggregation.project()
                        .and(quantidadeComSinal("quantidade")).as("quantidadeConsumida")
                        .and(DateOperators.IsoWeekYear.isoWeekYearOf("criadoEm")).as("ano")
                        .and(DateOperators.IsoWeek.isoWeekOf("criadoEm")).as("semana"),
                Aggregation.group("ano", "semana")
                        .sum("quantidadeConsumida").as("totalSemana"),
                Aggregation.group()
                        .avg("totalSemana").as("mediaSemanal"));

        Document resultado = mongoTemplate.aggregate(aggregation, Document.class).getUniqueMappedResult();
        BigDecimal media = resultado == null ? BigDecimal.ZERO : toBigDecimal(resultado.get("mediaSemanal"));
        return new ConsumoMedioSemanalResponseDTO(tipoInsumo, insumoId, semanas, media);
    }

    public List<InsumoCriticoResponseDTO> insumosCriticos() {
        List<AlertaEstoqueResponseDTO> alertas = estoqueService.listarEmAlerta();
        if (alertas.isEmpty()) {
            return List.of();
        }

        LocalDateTime inicio = LocalDateTime.now().minusDays(DIAS_JANELA_CONSUMO);
        TypedAggregation<MovimentoEstoque> aggregation = Aggregation.newAggregation(MovimentoEstoque.class,
                Aggregation.match(Criteria.where("criadoEm").gte(inicio)
                        .and("tipo").in(TipoMovimento.BAIXA.name(), TipoMovimento.ESTORNO.name())),
                Aggregation.project("tipoInsumo", "insumoId")
                        .and(quantidadeComSinal("quantidade")).as("quantidadeConsumida"),
                Aggregation.group("tipoInsumo", "insumoId")
                        .sum("quantidadeConsumida").as("totalConsumido"));

        Map<String, BigDecimal> consumoPorInsumo = new HashMap<>();
        for (Document doc : mongoTemplate.aggregate(aggregation, Document.class).getMappedResults()) {
            Document id = (Document) doc.get("_id");
            consumoPorInsumo.put(id.getString("tipoInsumo") + ":" + id.getString("insumoId"),
                    toBigDecimal(doc.get("totalConsumido")));
        }

        List<InsumoCriticoResponseDTO> criticos = new ArrayList<>();
        for (AlertaEstoqueResponseDTO alerta : alertas) {
            BigDecimal consumido = consumoPorInsumo
                    .getOrDefault(alerta.getTipoInsumo() + ":" + alerta.getInsumoId(), BigDecimal.ZERO)
                    .max(BigDecimal.ZERO);
            BigDecimal mediaDiaria = consumido.divide(BigDecimal.valueOf(DIAS_JANELA_CONSUMO), 4, RoundingMode.HALF_UP);
            BigDecimal diasCobertura = mediaDiaria.signum() > 0
                    ? alerta.getSaldo().divide(mediaDiaria, 1, RoundingMode.HALF_UP)
                    : null;
            criticos.add(new InsumoCriticoResponseDTO(alerta.getTipoInsumo(), alerta.getInsumoId(), alerta.getNome(),
                    alerta.getUnidade(), alerta.getSaldo(), alerta.getEstoqueMinimo(), mediaDiaria, diasCobertura));
        }

        criticos.sort(Comparator.comparing(InsumoCriticoResponseDTO::getDiasCobertura,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return criticos;
    }

    private Criteria criterioConsumoNoPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        return Criteria.where("criadoEm").gte(dataInicio).lte(dataFim)
                .and("tipo").in(TipoMovimento.BAIXA.name(), TipoMovimento.ESTORNO.name());
    }

    /**
     * BigDecimal é persistido como String pelo Spring Data; $toDecimal converte no próprio banco
     * para permitir $sum/$avg, e o sinal negativo do ESTORNO faz o consumo ser líquido de estornos.
     */
    private AggregationExpression quantidadeComSinal(String campo) {
        AggregationExpression valorDecimal = ConvertOperators.valueOf(campo).convertToDecimal();
        return ConditionalOperators.when(Criteria.where("tipo").is(TipoMovimento.BAIXA.name()))
                .thenValueOf(valorDecimal)
                .otherwiseValueOf(ArithmeticOperators.Multiply.valueOf(valorDecimal).multiplyBy(-1));
    }

    private BigDecimal toBigDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof Decimal128 decimal) {
            return decimal.bigDecimalValue();
        }
        if (valor instanceof Number numero) {
            return BigDecimal.valueOf(numero.doubleValue());
        }
        return new BigDecimal(valor.toString());
    }
}
