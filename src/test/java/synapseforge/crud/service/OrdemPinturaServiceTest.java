package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.OrdemPintura.OrdemPinturaRequestDTO;
import synapseforge.crud.DTO.OrdemPintura.OrdemPinturaResponseDTO;
import synapseforge.crud.DTO.OrdemPintura.TecnicoResumoDTO;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.entity.EtapaOrdemPintura;
import synapseforge.crud.infrastructure.entity.OrdemPintura;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.PrioridadeOrdemPintura;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.OrdemPinturaRepository;
import synapseforge.crud.infrastructure.repository.PedidoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class OrdemPinturaServiceTest {

    @Mock
    private OrdemPinturaRepository repository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private CorRepository corRepository;

    @Mock
    private PedidoService pedidoService;

    @Mock
    private EquipeContexto equipeContexto;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrdemPinturaService service;

    private void naEquipe(String usuarioId, String equipeId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.of(equipeId));
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenReturn(equipeId);
    }

    private void semEquipe(String usuarioId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.empty());
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenThrow(new SemEquipeException());
    }

    private OrdemPinturaRequestDTO dto(String tecnicoId) {
        OrdemPinturaRequestDTO dto = new OrdemPinturaRequestDTO();
        dto.setPedidoId("p-1");
        dto.setCorId("c-1");
        dto.setTecnicoId(tecnicoId);
        dto.setPrioridade(PrioridadeOrdemPintura.ALTA);
        dto.setPrazo(LocalDate.now());
        return dto;
    }

    private User usuario(String id, String nome, Role role, String equipeId, boolean ativo) {
        User u = new User();
        u.setId(id);
        u.setNome(nome);
        u.setRole(role);
        u.setEquipeId(equipeId);
        u.setAtivo(ativo);
        return u;
    }

    // Técnico cadastrado e ativo na equipe informada
    private User tecnicoNoBanco(String id, String nome, String equipeId) {
        User u = usuario(id, nome, Role.TECNICO, equipeId, true);
        lenient().when(userRepository.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    private void pedidoResponseSemImagens(Pedido pedido) {
        synapseforge.crud.DTO.Pedido.PedidoResponseDTO pedidoResponse =
                mock(synapseforge.crud.DTO.Pedido.PedidoResponseDTO.class);
        when(pedidoResponse.getImagensReferenciaFileIds()).thenReturn(List.of());
        when(pedidoService.toResponseDTO(pedido)).thenReturn(pedidoResponse);
    }

    @Test
    void listarDeveMapearParaResponse() {
        naEquipe("user-1", "eq-1");
        OrdemPintura ordem = new OrdemPintura();
        ordem.setId("ord-1");
        ordem.setEquipeId("eq-1");
        ordem.setUsuarioId("user-2");
        ordem.setPedidoId("p-1");
        ordem.setCorId("c-1");
        ordem.setTecnico("José");
        ordem.setPrioridade(PrioridadeOrdemPintura.ALTA);
        ordem.setPrazo(LocalDate.now());
        ordem.setEtapa(EtapaOrdemPintura.AGUARDANDO);
        ordem.setCriadoEm(LocalDateTime.now());
        ordem.setAtualizadoEm(LocalDateTime.now());

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setProjeto("Projeto X");
        pedido.setCliente("Cliente A");

        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setNome("Azul");
        cor.setHex("#0000FF");

        when(repository.findByEquipeIdOrderByCriadoEmDesc("eq-1")).thenReturn(List.of(ordem));
        when(pedidoRepository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(corRepository.findByIdAndEquipeId("c-1", "eq-1")).thenReturn(Optional.of(cor));
        synapseforge.crud.DTO.Pedido.PedidoResponseDTO pedidoResponse =
                mock(synapseforge.crud.DTO.Pedido.PedidoResponseDTO.class);
        when(pedidoResponse.getImagensReferenciaFileIds()).thenReturn(null);
        when(pedidoService.toResponseDTO(pedido)).thenReturn(pedidoResponse);

        List<OrdemPinturaResponseDTO> result = service.listar("user-1");

        assertEquals(1, result.size());
        assertEquals("José", result.get(0).getTecnicoNome());
        assertEquals("Projeto X", result.get(0).getPedidoProjeto());
    }

    @Test
    void listarSemEquipeDeveRetornarVazio() {
        semEquipe("user-1");

        assertTrue(service.listar("user-1").isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void criarDeveSalvarNaEquipeComEtapaInicial() {
        naEquipe("user-1", "eq-1");
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        // pedido criado por outro integrante da mesma equipe também vale
        pedido.setUsuarioId("user-2");

        Cor cor = new Cor();
        cor.setId("c-1");

        when(pedidoRepository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(corRepository.findByIdAndEquipeId("c-1", "eq-1")).thenReturn(Optional.of(cor));
        pedidoResponseSemImagens(pedido);
        when(repository.save(any(OrdemPintura.class))).thenAnswer(invocation -> {
            OrdemPintura ordem = invocation.getArgument(0);
            ordem.setId("ord-1");
            return ordem;
        });

        tecnicoNoBanco("tec-1", "José", "eq-1");

        OrdemPinturaResponseDTO result = service.criar(dto("tec-1"), "user-1");

        assertEquals(EtapaOrdemPintura.AGUARDANDO, result.getEtapa());
        assertEquals("tec-1", result.getTecnicoId());
        assertEquals("José", result.getTecnicoNome());
        verify(repository).save(argThat(o -> "eq-1".equals(o.getEquipeId())
                && "user-1".equals(o.getUsuarioId())
                && "tec-1".equals(o.getTecnicoId())
                && "José".equals(o.getTecnico())));
    }

    @Test
    void criarComPedidoOuCorDeOutraEquipeDeveFalhar() {
        naEquipe("user-1", "eq-1");

        RuntimeException semPedido = assertThrows(RuntimeException.class,
                () -> service.criar(dto("tec-1"), "user-1"));
        assertEquals("Pedido nao encontrado", semPedido.getMessage());

        when(pedidoRepository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(new Pedido()));
        RuntimeException semCor = assertThrows(RuntimeException.class,
                () -> service.criar(dto("tec-1"), "user-1"));
        assertEquals("Cor nao encontrada", semCor.getMessage());

        verify(repository, never()).save(any());
    }

    @Test
    void criarSemEquipeDeveSerRecusado() {
        semEquipe("user-1");

        assertThrows(SemEquipeException.class, () -> service.criar(dto("tec-1"), "user-1"));
        verifyNoInteractions(repository, pedidoRepository, corRepository);
    }

    @Test
    void atualizarEtapaDevePersistirNovaEtapa() {
        naEquipe("user-1", "eq-1");
        OrdemPintura ordem = new OrdemPintura();
        ordem.setId("ord-1");
        ordem.setEquipeId("eq-1");
        ordem.setEtapa(EtapaOrdemPintura.AGUARDANDO);

        when(repository.findByIdAndEquipeId("ord-1", "eq-1")).thenReturn(Optional.of(ordem));
        when(repository.save(ordem)).thenReturn(ordem);

        OrdemPinturaResponseDTO result = service.atualizarEtapa("ord-1", EtapaOrdemPintura.EM_PINTURA, "user-1");

        assertEquals(EtapaOrdemPintura.EM_PINTURA, result.getEtapa());
    }

    @Test
    void atualizarDeveAlterarDados() {
        naEquipe("user-1", "eq-1");
        OrdemPintura ordem = new OrdemPintura();
        ordem.setId("ord-1");
        ordem.setEquipeId("eq-1");
        ordem.setPedidoId("p-1");
        ordem.setCorId("c-1");
        ordem.setTecnicoId("tec-1");
        ordem.setTecnico("José");
        ordem.setPrioridade(PrioridadeOrdemPintura.ALTA);
        ordem.setPrazo(LocalDate.now());
        tecnicoNoBanco("tec-2", "Maria", "eq-1");

        OrdemPinturaRequestDTO dto = dto("tec-2");
        dto.setPrioridade(PrioridadeOrdemPintura.MEDIA);
        dto.setPrazo(LocalDate.now().plusDays(2));

        Pedido pedido = new Pedido();
        pedido.setId("p-1");

        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setNome("Azul");
        cor.setHex("#0000FF");

        when(repository.findByIdAndEquipeId("ord-1", "eq-1")).thenReturn(Optional.of(ordem));
        when(pedidoRepository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(corRepository.findByIdAndEquipeId("c-1", "eq-1")).thenReturn(Optional.of(cor));
        pedidoResponseSemImagens(pedido);
        when(repository.save(ordem)).thenReturn(ordem);

        OrdemPinturaResponseDTO result = service.atualizar("ord-1", dto, "user-1");

        assertEquals("tec-2", result.getTecnicoId());
        assertEquals("Maria", result.getTecnicoNome());
        assertEquals(PrioridadeOrdemPintura.MEDIA, result.getPrioridade());
    }

    @Test
    void criarComTecnicoInvalidoDeveFalhar() {
        naEquipe("user-1", "eq-1");
        when(pedidoRepository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(new Pedido()));
        when(corRepository.findByIdAndEquipeId("c-1", "eq-1")).thenReturn(Optional.of(new Cor()));

        // não existe no banco
        when(userRepository.findById("fantasma")).thenReturn(Optional.empty());
        // técnico de outra equipe
        tecnicoNoBanco("tec-9", "Outro", "eq-2");
        // da equipe, mas não é técnico
        when(userRepository.findById("cli-1")).thenReturn(Optional.of(usuario("cli-1", "Cliente", Role.CLIENTE, "eq-1", true)));
        // técnico desativado
        when(userRepository.findById("tec-off")).thenReturn(Optional.of(usuario("tec-off", "Inativo", Role.TECNICO, "eq-1", false)));

        for (String id : List.of("fantasma", "tec-9", "cli-1", "tec-off")) {
            RuntimeException erro = assertThrows(RuntimeException.class, () -> service.criar(dto(id), "user-1"));
            assertEquals("Tecnico nao encontrado na equipe", erro.getMessage());
        }
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarMantendoOMesmoTecnicoNaoRevalida() {
        naEquipe("user-1", "eq-1");
        OrdemPintura ordem = new OrdemPintura();
        ordem.setId("ord-1");
        ordem.setEquipeId("eq-1");
        ordem.setPedidoId("p-1");
        ordem.setCorId("c-1");
        ordem.setTecnicoId("tec-saiu");
        ordem.setTecnico("Ex-integrante");

        Pedido pedido = new Pedido();
        when(repository.findByIdAndEquipeId("ord-1", "eq-1")).thenReturn(Optional.of(ordem));
        when(pedidoRepository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(pedido));
        when(corRepository.findByIdAndEquipeId("c-1", "eq-1")).thenReturn(Optional.of(new Cor()));
        pedidoResponseSemImagens(pedido);
        when(repository.save(ordem)).thenReturn(ordem);
        // saiu da equipe: não seria aceito como técnico novo, mas continua na ordem
        when(userRepository.findById("tec-saiu")).thenReturn(Optional.of(usuario("tec-saiu", "Ex-integrante", Role.CLIENTE, null, true)));

        OrdemPinturaResponseDTO result = service.atualizar("ord-1", dto("tec-saiu"), "user-1");

        assertEquals("tec-saiu", result.getTecnicoId());
        assertEquals("Ex-integrante", result.getTecnicoNome());
    }

    @Test
    void listarTecnicosDeveTrazerSoTecnicosAtivosDaEquipeEmOrdemAlfabetica() {
        naEquipe("user-1", "eq-1");
        when(userRepository.findByEquipeId("eq-1")).thenReturn(List.of(
                usuario("t-2", "Úrsula", Role.TECNICO, "eq-1", true),
                usuario("g-1", "Gerente", Role.GERENTE, "eq-1", true),
                usuario("t-1", "Ana", Role.TECNICO, "eq-1", true),
                usuario("t-3", "Bruno", Role.TECNICO, "eq-1", false)
        ));

        List<TecnicoResumoDTO> tecnicos = service.listarTecnicos("user-1");

        assertEquals(List.of("Ana", "Úrsula"), tecnicos.stream().map(TecnicoResumoDTO::getNome).toList());
        assertEquals("t-1", tecnicos.get(0).getId());
    }

    @Test
    void listarTecnicosSemEquipeDeveRetornarVazio() {
        semEquipe("user-1");

        assertTrue(service.listarTecnicos("user-1").isEmpty());
        verifyNoInteractions(userRepository);
    }

    @Test
    void ordemAntigaSemTecnicoIdMostraONomeGravado() {
        naEquipe("user-1", "eq-1");
        OrdemPintura ordem = new OrdemPintura();
        ordem.setId("ord-1");
        ordem.setEquipeId("eq-1");
        ordem.setPedidoId("p-1");
        ordem.setCorId("c-1");
        ordem.setTecnico("Nome digitado antigamente");

        when(repository.findByEquipeIdOrderByCriadoEmDesc("eq-1")).thenReturn(List.of(ordem));

        OrdemPinturaResponseDTO result = service.listar("user-1").get(0);

        assertNull(result.getTecnicoId());
        assertEquals("Nome digitado antigamente", result.getTecnicoNome());
        verifyNoInteractions(userRepository);
    }

    @Test
    void ordemDeOutraEquipeNaoEEncontrada() {
        naEquipe("user-9", "eq-2");

        RuntimeException aoMoverEtapa = assertThrows(RuntimeException.class,
                () -> service.atualizarEtapa("ord-1", EtapaOrdemPintura.EM_PINTURA, "user-9"));
        RuntimeException aoAtualizar = assertThrows(RuntimeException.class,
                () -> service.atualizar("ord-1", dto("tec-1"), "user-9"));
        RuntimeException aoDeletar = assertThrows(RuntimeException.class,
                () -> service.deletar("ord-1", "user-9"));

        assertEquals("Ordem de pintura nao encontrada", aoMoverEtapa.getMessage());
        assertEquals("Ordem de pintura nao encontrada", aoAtualizar.getMessage());
        assertEquals("Ordem de pintura nao encontrada", aoDeletar.getMessage());
        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void deletarDeveRemoverOrdem() {
        naEquipe("user-1", "eq-1");
        OrdemPintura ordem = new OrdemPintura();
        ordem.setId("ord-1");
        ordem.setEquipeId("eq-1");

        when(repository.findByIdAndEquipeId("ord-1", "eq-1")).thenReturn(Optional.of(ordem));

        service.deletar("ord-1", "user-1");

        verify(repository).delete(ordem);
    }
}
