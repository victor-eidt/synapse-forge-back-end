package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Estoque.AlertaEstoqueResponseDTO;
import synapseforge.crud.DTO.Estoque.MovimentoEstoqueResponseDTO;
import synapseforge.crud.DTO.Estoque.SaldoInsumoResponseDTO;
import synapseforge.crud.exception.EstoqueInsuficienteException;
import synapseforge.crud.infrastructure.entity.ConsumoPedido;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.entity.ItemConsumo;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.MovimentoEstoque;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.TipoMovimento;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;
import synapseforge.crud.infrastructure.repository.ConsumoPedidoRepository;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.MaterialRepository;
import synapseforge.crud.infrastructure.repository.MovimentoEstoqueRepository;
import synapseforge.crud.service.politica.PoliticaConsumo;
import synapseforge.crud.service.politica.PoliticaConsumoResolver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Estoque por equipe: insumos, fichas de consumo e movimentos só são lidos/gravados na
// equipe do usuário logado; insumo de outra equipe se comporta como inexistente.
@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final MaterialRepository materialRepository;
    private final CorRepository corRepository;
    private final MovimentoEstoqueRepository movimentoRepository;
    private final ConsumoPedidoRepository consumoPedidoRepository;
    private final PoliticaConsumoResolver politicaResolver;
    private final EquipeContexto equipeContexto;

    public MovimentoEstoqueResponseDTO registrarEntrada(TipoInsumo tipoInsumo, String insumoId, BigDecimal quantidade,
                                                        UnidadeMedida unidade, String motivo, String usuarioId) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade da entrada deve ser positiva");
        }
        String equipeId = equipeContexto.equipeObrigatoria(usuarioId);
        InsumoEstoque insumo = carregarInsumo(tipoInsumo, insumoId, equipeId);
        BigDecimal quantidadeBase = converterParaBase(insumo, quantidade, unidade);
        BigDecimal novoSaldo = insumo.saldo.add(quantidadeBase);

        salvarSaldo(insumo, novoSaldo);
        MovimentoEstoque movimento = novoMovimento(insumo, TipoMovimento.ENTRADA, quantidadeBase, novoSaldo,
                null, null, motivo, usuarioId, equipeId, UUID.randomUUID().toString());
        return toMovimentoDTO(movimentoRepository.save(movimento));
    }

    public MovimentoEstoqueResponseDTO ajustar(TipoInsumo tipoInsumo, String insumoId, BigDecimal quantidade,
                                               UnidadeMedida unidade, String motivo, String usuarioId) {
        if (quantidade == null || quantidade.signum() == 0) {
            throw new IllegalArgumentException("Quantidade do ajuste deve ser diferente de zero");
        }
        String equipeId = equipeContexto.equipeObrigatoria(usuarioId);
        InsumoEstoque insumo = carregarInsumo(tipoInsumo, insumoId, equipeId);
        BigDecimal quantidadeBase = converterParaBase(insumo, quantidade, unidade);
        BigDecimal novoSaldo = insumo.saldo.add(quantidadeBase);
        if (novoSaldo.signum() < 0) {
            throw new EstoqueInsuficienteException(List.of(
                    new EstoqueInsuficienteException.Falta(insumo.nome, quantidadeBase.abs(), insumo.saldo)));
        }

        salvarSaldo(insumo, novoSaldo);
        MovimentoEstoque movimento = novoMovimento(insumo, TipoMovimento.AJUSTE, quantidadeBase, novoSaldo,
                null, null, motivo, usuarioId, equipeId, UUID.randomUUID().toString());
        return toMovimentoDTO(movimentoRepository.save(movimento));
    }

    public SaldoInsumoResponseDTO consultarSaldo(TipoInsumo tipoInsumo, String insumoId, String usuarioId) {
        InsumoEstoque insumo = carregarInsumoDaEquipeDe(tipoInsumo, insumoId, usuarioId);
        return new SaldoInsumoResponseDTO(
                insumo.tipo,
                insumo.id,
                insumo.nome,
                insumo.unidadeBase,
                insumo.saldo,
                insumo.estoqueMinimo,
                insumo.saldo.compareTo(insumo.estoqueMinimo) <= 0
        );
    }

    public List<AlertaEstoqueResponseDTO> listarEmAlerta() {
        return listarEmAlerta(null);
    }

    public List<AlertaEstoqueResponseDTO> listarEmAlerta(String usuarioId) {
        if (equipeContexto == null) {
            return List.of();
        }
        return equipeContexto.equipeDe(usuarioId)
                .map(this::listarEmAlertaDaEquipe)
                .orElse(List.of());
    }

    public List<AlertaEstoqueResponseDTO> listarEmAlertaDaEquipe(String equipeId) {
        List<AlertaEstoqueResponseDTO> alertas = new ArrayList<>();

        for (Material material : materialRepository.findByEquipeIdAndAtivoTrue(equipeId)) {
            BigDecimal saldo = zeroSeNulo(material.getSaldo());
            BigDecimal minimo = zeroSeNulo(material.getEstoqueMinimo());
            if (saldo.compareTo(minimo) <= 0) {
                alertas.add(new AlertaEstoqueResponseDTO(TipoInsumo.MATERIAL, material.getId(), material.getNome(),
                        unidadeBaseDe(material), saldo, minimo));
            }
        }

        for (Cor cor : corRepository.findByEquipeId(equipeId)) {
            BigDecimal saldo = BigDecimal.valueOf(cor.getEstoqueMl() == null ? 0 : cor.getEstoqueMl());
            BigDecimal minimo = BigDecimal.valueOf(cor.getEstoqueMinimoMl() == null ? 0 : cor.getEstoqueMinimoMl());
            if (saldo.compareTo(minimo) <= 0) {
                alertas.add(new AlertaEstoqueResponseDTO(TipoInsumo.COR, cor.getId(), cor.getNome(),
                        UnidadeMedida.ML, saldo, minimo));
            }
        }
        return alertas;
    }

    public void baixarPorEtapa(String pedidoId, StatusPedido etapa, String usuarioId) {
        String equipeId = equipeContexto.equipeObrigatoria(usuarioId);
        ConsumoPedido consumo = consumoPedidoRepository.findByPedidoIdAndEquipeId(pedidoId, equipeId).orElse(null);
        if (consumo == null) {
            return;
        }
        PoliticaConsumo politica = politicaResolver.paraEtapa(etapa).orElse(null);
        if (politica == null) {
            return;
        }

        List<MovimentoEstoque> movimentosDoPedido = movimentoRepository.findByEquipeIdAndPedidoId(equipeId, pedidoId);
        List<BaixaPendente> pendentes = new ArrayList<>();
        List<EstoqueInsuficienteException.Falta> faltas = new ArrayList<>();
        for (ItemConsumo item : politica.itensDe(consumo)) {
            int ciclo = contarEstornos(movimentosDoPedido, item.getTipoInsumo(), item.getInsumoId(), etapa);
            String chave = chaveIdempotencia(pedidoId, item.getTipoInsumo(), item.getInsumoId(), etapa, TipoMovimento.BAIXA, ciclo);
            if (movimentoRepository.findByChaveIdempotencia(chave).isPresent()) {
                continue;
            }
            InsumoEstoque insumo = carregarInsumo(item.getTipoInsumo(), item.getInsumoId(), equipeId);
            BigDecimal quantidadeBase = converterParaBase(insumo, item.getQuantidade(), item.getUnidade());
            if (insumo.saldo.compareTo(quantidadeBase) < 0) {
                faltas.add(new EstoqueInsuficienteException.Falta(insumo.nome, quantidadeBase, insumo.saldo));
            } else {
                pendentes.add(new BaixaPendente(insumo, quantidadeBase, chave));
            }
        }
        if (!faltas.isEmpty()) {
            throw new EstoqueInsuficienteException(faltas);
        }

        for (BaixaPendente pendente : pendentes) {
            BigDecimal novoSaldo = pendente.insumo.saldo.subtract(pendente.quantidadeBase);
            salvarSaldo(pendente.insumo, novoSaldo);
            movimentoRepository.save(novoMovimento(pendente.insumo, TipoMovimento.BAIXA, pendente.quantidadeBase,
                    novoSaldo, pedidoId, etapa, "Consumo do pedido na etapa " + etapa, usuarioId, equipeId,
                    pendente.chave));
        }
    }

    public void estornarPorEtapa(String pedidoId, StatusPedido etapa, String usuarioId) {
        String equipeId = equipeContexto.equipeObrigatoria(usuarioId);
        List<MovimentoEstoque> baixas = movimentoRepository.findByEquipeIdAndPedidoId(equipeId, pedidoId).stream()
                .filter(m -> m.getTipo() == TipoMovimento.BAIXA && m.getEtapaOrigem() == etapa)
                .toList();

        for (MovimentoEstoque baixa : baixas) {
            String chaveEstorno = chaveIdempotencia(pedidoId, baixa.getTipoInsumo(), baixa.getInsumoId(),
                    etapa, TipoMovimento.ESTORNO, cicloDaChave(baixa.getChaveIdempotencia()));
            if (movimentoRepository.findByChaveIdempotencia(chaveEstorno).isPresent()) {
                continue;
            }
            InsumoEstoque insumo = carregarInsumo(baixa.getTipoInsumo(), baixa.getInsumoId(), equipeId);
            BigDecimal novoSaldo = insumo.saldo.add(baixa.getQuantidade());
            salvarSaldo(insumo, novoSaldo);
            MovimentoEstoque estorno = novoMovimento(insumo, TipoMovimento.ESTORNO, baixa.getQuantidade(), novoSaldo,
                    pedidoId, etapa, "Estorno da baixa da etapa " + etapa, usuarioId, equipeId, chaveEstorno);
            // estorno devolve exatamente o custo registrado na baixa, não o custo atual do insumo
            estorno.setCustoUnitario(baixa.getCustoUnitario());
            estorno.setCustoTotal(baixa.getCustoTotal());
            movimentoRepository.save(estorno);
        }
    }

    public List<MovimentoEstoqueResponseDTO> historicoPorInsumo(TipoInsumo tipoInsumo, String insumoId, String usuarioId) {
        // insumo de outra equipe -> não encontrado (não apenas lista vazia)
        InsumoEstoque insumo = carregarInsumoDaEquipeDe(tipoInsumo, insumoId, usuarioId);
        return movimentoRepository.findByEquipeIdAndTipoInsumoAndInsumoIdOrderByCriadoEmDesc(
                        insumo.equipeId, tipoInsumo, insumoId).stream()
                .map(this::toMovimentoDTO)
                .toList();
    }

    // A chave começa pelo pedidoId (ObjectId único no banco inteiro), então não colide entre
    // equipes mesmo com o índice único global; por isso ela não precisa do equipeId.
    // O ciclo separa reentradas na etapa: cada ESTORNO abre um ciclo novo, então a baixa
    // seguinte da mesma etapa ganha chave própria e volta a debitar. Dentro do mesmo ciclo
    // a chave se repete e o índice único do banco segue recusando o segundo movimento.
    private String chaveIdempotencia(String pedidoId, TipoInsumo tipoInsumo, String insumoId,
                                     StatusPedido etapaOrigem, TipoMovimento tipo, int ciclo) {
        return pedidoId + ":" + tipoInsumo + ":" + insumoId + ":" + etapaOrigem + ":" + tipo + ":" + ciclo;
    }

    private int contarEstornos(List<MovimentoEstoque> movimentosDoPedido, TipoInsumo tipoInsumo,
                               String insumoId, StatusPedido etapa) {
        return (int) movimentosDoPedido.stream()
                .filter(m -> m.getTipo() == TipoMovimento.ESTORNO
                        && m.getTipoInsumo() == tipoInsumo
                        && insumoId.equals(m.getInsumoId())
                        && m.getEtapaOrigem() == etapa)
                .count();
    }

    private int cicloDaChave(String chave) {
        if (chave != null) {
            int separador = chave.lastIndexOf(':');
            if (separador >= 0) {
                try {
                    return Integer.parseInt(chave.substring(separador + 1));
                } catch (NumberFormatException ignored) {
                    // movimento gravado antes de a chave ganhar ciclo
                }
            }
        }
        return 0;
    }

    private BigDecimal converterParaBase(InsumoEstoque insumo, BigDecimal quantidade, UnidadeMedida unidade) {
        if (unidade == null) {
            throw new IllegalArgumentException("Unidade de medida é obrigatória");
        }
        if (unidade.base() != insumo.unidadeBase) {
            throw new IllegalArgumentException("Unidade " + unidade + " incompatível com o insumo "
                    + insumo.nome + " (unidade base " + insumo.unidadeBase + ")");
        }
        return unidade.paraBase(quantidade);
    }

    // leitura: sem equipe, o insumo simplesmente não existe (mesma mensagem de não encontrado)
    private InsumoEstoque carregarInsumoDaEquipeDe(TipoInsumo tipoInsumo, String insumoId, String usuarioId) {
        String equipeId = equipeContexto.equipeDe(usuarioId)
                .orElseThrow(() -> new RuntimeException(tipoInsumo == TipoInsumo.MATERIAL
                        ? "Material não encontrado" : "Cor não encontrada"));
        return carregarInsumo(tipoInsumo, insumoId, equipeId);
    }

    private InsumoEstoque carregarInsumo(TipoInsumo tipoInsumo, String insumoId, String equipeId) {
        if (tipoInsumo == TipoInsumo.MATERIAL) {
            Material material = materialRepository.findByIdAndEquipeId(insumoId, equipeId)
                    .orElseThrow(() -> new RuntimeException("Material não encontrado"));
            return new InsumoEstoque(TipoInsumo.MATERIAL, material.getId(), material.getNome(),
                    unidadeBaseDe(material), zeroSeNulo(material.getSaldo()), zeroSeNulo(material.getEstoqueMinimo()),
                    zeroSeNulo(material.getPrecoPorGrama()), equipeId, material);
        }
        Cor cor = corRepository.findByIdAndEquipeId(insumoId, equipeId)
                .orElseThrow(() -> new RuntimeException("Cor não encontrada"));
        BigDecimal saldo = BigDecimal.valueOf(cor.getEstoqueMl() == null ? 0 : cor.getEstoqueMl());
        BigDecimal minimo = BigDecimal.valueOf(cor.getEstoqueMinimoMl() == null ? 0 : cor.getEstoqueMinimoMl());
        BigDecimal custo = BigDecimal.valueOf(cor.getCustoMl() == null ? 0 : cor.getCustoMl());
        return new InsumoEstoque(TipoInsumo.COR, cor.getId(), cor.getNome(), UnidadeMedida.ML, saldo, minimo, custo,
                equipeId, cor);
    }

    private void salvarSaldo(InsumoEstoque insumo, BigDecimal novoSaldo) {
        if (insumo.entidade instanceof Material material) {
            material.setSaldo(novoSaldo);
            materialRepository.save(material);
        } else if (insumo.entidade instanceof Cor cor) {
            // Cor guarda o estoque como Integer em ML; arredonda apenas na persistência
            cor.setEstoqueMl(novoSaldo.setScale(0, RoundingMode.HALF_UP).intValueExact());
            cor.setAtualizadoEm(LocalDateTime.now());
            corRepository.save(cor);
        }
        insumo.saldo = novoSaldo;
    }

    private MovimentoEstoque novoMovimento(InsumoEstoque insumo, TipoMovimento tipo, BigDecimal quantidadeBase,
                                           BigDecimal saldoApos, String pedidoId, StatusPedido etapaOrigem,
                                           String motivo, String usuarioId, String equipeId,
                                           String chaveIdempotencia) {
        MovimentoEstoque movimento = new MovimentoEstoque();
        movimento.setEquipeId(equipeId);
        movimento.setTipoInsumo(insumo.tipo);
        movimento.setInsumoId(insumo.id);
        movimento.setTipo(tipo);
        movimento.setQuantidade(quantidadeBase);
        movimento.setUnidade(insumo.unidadeBase);
        movimento.setSaldoApos(saldoApos);
        movimento.setCustoUnitario(insumo.custoUnitario);
        movimento.setCustoTotal(insumo.custoUnitario.multiply(quantidadeBase));
        movimento.setPedidoId(pedidoId);
        movimento.setEtapaOrigem(etapaOrigem);
        movimento.setMotivo(motivo);
        movimento.setUsuarioId(usuarioId);
        movimento.setCriadoEm(LocalDateTime.now());
        movimento.setChaveIdempotencia(chaveIdempotencia);
        return movimento;
    }

    private MovimentoEstoqueResponseDTO toMovimentoDTO(MovimentoEstoque movimento) {
        return new MovimentoEstoqueResponseDTO(
                movimento.getId(),
                movimento.getTipoInsumo(),
                movimento.getInsumoId(),
                movimento.getTipo(),
                movimento.getQuantidade(),
                movimento.getUnidade(),
                movimento.getSaldoApos(),
                movimento.getCustoUnitario(),
                movimento.getCustoTotal(),
                movimento.getPedidoId(),
                movimento.getEtapaOrigem(),
                movimento.getMotivo(),
                movimento.getUsuarioId(),
                movimento.getCriadoEm()
        );
    }

    private UnidadeMedida unidadeBaseDe(Material material) {
        return material.getUnidade() == null ? UnidadeMedida.G : material.getUnidade().base();
    }

    private BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private record BaixaPendente(InsumoEstoque insumo, BigDecimal quantidadeBase, String chave) {}

    // visão uniforme de Material e Cor para as operações de saldo
    private static class InsumoEstoque {
        private final TipoInsumo tipo;
        private final String id;
        private final String nome;
        private final UnidadeMedida unidadeBase;
        private BigDecimal saldo;
        private final BigDecimal estoqueMinimo;
        private final BigDecimal custoUnitario;
        private final String equipeId;
        private final Object entidade;

        private InsumoEstoque(TipoInsumo tipo, String id, String nome, UnidadeMedida unidadeBase,
                              BigDecimal saldo, BigDecimal estoqueMinimo, BigDecimal custoUnitario,
                              String equipeId, Object entidade) {
            this.tipo = tipo;
            this.id = id;
            this.nome = nome;
            this.unidadeBase = unidadeBase;
            this.saldo = saldo;
            this.estoqueMinimo = estoqueMinimo;
            this.custoUnitario = custoUnitario;
            this.equipeId = equipeId;
            this.entidade = entidade;
        }
    }
}
