package synapseforge.crud.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DashboardMockSeederTest {

    private static final String EQUIPE = "equipe-teste-synapse";

    // mesmo padrão do JSON Schema de pedidos para os campos BigDecimal (gravados como texto)
    private static final String PADRAO_DECIMAL = "^[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?$";

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardMockSeeder seeder;

    @Test
    void seed_criaMateriaisEPedidosDaEquipeCobrindoTodosOsStatus() {
        User alice = new User();
        alice.setId("u-alice");
        alice.setNome("Alice Silva");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice.silva@teste.com")).thenReturn(Optional.of(alice));

        seeder.seed(EQUIPE, "u-gerente");

        List<Material> materiais = capturar(Material.class);
        List<Pedido> pedidos = capturar(Pedido.class);

        assertEquals(6, materiais.size());
        assertEquals(24, pedidos.size());
        assertTrue(materiais.stream().allMatch(m -> EQUIPE.equals(m.getEquipeId())));

        // o $lookup das métricas converte materialId para ObjectId: precisa ser hex válido
        Set<String> idsMateriais = materiais.stream().map(Material::getId).collect(Collectors.toSet());
        assertTrue(idsMateriais.stream().allMatch(ObjectId::isValid));
        assertTrue(pedidos.stream().allMatch(p -> idsMateriais.contains(p.getMaterialId())));

        assertEquals(EnumSet.allOf(StatusPedido.class),
                pedidos.stream().map(Pedido::getStatus).collect(Collectors.toCollection(() -> EnumSet.noneOf(StatusPedido.class))));
        assertTrue(pedidos.stream().anyMatch(p -> p.getPrazo().isBefore(LocalDate.now())
                && p.getStatus() != StatusPedido.FINALIZADO && p.getStatus() != StatusPedido.CANCELADO));

        for (Pedido pedido : pedidos) {
            assertTrue(ObjectId.isValid(pedido.getId()));
            assertEquals(EQUIPE, pedido.getEquipeId());
            assertEquals("u-gerente", pedido.getUsuarioId());
            assertNotNull(pedido.getCriadoEm());
            assertTrue(pedido.getPrecoFinal().toString().matches(PADRAO_DECIMAL));
            assertTrue(pedido.getPrecoFinal().compareTo(pedido.getCustoTotal()) > 0);
        }

        // cliente que não existe fica sem vínculo em vez de quebrar a carga
        assertTrue(pedidos.stream().anyMatch(p -> "u-alice".equals(p.getClienteId())
                && "Alice Silva".equals(p.getCliente())));
        assertTrue(pedidos.stream().noneMatch(p -> p.getCliente() == null && p.getClienteId() != null));
        // todo pedido tem nome de cliente, mesmo sem vínculo (o campo é @NotBlank no PedidoRequestDTO)
        assertTrue(pedidos.stream().allMatch(p -> p.getCliente() != null && !p.getCliente().isBlank()));
        assertTrue(pedidos.stream().anyMatch(p -> p.getClienteId() == null && "Cliente balcão".equals(p.getCliente())));
    }

    @Test
    void seed_naoSobrescreveDocumentosQueJaExistem() {
        when(mongoTemplate.findById(anyString(), any())).thenReturn(new Object());

        seeder.seed(EQUIPE, "u-gerente");

        verify(mongoTemplate, never()).insert(any(Object.class));
    }

    @Test
    void seed_semGerenteNaoFazNada() {
        seeder.seed(EQUIPE, null);

        verifyNoInteractions(mongoTemplate, userRepository);
    }

    private <T> List<T> capturar(Class<T> tipo) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(mongoTemplate, atLeastOnce()).insert(captor.capture());
        verify(mongoTemplate, atLeastOnce()).findById(anyString(), eq(tipo));
        return captor.getAllValues().stream().filter(tipo::isInstance).map(tipo::cast).toList();
    }
}
