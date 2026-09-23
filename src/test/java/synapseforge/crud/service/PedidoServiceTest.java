package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.exception.EstoqueInsuficienteException;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.infrastructure.entity.ConsumoPedido;
import synapseforge.crud.infrastructure.entity.ItemConsumo;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.MovimentoEstoque;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.TipoMovimento;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.ConsumoPedidoRepository;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.MaterialRepository;
import synapseforge.crud.infrastructure.repository.MovimentoEstoqueRepository;
import synapseforge.crud.infrastructure.repository.PedidoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;
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

    @Mock
    private UserRepository userRepository;

    @Mock
    private EquipeContexto equipeContexto;

    @InjectMocks
    private PedidoService service;

    @BeforeEach
    void setUp() {
        // user-1 e user-2 são da eq-1; user-9 é de outra oficina (eq-2); semEquipe não entrou em nenhuma.
        // cliente-1 é CLIENTE: não tem equipe e nunca consulta o EquipeContexto
        naEquipe("user-1", "eq-1");
        naEquipe("user-2", "eq-1");
        naEquipe("user-9", "eq-2");
        lenient().when(equipeContexto.equipeDe("semEquipe")).thenReturn(Optional.empty());
        lenient().when(equipeContexto.equipeObrigatoria("semEquipe")).thenThrow(new SemEquipeException());
    }

    private void naEquipe(String usuarioId, String equipeId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.of(equipeId));
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenReturn(equipeId);
    }

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
        pedido.setUsuarioId("user-1");
        pedido.setCliente("Cliente A");
        when(repository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido result = service.criar(pedido);

        assertEquals(StatusPedido.MODELAGEM, result.getStatus());
        assertEquals("eq-1", result.getEquipeId());
        assertNotNull(result.getCriadoEm());
        assertNotNull(result.getAtualizadoEm());
    }

    @Test
    void avancarStatusDeveIrParaProximaEtapa() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.MODELAGEM);

        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.avancarStatus("p-1", "user-1", Role.ADMIN);

        assertEquals(StatusPedido.IMPRESSAO, result.getStatus());
    }

    @Test
    void regredirStatusDeveVoltarEtapa() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.regredirStatus("p-1", "user-1", Role.ADMIN);

        assertEquals(StatusPedido.MODELAGEM, result.getStatus());
    }

    @Test
    void avancarParaImpressaoDeveBaixarEstoqueDaEtapa() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.MODELAGEM);

        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        service.avancarStatus("p-1", "user-1", Role.ADMIN);

        verify(estoqueService).baixarPorEtapa("p-1", StatusPedido.IMPRESSAO, "user-1");
    }

    @Test
    void baixaComEstoqueInsuficienteImpedeOSaveDoPedido() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.MODELAGEM);

        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        doThrow(new EstoqueInsuficienteException(List.of(
                new EstoqueInsuficienteException.Falta("Resina Cinza", new BigDecimal("100"), new BigDecimal("50")))))
                .when(estoqueService).baixarPorEtapa("p-1", StatusPedido.IMPRESSAO, "user-1");

        assertThrows(EstoqueInsuficienteException.class, () -> service.avancarStatus("p-1", "user-1", Role.ADMIN));

        verify(repository, never()).save(any());
    }

    @Test
    void regredirDeImpressaoDeveEstornarAEtapaAbandonada() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        service.regredirStatus("p-1", "user-1", Role.ADMIN);

        verify(estoqueService).estornarPorEtapa("p-1", StatusPedido.IMPRESSAO, "user-1");
    }

    @Test
    void clienteNaoPodeMudarEtapaNemCancelar() {
        // a checagem de perfil vem antes de qualquer acesso ao pedido ou ao estoque
        RuntimeException aoAvancar = assertThrows(RuntimeException.class,
                () -> service.avancarStatus("p-1", "user-1", Role.CLIENTE));
        RuntimeException aoRegredir = assertThrows(RuntimeException.class,
                () -> service.regredirStatus("p-1", "user-1", Role.CLIENTE));
        RuntimeException aoCancelar = assertThrows(RuntimeException.class,
                () -> service.cancelar("p-1", "user-1", Role.CLIENTE));

        assertEquals("Cliente não possui permissão para alterar pedidos", aoAvancar.getMessage());
        assertEquals("Cliente não possui permissão para alterar pedidos", aoRegredir.getMessage());
        assertEquals("Cliente não possui permissão para alterar pedidos", aoCancelar.getMessage());
        verify(repository, never()).save(any());
        verifyNoInteractions(estoqueService);
    }

    @Test
    void cancelarDeveDefinirCanceladoSemEstornar() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.cancelar("p-1", "user-1", Role.ADMIN);

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
                        new PoliticaConsumoPintura(), new PoliticaConsumoFinalizado())),
                equipeContexto);
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

        lenient().when(movimentoRepository.findByEquipeIdAndPedidoId("eq-1", "p-1")).thenReturn(List.of(baixaImpressao));
        lenient().when(consumoPedidoRepository.findByPedidoIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(ficha));
        lenient().when(materialRepository.findByIdAndEquipeId("mat1", "eq-1")).thenReturn(Optional.of(resina));

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.cancelar("p-1", "user-1", Role.ADMIN);

        assertEquals(StatusPedido.CANCELADO, result.getStatus());
        // o consumo realizado permanece: saldo segue debitado e nenhum movimento novo é gravado
        assertEquals(new BigDecimal("400"), resina.getSaldo());
        verify(materialRepository, never()).save(any());
        verify(movimentoRepository, never()).save(any());
        // a embalagem da etapa não alcançada nunca é sequer carregada
        verify(materialRepository, never()).findByIdAndEquipeId("emb1", "eq-1");
    }

    @Test
    void pedidoCanceladoNaoAvancaNemRegride() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.CANCELADO);

        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));

        RuntimeException aoAvancar = assertThrows(RuntimeException.class,
                () -> service.avancarStatus("p-1", "user-1", Role.ADMIN));
        RuntimeException aoRegredir = assertThrows(RuntimeException.class,
                () -> service.regredirStatus("p-1", "user-1", Role.ADMIN));

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

        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.atualizar("p-1", "user-1", Role.ADMIN, dados);

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

        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));

        ReflectionTestUtils.setField(service, "gridFsTemplate", gridFsTemplate);

        service.deletar("p-1", "user-1", Role.ADMIN);

        verify(repository).deleteById("p-1");
    }

    // =========================================================
    // ISOLAMENTO POR EQUIPE (SYN-100)
    // =========================================================

    private Pedido pedidoDaEquipe(String id, String equipeId) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setEquipeId(equipeId);
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.MODELAGEM);
        return pedido;
    }

    private User cliente(String id, String equipeId) {
        User cliente = new User();
        cliente.setId(id);
        cliente.setNome("Cliente " + id);
        cliente.setRole(Role.CLIENTE);
        cliente.setEquipeId(equipeId);
        return cliente;
    }

    @Test
    void listarTrazSoPedidosDaEquipeParaFuncionarios() {
        Pedido pedido = pedidoDaEquipe("p-1", "eq-1");
        when(repository.findByEquipeId("eq-1")).thenReturn(List.of(pedido));

        List<Pedido> result = service.listar("user-2", Role.TECNICO);

        assertEquals(List.of(pedido), result);
        verify(repository, never()).findAll();
    }

    @Test
    void listarPorStatusFiltraPelaEquipe() {
        Pedido pedido = pedidoDaEquipe("p-1", "eq-1");
        when(repository.findByEquipeIdAndStatus("eq-1", StatusPedido.MODELAGEM)).thenReturn(List.of(pedido));

        assertEquals(1, service.listarPorStatus("user-1", Role.GERENTE, StatusPedido.MODELAGEM).size());
    }

    @Test
    void usuarioSemEquipeListaVazioENaoCriaPedido() {
        assertTrue(service.listar("semEquipe", Role.GERENTE).isEmpty());
        assertTrue(service.listarPorStatus("semEquipe", Role.GERENTE, StatusPedido.MODELAGEM).isEmpty());
        assertTrue(service.buscarPorId("p-1", "semEquipe", Role.GERENTE).isEmpty());

        Pedido novo = new Pedido();
        novo.setUsuarioId("semEquipe");
        assertThrows(SemEquipeException.class, () -> service.criar(novo));
        assertThrows(SemEquipeException.class, () -> service.avancarStatus("p-1", "semEquipe", Role.GERENTE));
        assertThrows(SemEquipeException.class, () -> service.deletar("p-1", "semEquipe", Role.GERENTE));

        verifyNoInteractions(repository, estoqueService);
    }

    @Test
    void colegaDeEquipeAlteraPedidoCriadoPorOutroIntegrante() {
        Pedido pedido = pedidoDaEquipe("p-1", "eq-1");
        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.avancarStatus("p-1", "user-2", Role.TECNICO);

        assertEquals(StatusPedido.IMPRESSAO, result.getStatus());
    }

    @Test
    void pedidoDeOutraEquipeNaoEEncontrado() {
        // p-1 é da eq-1; user-9 (eq-2) sempre busca na própria equipe
        assertTrue(service.buscarPorId("p-1", "user-9", Role.ADMIN).isEmpty());

        RuntimeException aoAvancar = assertThrows(RuntimeException.class,
                () -> service.avancarStatus("p-1", "user-9", Role.ADMIN));
        RuntimeException aoAtualizar = assertThrows(RuntimeException.class,
                () -> service.atualizar("p-1", "user-9", Role.ADMIN, new Pedido()));
        RuntimeException aoCancelar = assertThrows(RuntimeException.class,
                () -> service.cancelar("p-1", "user-9", Role.ADMIN));
        RuntimeException aoDeletar = assertThrows(RuntimeException.class,
                () -> service.deletar("p-1", "user-9", Role.ADMIN));

        assertEquals("Pedido não encontrado", aoAvancar.getMessage());
        assertEquals("Pedido não encontrado", aoAtualizar.getMessage());
        assertEquals("Pedido não encontrado", aoCancelar.getMessage());
        assertEquals("Pedido não encontrado", aoDeletar.getMessage());
        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
        verifyNoInteractions(estoqueService);
    }

    // =========================================================
    // CLIENTE VINCULADO POR PEDIDOS (SYN-100, rodada 2)
    // =========================================================

    @Test
    void clienteVeOsPropriosPedidosDeDuasEquipes() {
        Pedido daEquipeA = pedidoDaEquipe("p-A", "eq-1");
        daEquipeA.setClienteId("cliente-1");
        Pedido daEquipeB = pedidoDaEquipe("p-B", "eq-2");
        daEquipeB.setClienteId("cliente-1");
        when(repository.findByClienteId("cliente-1")).thenReturn(List.of(daEquipeA, daEquipeB));
        when(repository.findByClienteIdAndStatus("cliente-1", StatusPedido.MODELAGEM))
                .thenReturn(List.of(daEquipeA, daEquipeB));

        assertEquals(2, service.listar("cliente-1", Role.CLIENTE).size());
        assertEquals(2, service.listarPorStatus("cliente-1", Role.CLIENTE, StatusPedido.MODELAGEM).size());
        // cliente não tem equipe e nunca recebe SEM_EQUIPE
        verifyNoInteractions(equipeContexto);
    }

    @Test
    void clienteAbrePedidoProprioDeQualquerEquipeMasNaoODeOutroCliente() {
        Pedido proprio = pedidoDaEquipe("p-B", "eq-2");
        proprio.setClienteId("cliente-1");
        Pedido deOutro = pedidoDaEquipe("p-C", "eq-2");
        deOutro.setClienteId("outro-cliente");
        when(repository.findById("p-B")).thenReturn(Optional.of(proprio));
        when(repository.findById("p-C")).thenReturn(Optional.of(deOutro));

        assertTrue(service.buscarPorId("p-B", "cliente-1", Role.CLIENTE).isPresent());
        assertTrue(service.buscarPorId("p-C", "cliente-1", Role.CLIENTE).isEmpty());
        verifyNoInteractions(equipeContexto);
    }

    @Test
    void equipeBNaoVePedidosDaEquipeADoMesmoCliente() {
        // cliente-1 tem pedido nas duas equipes; a equipe B só enxerga o dela
        Pedido daEquipeB = pedidoDaEquipe("p-B", "eq-2");
        daEquipeB.setClienteId("cliente-1");
        when(repository.findByEquipeId("eq-2")).thenReturn(List.of(daEquipeB));

        assertEquals(List.of(daEquipeB), service.listar("user-9", Role.GERENTE));
        assertTrue(service.buscarPorId("p-A", "user-9", Role.GERENTE).isEmpty());
        verify(repository).findByIdAndEquipeId("p-A", "eq-2");
        verify(repository, never()).findById(any());
        verify(repository, never()).findByClienteId(any());
    }

    @Test
    void vinculaClienteIndependenteDaEquipeDele() {
        // clientes não pertencem a equipe: o equipeId deles é irrelevante
        when(userRepository.findById("cliente-livre")).thenReturn(Optional.of(cliente("cliente-livre", null)));
        when(userRepository.findById("cliente-x")).thenReturn(Optional.of(cliente("cliente-x", "eq-2")));
        when(repository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido semEquipe = new Pedido();
        semEquipe.setUsuarioId("user-1");
        semEquipe.setClienteId("cliente-livre");
        Pedido comEquipeGravada = new Pedido();
        comEquipeGravada.setUsuarioId("user-1");
        comEquipeGravada.setClienteId("cliente-x");

        assertEquals("Cliente cliente-livre", service.criar(semEquipe).getCliente());
        assertEquals("Cliente cliente-x", service.criar(comEquipeGravada).getCliente());
        assertEquals("eq-1", comEquipeGravada.getEquipeId());
    }

    @Test
    void vincularUsuarioQueNaoEClienteOuInexistenteFalha() {
        User tecnico = cliente("tec-1", "eq-1");
        tecnico.setRole(Role.TECNICO);
        when(userRepository.findById("tec-1")).thenReturn(Optional.of(tecnico));

        Pedido comTecnico = new Pedido();
        comTecnico.setUsuarioId("user-1");
        comTecnico.setClienteId("tec-1");
        Pedido comInexistente = new Pedido();
        comInexistente.setUsuarioId("user-1");
        comInexistente.setClienteId("nao-existe");

        assertEquals("O usuário selecionado não possui a role CLIENTE",
                assertThrows(RuntimeException.class, () -> service.criar(comTecnico)).getMessage());
        assertEquals("Cliente não encontrado",
                assertThrows(RuntimeException.class, () -> service.criar(comInexistente)).getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void edicaoTrocaParaClienteDeQualquerEquipe() {
        Pedido pedido = pedidoDaEquipe("p-1", "eq-1");
        Pedido dados = new Pedido();
        dados.setClienteId("cliente-x");
        dados.setProjeto("Projeto B");

        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(userRepository.findById("cliente-x")).thenReturn(Optional.of(cliente("cliente-x", "eq-2")));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.atualizar("p-1", "user-1", Role.GERENTE, dados);

        assertEquals("cliente-x", result.getClienteId());
        assertEquals("Cliente cliente-x", result.getCliente());
        assertEquals("eq-1", result.getEquipeId());
    }

    // =========================================================
    // VALIDAÇÃO ANTES DE GRAVAR ARQUIVOS
    // =========================================================

    @Test
    void prepararParaCriacaoRecusaSemEquipeOuClienteInvalido() {
        Pedido semEquipe = new Pedido();
        semEquipe.setUsuarioId("semEquipe");
        assertThrows(SemEquipeException.class, () -> service.prepararParaCriacao(semEquipe));

        Pedido clienteInvalido = new Pedido();
        clienteInvalido.setUsuarioId("user-1");
        clienteInvalido.setClienteId("nao-existe");
        assertThrows(RuntimeException.class, () -> service.prepararParaCriacao(clienteInvalido));

        verify(repository, never()).save(any());
    }

    @Test
    void validarAtualizacaoRecusaClienteOutraEquipeSemEquipeEClienteInvalido() {
        Pedido dados = new Pedido();

        assertEquals("Cliente não possui permissão para alterar pedidos",
                assertThrows(RuntimeException.class,
                        () -> service.validarAtualizacao("p-1", "cliente-1", Role.CLIENTE, dados)).getMessage());
        assertThrows(SemEquipeException.class,
                () -> service.validarAtualizacao("p-1", "semEquipe", Role.GERENTE, dados));
        assertEquals("Pedido não encontrado",
                assertThrows(RuntimeException.class,
                        () -> service.validarAtualizacao("p-1", "user-9", Role.GERENTE, dados)).getMessage());

        when(repository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedidoDaEquipe("p-1", "eq-1")));
        Pedido comClienteInvalido = new Pedido();
        comClienteInvalido.setClienteId("nao-existe");
        assertEquals("Cliente não encontrado",
                assertThrows(RuntimeException.class,
                        () -> service.validarAtualizacao("p-1", "user-1", Role.GERENTE, comClienteInvalido)).getMessage());

        // pedido sem cliente continua permitido
        service.validarAtualizacao("p-1", "user-1", Role.GERENTE, dados);
        verify(repository, never()).save(any());
    }
}
