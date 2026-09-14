package synapseforge.crud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.Estoque.AlertaEstoqueResponseDTO;
import synapseforge.crud.DTO.Estoque.MovimentoEstoqueResponseDTO;
import synapseforge.crud.exception.EstoqueInsuficienteException;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.entity.ConsumoPedido;
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
import synapseforge.crud.service.politica.PoliticaConsumoFinalizado;
import synapseforge.crud.service.politica.PoliticaConsumoImpressao;
import synapseforge.crud.service.politica.PoliticaConsumoPintura;
import synapseforge.crud.service.politica.PoliticaConsumoResolver;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private CorRepository corRepository;

    @Mock
    private MovimentoEstoqueRepository movimentoRepository;

    @Mock
    private ConsumoPedidoRepository consumoPedidoRepository;

    private EstoqueService service;

    @BeforeEach
    void setUp() {
        PoliticaConsumoResolver resolver = new PoliticaConsumoResolver(List.of(
                new PoliticaConsumoImpressao(),
                new PoliticaConsumoPintura(),
                new PoliticaConsumoFinalizado()));
        service = new EstoqueService(materialRepository, corRepository, movimentoRepository,
                consumoPedidoRepository, resolver);
    }

    @Test
    void baixaFelizDebitaEGravaMovimentoComSaldoAposCorreto() {
        Material resina = material("mat1", "Resina Cinza", "500", "100");
        when(consumoPedidoRepository.findByPedidoId("ped1"))
                .thenReturn(Optional.of(ficha("ped1",
                        item(TipoInsumo.MATERIAL, "mat1", "100", UnidadeMedida.G, StatusPedido.IMPRESSAO))));
        when(movimentoRepository.findByChaveIdempotencia("ped1:MATERIAL:mat1:IMPRESSAO:BAIXA"))
                .thenReturn(Optional.empty());
        when(materialRepository.findById("mat1")).thenReturn(Optional.of(resina));

        service.baixarPorEtapa("ped1", StatusPedido.IMPRESSAO, "user1");

        ArgumentCaptor<Material> materialSalvo = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(materialSalvo.capture());
        assertThat(materialSalvo.getValue().getSaldo()).isEqualByComparingTo("400");

        ArgumentCaptor<MovimentoEstoque> movimentoSalvo = ArgumentCaptor.forClass(MovimentoEstoque.class);
        verify(movimentoRepository).save(movimentoSalvo.capture());
        MovimentoEstoque movimento = movimentoSalvo.getValue();
        assertThat(movimento.getTipo()).isEqualTo(TipoMovimento.BAIXA);
        assertThat(movimento.getQuantidade()).isEqualByComparingTo("100");
        assertThat(movimento.getSaldoApos()).isEqualByComparingTo("400");
        assertThat(movimento.getUnidade()).isEqualTo(UnidadeMedida.G);
        assertThat(movimento.getPedidoId()).isEqualTo("ped1");
        assertThat(movimento.getEtapaOrigem()).isEqualTo(StatusPedido.IMPRESSAO);
        assertThat(movimento.getChaveIdempotencia()).isEqualTo("ped1:MATERIAL:mat1:IMPRESSAO:BAIXA");
        assertThat(movimento.getCustoTotal()).isEqualByComparingTo("10.00");
    }

    @Test
    void baixaRepetidaNaMesmaEtapaNaoDebitaDuasVezes() {
        when(consumoPedidoRepository.findByPedidoId("ped1"))
                .thenReturn(Optional.of(ficha("ped1",
                        item(TipoInsumo.MATERIAL, "mat1", "100", UnidadeMedida.G, StatusPedido.IMPRESSAO))));
        when(movimentoRepository.findByChaveIdempotencia("ped1:MATERIAL:mat1:IMPRESSAO:BAIXA"))
                .thenReturn(Optional.of(new MovimentoEstoque()));

        service.baixarPorEtapa("ped1", StatusPedido.IMPRESSAO, "user1");

        verify(materialRepository, never()).save(any());
        verify(movimentoRepository, never()).save(any());
    }

    @Test
    void estornoDevolveOSaldoEMantemOsDoisMovimentos() {
        Material resina = material("mat1", "Resina Cinza", "400", "100");
        MovimentoEstoque baixa = new MovimentoEstoque();
        baixa.setTipoInsumo(TipoInsumo.MATERIAL);
        baixa.setInsumoId("mat1");
        baixa.setTipo(TipoMovimento.BAIXA);
        baixa.setEtapaOrigem(StatusPedido.IMPRESSAO);
        baixa.setQuantidade(new BigDecimal("100"));
        baixa.setCustoUnitario(new BigDecimal("0.10"));
        baixa.setCustoTotal(new BigDecimal("10.00"));

        when(movimentoRepository.findByPedidoId("ped1")).thenReturn(List.of(baixa));
        when(movimentoRepository.findByChaveIdempotencia("ped1:MATERIAL:mat1:IMPRESSAO:ESTORNO"))
                .thenReturn(Optional.empty());
        when(materialRepository.findById("mat1")).thenReturn(Optional.of(resina));

        service.estornarPorEtapa("ped1", StatusPedido.IMPRESSAO, "user1");

        ArgumentCaptor<Material> materialSalvo = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(materialSalvo.capture());
        assertThat(materialSalvo.getValue().getSaldo()).isEqualByComparingTo("500");

        ArgumentCaptor<MovimentoEstoque> movimentoSalvo = ArgumentCaptor.forClass(MovimentoEstoque.class);
        verify(movimentoRepository).save(movimentoSalvo.capture());
        MovimentoEstoque estorno = movimentoSalvo.getValue();
        assertThat(estorno.getTipo()).isEqualTo(TipoMovimento.ESTORNO);
        assertThat(estorno.getQuantidade()).isEqualByComparingTo("100");
        assertThat(estorno.getSaldoApos()).isEqualByComparingTo("500");
        assertThat(estorno.getCustoTotal()).isEqualByComparingTo("10.00");

        // nada é apagado: a baixa original permanece no histórico
        verify(movimentoRepository, never()).delete(any());
        verify(movimentoRepository, never()).deleteById(anyString());
    }

    @Test
    void saldoInsuficienteLancaExcecaoENenhumInsumoEDebitado() {
        Material resina = material("mat1", "Resina Cinza", "500", "100");
        Material filamento = material("mat2", "Filamento PLA", "50", "100");
        when(consumoPedidoRepository.findByPedidoId("ped1"))
                .thenReturn(Optional.of(ficha("ped1",
                        item(TipoInsumo.MATERIAL, "mat1", "100", UnidadeMedida.G, StatusPedido.IMPRESSAO),
                        item(TipoInsumo.MATERIAL, "mat2", "100", UnidadeMedida.G, StatusPedido.IMPRESSAO))));
        when(movimentoRepository.findByChaveIdempotencia(anyString())).thenReturn(Optional.empty());
        when(materialRepository.findById("mat1")).thenReturn(Optional.of(resina));
        when(materialRepository.findById("mat2")).thenReturn(Optional.of(filamento));

        assertThatThrownBy(() -> service.baixarPorEtapa("ped1", StatusPedido.IMPRESSAO, "user1"))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("Filamento PLA")
                .satisfies(ex -> {
                    List<EstoqueInsuficienteException.Falta> faltas =
                            ((EstoqueInsuficienteException) ex).getFaltas();
                    assertThat(faltas).hasSize(1);
                    assertThat(faltas.get(0).getNecessario()).isEqualByComparingTo("100");
                    assertThat(faltas.get(0).getDisponivel()).isEqualByComparingTo("50");
                });

        verify(materialRepository, never()).save(any());
        verify(corRepository, never()).save(any());
        verify(movimentoRepository, never()).save(any());
    }

    @Test
    void itemDeEtapaNaoAlcancadaNuncaEDebitado() {
        Material resina = material("mat1", "Resina Cinza", "500", "100");
        when(consumoPedidoRepository.findByPedidoId("ped1"))
                .thenReturn(Optional.of(ficha("ped1",
                        item(TipoInsumo.MATERIAL, "mat1", "100", UnidadeMedida.G, StatusPedido.IMPRESSAO),
                        item(TipoInsumo.MATERIAL, "emb1", "1", UnidadeMedida.UN, StatusPedido.FINALIZADO))));
        when(movimentoRepository.findByChaveIdempotencia("ped1:MATERIAL:mat1:IMPRESSAO:BAIXA"))
                .thenReturn(Optional.empty());
        when(materialRepository.findById("mat1")).thenReturn(Optional.of(resina));

        service.baixarPorEtapa("ped1", StatusPedido.IMPRESSAO, "user1");

        // a embalagem (FINALIZADO) não é sequer carregada ao processar IMPRESSAO
        verify(materialRepository, never()).findById("emb1");
        ArgumentCaptor<MovimentoEstoque> movimentoSalvo = ArgumentCaptor.forClass(MovimentoEstoque.class);
        verify(movimentoRepository).save(movimentoSalvo.capture());
        assertThat(movimentoSalvo.getValue().getInsumoId()).isEqualTo("mat1");
    }

    @Test
    void entradaEmKgEConvertidaParaGramas() {
        Material resina = material("mat1", "Resina Cinza", "500", "100");
        when(materialRepository.findById("mat1")).thenReturn(Optional.of(resina));
        when(movimentoRepository.save(any(MovimentoEstoque.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimentoEstoqueResponseDTO dto = service.registrarEntrada(TipoInsumo.MATERIAL, "mat1",
                new BigDecimal("2"), UnidadeMedida.KG, "compra", "user1");

        assertThat(dto.getQuantidade()).isEqualByComparingTo("2000");
        assertThat(dto.getUnidade()).isEqualTo(UnidadeMedida.G);
        assertThat(dto.getSaldoApos()).isEqualByComparingTo("2500");

        ArgumentCaptor<Material> materialSalvo = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).save(materialSalvo.capture());
        assertThat(materialSalvo.getValue().getSaldo()).isEqualByComparingTo("2500");
    }

    @Test
    void entradaEmLitrosEConvertidaParaMililitros() {
        Cor tinta = cor("cor1", "Azul Cobalto", 450, 500);
        when(corRepository.findById("cor1")).thenReturn(Optional.of(tinta));
        when(movimentoRepository.save(any(MovimentoEstoque.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimentoEstoqueResponseDTO dto = service.registrarEntrada(TipoInsumo.COR, "cor1",
                BigDecimal.ONE, UnidadeMedida.L, "compra", "user1");

        assertThat(dto.getQuantidade()).isEqualByComparingTo("1000");
        assertThat(dto.getUnidade()).isEqualTo(UnidadeMedida.ML);
        assertThat(dto.getSaldoApos()).isEqualByComparingTo("1450");

        ArgumentCaptor<Cor> corSalva = ArgumentCaptor.forClass(Cor.class);
        verify(corRepository).save(corSalva.capture());
        assertThat(corSalva.getValue().getEstoqueMl()).isEqualTo(1450);
    }

    @Test
    void alertaDisparaQuandoSaldoFicaIgualAoMinimo() {
        Material resina = material("mat1", "Resina Cinza", "600", "500");
        when(consumoPedidoRepository.findByPedidoId("ped1"))
                .thenReturn(Optional.of(ficha("ped1",
                        item(TipoInsumo.MATERIAL, "mat1", "100", UnidadeMedida.G, StatusPedido.IMPRESSAO))));
        when(movimentoRepository.findByChaveIdempotencia("ped1:MATERIAL:mat1:IMPRESSAO:BAIXA"))
                .thenReturn(Optional.empty());
        when(materialRepository.findById("mat1")).thenReturn(Optional.of(resina));

        service.baixarPorEtapa("ped1", StatusPedido.IMPRESSAO, "user1");
        assertThat(resina.getSaldo()).isEqualByComparingTo("500");

        when(materialRepository.findByAtivoTrue()).thenReturn(List.of(resina));
        when(corRepository.findAll()).thenReturn(List.of());

        List<AlertaEstoqueResponseDTO> alertas = service.listarEmAlerta();

        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).getInsumoId()).isEqualTo("mat1");
        assertThat(alertas.get(0).getSaldo()).isEqualByComparingTo("500");
        assertThat(alertas.get(0).getEstoqueMinimo()).isEqualByComparingTo("500");
    }

    private Material material(String id, String nome, String saldo, String estoqueMinimo) {
        Material material = new Material();
        material.setId(id);
        material.setNome(nome);
        material.setTipo("RESINA");
        material.setPrecoPorGrama(new BigDecimal("0.10"));
        material.setAtivo(true);
        material.setUnidade(UnidadeMedida.G);
        material.setSaldo(new BigDecimal(saldo));
        material.setEstoqueMinimo(new BigDecimal(estoqueMinimo));
        return material;
    }

    private Cor cor(String id, String nome, int estoqueMl, int estoqueMinimoMl) {
        Cor cor = new Cor();
        cor.setId(id);
        cor.setNome(nome);
        cor.setEstoqueMl(estoqueMl);
        cor.setEstoqueMinimoMl(estoqueMinimoMl);
        cor.setCustoMl(0.30);
        return cor;
    }

    private ConsumoPedido ficha(String pedidoId, ItemConsumo... itens) {
        ConsumoPedido consumo = new ConsumoPedido();
        consumo.setPedidoId(pedidoId);
        consumo.setItens(Arrays.asList(itens));
        return consumo;
    }

    private ItemConsumo item(TipoInsumo tipoInsumo, String insumoId, String quantidade,
                             UnidadeMedida unidade, StatusPedido etapaConsumo) {
        return new ItemConsumo(tipoInsumo, insumoId, new BigDecimal(quantidade), unidade, etapaConsumo);
    }
}
