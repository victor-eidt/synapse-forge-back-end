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
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.entity.EtapaOrdemPintura;
import synapseforge.crud.infrastructure.entity.OrdemPintura;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.PrioridadeOrdemPintura;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.OrdemPinturaRepository;
import synapseforge.crud.infrastructure.repository.PedidoRepository;

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

    @InjectMocks
    private OrdemPinturaService service;

    @Test
    void listarDeveMapearParaResponse() {
        OrdemPintura ordem = new OrdemPintura();
        ordem.setId("ord-1");
        ordem.setUsuarioId("user-1");
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
        pedido.setUsuarioId("user-1");
        pedido.setProjeto("Projeto X");
        pedido.setCliente("Cliente A");

        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setUsuarioId("user-1");
        cor.setNome("Azul");
        cor.setHex("#0000FF");

        when(repository.findByUsuarioIdOrderByCriadoEmDesc("user-1")).thenReturn(List.of(ordem));
        when(pedidoRepository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(corRepository.findById("c-1")).thenReturn(Optional.of(cor));
        synapseforge.crud.DTO.Pedido.PedidoResponseDTO pedidoResponse =
                mock(synapseforge.crud.DTO.Pedido.PedidoResponseDTO.class);
        when(pedidoResponse.getImagensReferenciaFileIds()).thenReturn(null);
        when(pedidoService.toResponseDTO(pedido)).thenReturn(pedidoResponse);

        List<OrdemPinturaResponseDTO> result = service.listar("user-1");

        assertEquals(1, result.size());
        assertEquals("José", result.get(0).getTecnicoNome());
    }

    @Test
    void criarDeveSalvarComEtapaInicial() {
        OrdemPinturaRequestDTO dto = new OrdemPinturaRequestDTO();
        dto.setPedidoId("p-1");
        dto.setCorId("c-1");
        dto.setTecnico(" José ");
        dto.setPrioridade(PrioridadeOrdemPintura.ALTA);
        dto.setPrazo(LocalDate.now());

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");

        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setUsuarioId("user-1");

        when(pedidoRepository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(corRepository.findById("c-1")).thenReturn(Optional.of(cor));
        synapseforge.crud.DTO.Pedido.PedidoResponseDTO pedidoResponse =
                mock(synapseforge.crud.DTO.Pedido.PedidoResponseDTO.class);
        when(pedidoResponse.getImagensReferenciaFileIds()).thenReturn(List.of());
        when(pedidoService.toResponseDTO(pedido)).thenReturn(pedidoResponse);
        when(repository.save(any(OrdemPintura.class))).thenAnswer(invocation -> {
            OrdemPintura ordem = invocation.getArgument(0);
            ordem.setId("ord-1");
            return ordem;
        });

        OrdemPinturaResponseDTO result = service.criar(dto, "user-1");

        assertEquals(EtapaOrdemPintura.AGUARDANDO, result.getEtapa());
        assertEquals("José", result.getTecnicoNome());
    }

    @Test
    void atualizarEtapaDevePersistirNovaEtapa() {
        OrdemPintura ordem = new OrdemPintura();
        ordem.setId("ord-1");
        ordem.setUsuarioId("user-1");
        ordem.setEtapa(EtapaOrdemPintura.AGUARDANDO);

        when(repository.findById("ord-1")).thenReturn(Optional.of(ordem));
        when(repository.save(ordem)).thenReturn(ordem);

        OrdemPinturaResponseDTO result = service.atualizarEtapa("ord-1", EtapaOrdemPintura.EM_PINTURA, "user-1");

        assertEquals(EtapaOrdemPintura.EM_PINTURA, result.getEtapa());
    }

    @Test
    void atualizarDeveAlterarDados() {
        OrdemPintura ordem = new OrdemPintura();
        ordem.setId("ord-1");
        ordem.setUsuarioId("user-1");
        ordem.setPedidoId("p-1");
        ordem.setCorId("c-1");
        ordem.setTecnico("José");
        ordem.setPrioridade(PrioridadeOrdemPintura.ALTA);
        ordem.setPrazo(LocalDate.now());

        OrdemPinturaRequestDTO dto = new OrdemPinturaRequestDTO();
        dto.setPedidoId("p-1");
        dto.setCorId("c-1");
        dto.setTecnico(" Maria ");
        dto.setPrioridade(PrioridadeOrdemPintura.MEDIA);
        dto.setPrazo(LocalDate.now().plusDays(2));

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");

        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setUsuarioId("user-1");
        cor.setNome("Azul");
        cor.setHex("#0000FF");

        when(repository.findById("ord-1")).thenReturn(Optional.of(ordem));
        when(pedidoRepository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(corRepository.findById("c-1")).thenReturn(Optional.of(cor));
        synapseforge.crud.DTO.Pedido.PedidoResponseDTO pedidoResponse =
                mock(synapseforge.crud.DTO.Pedido.PedidoResponseDTO.class);
        when(pedidoResponse.getImagensReferenciaFileIds()).thenReturn(List.of());
        when(pedidoService.toResponseDTO(pedido)).thenReturn(pedidoResponse);
        when(repository.save(ordem)).thenReturn(ordem);

        OrdemPinturaResponseDTO result = service.atualizar("ord-1", dto, "user-1");

        assertEquals("Maria", result.getTecnicoNome());
        assertEquals(PrioridadeOrdemPintura.MEDIA, result.getPrioridade());
    }

    @Test
    void deletarDeveRemoverOrdem() {
        OrdemPintura ordem = new OrdemPintura();
        ordem.setId("ord-1");
        ordem.setUsuarioId("user-1");

        when(repository.findById("ord-1")).thenReturn(Optional.of(ordem));

        service.deletar("ord-1", "user-1");

        verify(repository).delete(ordem);
    }
}
