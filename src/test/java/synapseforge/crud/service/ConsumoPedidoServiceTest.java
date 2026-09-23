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
import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoRequestDTO;
import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoResponseDTO;
import synapseforge.crud.DTO.ConsumoPedido.ItemConsumoRequestDTO;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.infrastructure.entity.ConsumoPedido;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;
import synapseforge.crud.infrastructure.repository.ConsumoPedidoRepository;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.MaterialRepository;
import synapseforge.crud.infrastructure.repository.PedidoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ConsumoPedidoServiceTest {

    @Mock
    private ConsumoPedidoRepository repository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private CorRepository corRepository;

    @Mock
    private EquipeContexto equipeContexto;

    @InjectMocks
    private ConsumoPedidoService service;

    @BeforeEach
    void setUp() {
        lenient().when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.of("eq-1"));
        lenient().when(equipeContexto.equipeObrigatoria("user-1")).thenReturn("eq-1");
        lenient().when(equipeContexto.equipeDe("user-9")).thenReturn(Optional.of("eq-2"));
        lenient().when(equipeContexto.equipeObrigatoria("user-9")).thenReturn("eq-2");
        lenient().when(equipeContexto.equipeObrigatoria("semEquipe")).thenThrow(new SemEquipeException());
    }

    private ConsumoPedidoRequestDTO ficha() {
        ItemConsumoRequestDTO item = new ItemConsumoRequestDTO();
        item.setTipoInsumo(TipoInsumo.MATERIAL);
        item.setInsumoId("mat1");
        item.setQuantidade(new BigDecimal("100"));
        item.setUnidade(UnidadeMedida.G);
        item.setEtapaConsumo(StatusPedido.IMPRESSAO);

        ConsumoPedidoRequestDTO dto = new ConsumoPedidoRequestDTO();
        dto.setPedidoId("p-1");
        dto.setItens(List.of(item));
        return dto;
    }

    @Test
    void salvarGravaAFichaNaEquipeDoPedido() {
        when(pedidoRepository.findByIdAndEquipeId("p-1", "eq-1")).thenReturn(Optional.of(new Pedido()));
        when(materialRepository.findByIdAndEquipeId("mat1", "eq-1")).thenReturn(Optional.of(new Material()));
        when(repository.save(any(ConsumoPedido.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsumoPedidoResponseDTO result = service.salvar(ficha(), "user-1");

        assertEquals("p-1", result.getPedidoId());
        verify(repository).save(argThat(c -> "eq-1".equals(c.getEquipeId())));
    }

    @Test
    void pedidoOuInsumoDeOutraEquipeNaoSaoEncontrados() {
        RuntimeException semPedido = assertThrows(RuntimeException.class, () -> service.salvar(ficha(), "user-9"));
        assertEquals("Pedido não encontrado", semPedido.getMessage());

        when(pedidoRepository.findByIdAndEquipeId("p-1", "eq-2")).thenReturn(Optional.of(new Pedido()));
        RuntimeException semMaterial = assertThrows(RuntimeException.class, () -> service.salvar(ficha(), "user-9"));
        assertEquals("Material não encontrado", semMaterial.getMessage());

        RuntimeException leitura = assertThrows(RuntimeException.class,
                () -> service.buscarPorPedido("p-1", "user-9"));
        assertEquals("Ficha de consumo não encontrada para o pedido", leitura.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void salvarSemEquipeDeveSerRecusado() {
        assertThrows(SemEquipeException.class, () -> service.salvar(ficha(), "semEquipe"));
        verifyNoInteractions(repository, pedidoRepository);
    }
}
