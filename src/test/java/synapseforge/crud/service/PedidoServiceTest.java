package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.exception.EstoqueInsuficienteException;
import synapseforge.crud.infrastructure.entity.ConsumoPedido;
import synapseforge.crud.infrastructure.entity.ItemConsumo;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.MovimentoEstoque;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.TipoMovimento;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;
import synapseforge.crud.infrastructure.repository.ConsumoPedidoRepository;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.MaterialRepository;
import synapseforge.crud.infrastructure.repository.MovimentoEstoqueRepository;
import synapseforge.crud.infrastructure.repository.PedidoRepository;
import synapseforge.crud.service.politica.PoliticaConsumoFinalizado;
import synapseforge.crud.service.politica.PoliticaConsumoImpressao;
import synapseforge.crud.service.politica.PoliticaConsumoPintura;
import synapseforge.crud.service.politica.PoliticaConsumoResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository repository;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @Mock
    private EstoqueService estoqueService;

    @InjectMocks
    private PedidoService service;

    @Test
    void toEntityDeveMapearCampos() {
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setCliente("Cliente A");
        dto.setProjeto("Projeto X");
        dto.setDescricao("Desc");
        dto.setPrazo(LocalDate.now().plusDays(5));
        dto.setStatus(StatusPedido.MODELAGEM);

        Pedido pedido = service.toEntity(dto, "user-1");

        assertEquals("Cliente A", pedido.getCliente());
        assertEquals("Projeto X", pedido.getProjeto());
        assertEquals("user-1", pedido.getUsuarioId());
        assertEquals(StatusPedido.MODELAGEM, pedido.getStatus());
    }

    @Test
    void criarDeveDefinirStatusEaDatas() {
        Pedido pedido = new Pedido();
        pedido.setCliente("Cliente A");
        when(repository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido result = service.criar(pedido);

        assertEquals(StatusPedido.MODELAGEM, result.getStatus());
        assertNotNull(result.getCriadoEm());
        assertNotNull(result.getAtualizadoEm());
    }

    @Test
    void avancarStatusDeveIrParaProximaEtapa() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.MODELAGEM);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.avancarStatus("p-1", "user-1");

        assertEquals(StatusPedido.IMPRESSAO, result.getStatus());
    }

    @Test
    void regredirStatusDeveVoltarEtapa() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.regredirStatus("p-1", "user-1");

        assertEquals(StatusPedido.MODELAGEM, result.getStatus());
    }

    @Test
    void avancarParaImpressaoDeveBaixarEstoqueDaEtapa() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.MODELAGEM);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        service.avancarStatus("p-1", "user-1");

        verify(estoqueService).baixarPorEtapa("p-1", StatusPedido.IMPRESSAO, "user-1");
    }

    @Test
    void baixaComEstoqueInsuficienteImpedeOSaveDoPedido() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.MODELAGEM);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        doThrow(new EstoqueInsuficienteException(List.of(
                new EstoqueInsuficienteException.Falta("Resina Cinza", new BigDecimal("100"), new BigDecimal("50")))))
                .when(estoqueService).baixarPorEtapa("p-1", StatusPedido.IMPRESSAO, "user-1");

        assertThrows(EstoqueInsuficienteException.class, () -> service.avancarStatus("p-1", "user-1"));

        verify(repository, never()).save(any());
    }

    @Test
    void regredirDeImpressaoDeveEstornarAEtapaAbandonada() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        service.regredirStatus("p-1", "user-1");

        verify(estoqueService).estornarPorEtapa("p-1", StatusPedido.IMPRESSAO, "user-1");
    }

    @Test
    void cancelarDeveDefinirCanceladoSemEstornar() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.cancelar("p-1", "user-1");

        assertEquals(StatusPedido.CANCELADO, result.getStatus());
        verify(estoqueService, never()).estornarPorEtapa(any(), any(), any());
    }

    @Test
    void cancelarMantemConsumoRealizadoENaoDebitaEtapasNaoAlcancadas() {
        // Regra de negócio: material consumido virou peça e não retorna ao estoque no
        // cancelamento (o saldo debitado permanece debitado), e insumo de etapa nunca
        // alcançada nunca foi debitado, então permanece intacto sem qualquer ação.
        // Usa um EstoqueService real sobre repositórios mockados para falhar se o
        // cancelamento voltar a disparar estorno ou baixa.
        MaterialRepository materialRepository = mock(MaterialRepository.class);
        CorRepository corRepository = mock(CorRepository.class);
        MovimentoEstoqueRepository movimentoRepository = mock(MovimentoEstoqueRepository.class);
        ConsumoPedidoRepository consumoPedidoRepository = mock(ConsumoPedidoRepository.class);
        EstoqueService estoqueReal = new EstoqueService(materialRepository, corRepository,
                movimentoRepository, consumoPedidoRepository,
                new PoliticaConsumoResolver(List.of(new PoliticaConsumoImpressao(),
                        new PoliticaConsumoPintura(), new PoliticaConsumoFinalizado())));
        ReflectionTestUtils.setField(service, "estoqueService", estoqueReal);

        // resina já debitada pela baixa de IMPRESSAO: saldo na prateleira é 400
        Material resina = new Material();
        resina.setId("mat1");
        resina.setSaldo(new BigDecimal("400"));

        MovimentoEstoque baixaImpressao = new MovimentoEstoque();
        baixaImpressao.setTipoInsumo(TipoInsumo.MATERIAL);
        baixaImpressao.setInsumoId("mat1");
        baixaImpressao.setTipo(TipoMovimento.BAIXA);
        baixaImpressao.setEtapaOrigem(StatusPedido.IMPRESSAO);
        baixaImpressao.setQuantidade(new BigDecimal("100"));

        // a ficha de consumo ainda prevê embalagem para FINALIZADO, etapa nunca alcançada
        ConsumoPedido ficha = new ConsumoPedido();
        ficha.setPedidoId("p-1");
        ficha.setItens(List.of(
                new ItemConsumo(TipoInsumo.MATERIAL, "mat1", new BigDecimal("100"), UnidadeMedida.G, StatusPedido.IMPRESSAO),
                new ItemConsumo(TipoInsumo.MATERIAL, "emb1", new BigDecimal("1"), UnidadeMedida.UN, StatusPedido.FINALIZADO)));

        lenient().when(movimentoRepository.findByPedidoId("p-1")).thenReturn(List.of(baixaImpressao));
        lenient().when(consumoPedidoRepository.findByPedidoId("p-1")).thenReturn(Optional.of(ficha));
        lenient().when(materialRepository.findById("mat1")).thenReturn(Optional.of(resina));

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.cancelar("p-1", "user-1");

        assertEquals(StatusPedido.CANCELADO, result.getStatus());
        // o consumo realizado permanece: saldo segue debitado e nenhum movimento novo é gravado
        assertEquals(new BigDecimal("400"), resina.getSaldo());
        verify(materialRepository, never()).save(any());
        verify(movimentoRepository, never()).save(any());
        // a embalagem da etapa não alcançada nunca é sequer carregada
        verify(materialRepository, never()).findById("emb1");
    }

    @Test
    void pedidoCanceladoNaoAvancaNemRegride() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.CANCELADO);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));

        RuntimeException aoAvancar = assertThrows(RuntimeException.class,
                () -> service.avancarStatus("p-1", "user-1"));
        RuntimeException aoRegredir = assertThrows(RuntimeException.class,
                () -> service.regredirStatus("p-1", "user-1"));

        assertEquals("Pedido cancelado não pode mudar de etapa", aoAvancar.getMessage());
        assertEquals("Pedido cancelado não pode mudar de etapa", aoRegredir.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarDeveSalvarMudancas() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setCliente("Cliente A");
        pedido.setProjeto("Projeto A");
        pedido.setDescricao("Desc A");
        pedido.setPrazo(LocalDate.now());
        pedido.setStatus(StatusPedido.MODELAGEM);

        Pedido dados = new Pedido();
        dados.setCliente("Cliente B");
        dados.setProjeto("Projeto B");
        dados.setDescricao("Desc B");
        dados.setPrazo(LocalDate.now().plusDays(2));
        dados.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.atualizar("p-1", "user-1", dados);

        assertEquals("Cliente B", result.getCliente());
        assertEquals(StatusPedido.IMPRESSAO, result.getStatus());
    }

    @Test
    void deletarDeveExcluirPedidoEArquivos() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setObjeto3DFileId(null);
        pedido.setImagensReferenciaFileIds(List.of());

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));

        ReflectionTestUtils.setField(service, "gridFsTemplate", gridFsTemplate);

        service.deletar("p-1", "user-1");

        verify(repository).deleteById("p-1");
    }
}
