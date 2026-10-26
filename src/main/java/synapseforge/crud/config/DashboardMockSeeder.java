package synapseforge.crud.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static synapseforge.crud.infrastructure.entity.StatusPedido.ACABAMENTO;
import static synapseforge.crud.infrastructure.entity.StatusPedido.CANCELADO;
import static synapseforge.crud.infrastructure.entity.StatusPedido.FINALIZADO;
import static synapseforge.crud.infrastructure.entity.StatusPedido.IMPRESSAO;
import static synapseforge.crud.infrastructure.entity.StatusPedido.MODELAGEM;
import static synapseforge.crud.infrastructure.entity.StatusPedido.PINTURA;

/**
 * SYN-164: materiais e pedidos fictícios na equipe de teste para o dashboard de pedidos
 * (GET /pedidos/metricas/por-status e /materiais-mais-usados) ter o que mostrar.
 *
 * Chamado pelo DataInitializer, então só roda com app.seed.enabled=true; os mocks podem ser
 * desligados sozinhos com app.seed.dashboard-mocks=false. Ids fixos tornam a carga idempotente:
 * documento que já existe (mesmo editado à mão) não é sobrescrito.
 */
@Component
public class DashboardMockSeeder {

    // ObjectIds válidos: o $lookup das métricas converte Pedido.materialId para ObjectId
    private static final String PREFIXO_ID_MOCK = "5eed0164";

    private static final BigDecimal CUSTO_MAQUINA_HORA = new BigDecimal("8.00");
    private static final BigDecimal CUSTO_MAO_DE_OBRA_HORA = new BigDecimal("35.00");
    private static final BigDecimal MARGEM_LUCRO = new BigDecimal("0.40");

    private static final String[] EMAILS_CLIENTES = {
            "alice.silva@teste.com",
            "antonio.santos@teste.com"
    };

    private record MaterialMock(String nome, String tipo, double densidadeGcm3,
                                String precoPorGrama, String saldoG, String estoqueMinimoG) {
    }

    // cliente: índice em EMAILS_CLIENTES, -1 = sem cliente vinculado
    // prazoDias: relativo a hoje (negativo = vencido, conta como atrasado se ainda em produção)
    private record PedidoMock(String projeto, int cliente, int material, StatusPedido status,
                              int criadoDiasAtras, int prazoDias, double volumeCm3,
                              double horasImpressao, double horasMaoDeObra) {
    }

    private static final List<MaterialMock> MATERIAIS = List.of(
            new MaterialMock("PLA Branco", "PLA", 1.24, "0.12", "4200", "1000"),
            new MaterialMock("PLA Preto", "PLA", 1.24, "0.12", "2600", "1000"),
            new MaterialMock("PETG Transparente", "PETG", 1.27, "0.15", "1800", "800"),
            new MaterialMock("Resina Standard Cinza", "Resina", 1.10, "0.35", "650", "500"),
            new MaterialMock("ABS Vermelho", "ABS", 1.04, "0.14", "900", "800"),
            new MaterialMock("TPU Flexível Preto", "TPU", 1.21, "0.22", "300", "500")
    );

    // Uso decrescente por material (PLA Branco > PLA Preto > PETG > Resina > ABS > TPU) e todos
    // os status representados, com alguns pedidos atrasados nas etapas de produção
    private static final List<PedidoMock> PEDIDOS = List.of(
            new PedidoMock("Miniatura de dragão", 0, 3, FINALIZADO, 88, -70, 42.0, 9.5, 3.0),
            new PedidoMock("Suporte de monitor", 1, 0, FINALIZADO, 80, -66, 180.0, 14.0, 1.0),
            new PedidoMock("Engrenagens de reposição", -1, 4, FINALIZADO, 74, -60, 35.0, 4.0, 0.5),
            new PedidoMock("Vaso geométrico", 0, 0, FINALIZADO, 65, -50, 210.0, 16.0, 1.5),
            new PedidoMock("Maquete residencial", 1, 1, FINALIZADO, 58, -40, 320.0, 26.0, 6.0),
            new PedidoMock("Case para Raspberry Pi", -1, 2, FINALIZADO, 45, -30, 95.0, 7.0, 0.5),
            new PedidoMock("Busto personalizado", 0, 3, FINALIZADO, 38, -20, 60.0, 12.0, 4.0),
            new PedidoMock("Chaveiros da empresa", 1, 0, CANCELADO, 52, -35, 40.0, 3.0, 0.5),
            new PedidoMock("Peça de drone", -1, 5, CANCELADO, 30, -10, 25.0, 3.5, 0.5),
            new PedidoMock("Troféu do campeonato", 0, 0, ACABAMENTO, 26, -3, 150.0, 13.0, 3.0),
            new PedidoMock("Luminária lua", 1, 0, ACABAMENTO, 20, 4, 130.0, 11.0, 2.0),
            new PedidoMock("Cosplay: elmo", 0, 1, ACABAMENTO, 18, 7, 450.0, 32.0, 8.0),
            new PedidoMock("Figura de anime", 1, 3, PINTURA, 22, -5, 55.0, 10.0, 5.0),
            new PedidoMock("Placa de sinalização", -1, 1, PINTURA, 15, 6, 120.0, 9.0, 2.0),
            new PedidoMock("Peças de tabuleiro", 0, 0, PINTURA, 12, 10, 80.0, 8.0, 3.0),
            new PedidoMock("Protótipo de embalagem", 1, 2, IMPRESSAO, 16, -2, 140.0, 10.0, 1.0),
            new PedidoMock("Organizador de mesa", -1, 0, IMPRESSAO, 10, 5, 190.0, 15.0, 0.5),
            new PedidoMock("Suporte de headset", 0, 1, IMPRESSAO, 8, 8, 110.0, 9.0, 0.5),
            new PedidoMock("Carcaça de controle", 1, 4, IMPRESSAO, 6, 12, 75.0, 6.0, 1.0),
            new PedidoMock("Peças de robô", -1, 2, IMPRESSAO, 5, 14, 65.0, 5.5, 1.0),
            new PedidoMock("Logo 3D da loja", 0, 0, MODELAGEM, 9, -1, 100.0, 8.0, 2.0),
            new PedidoMock("Prótese de mão", 1, 1, MODELAGEM, 4, 15, 210.0, 18.0, 6.0),
            new PedidoMock("Kit de miniaturas RPG", 0, 3, MODELAGEM, 3, 20, 48.0, 11.0, 4.0),
            new PedidoMock("Porta-canetas", -1, 2, MODELAGEM, 1, 21, 70.0, 6.0, 0.5)
    );

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserRepository userRepository;

    public void seed(String equipeId, String usuarioId) {

        if (usuarioId == null) {
            System.out.println("Mocks do dashboard ignorados: gerente de teste não encontrado");
            return;
        }

        int materiaisCriados = 0;

        for (int i = 0; i < MATERIAIS.size(); i++) {

            if (salvarSeNaoExiste(novoMaterial(equipeId, i), idMaterial(i))) {
                materiaisCriados++;
            }
        }

        List<User> clientes = Arrays.stream(EMAILS_CLIENTES)
                .map(email -> userRepository.findByEmail(email).orElse(null))
                .toList();

        int pedidosCriados = 0;

        for (int i = 0; i < PEDIDOS.size(); i++) {

            Pedido pedido = novoPedido(equipeId, usuarioId, clientes, PEDIDOS.get(i), i);

            if (salvarSeNaoExiste(pedido, pedido.getId())) {
                pedidosCriados++;
            }
        }

        System.out.println(
                "Mocks do dashboard: "
                        + materiaisCriados
                        + " materiais e "
                        + pedidosCriados
                        + " pedidos criados na equipe de teste"
        );
    }

    private boolean salvarSeNaoExiste(Object documento, String id) {

        if (mongoTemplate.findById(id, documento.getClass()) != null) {
            return false;
        }

        mongoTemplate.insert(documento);
        return true;
    }

    private Material novoMaterial(String equipeId, int indice) {

        MaterialMock mock = MATERIAIS.get(indice);

        Material material = new Material();

        material.setId(idMaterial(indice));
        material.setEquipeId(equipeId);
        material.setNome(mock.nome());
        material.setTipo(mock.tipo());
        material.setDensidadeGcm3(mock.densidadeGcm3());
        material.setPrecoPorGrama(new BigDecimal(mock.precoPorGrama()));
        material.setAtivo(true);
        material.setUnidade(UnidadeMedida.G);
        material.setSaldo(new BigDecimal(mock.saldoG()));
        material.setEstoqueMinimo(new BigDecimal(mock.estoqueMinimoG()));

        return material;
    }

    private Pedido novoPedido(
            String equipeId,
            String usuarioId,
            List<User> clientes,
            PedidoMock mock,
            int indice
    ) {

        MaterialMock material = MATERIAIS.get(mock.material());
        LocalDateTime criadoEm = LocalDateTime.now().minusDays(mock.criadoDiasAtras());

        // mesma conta do orçamento: material por grama + máquina e mão de obra por hora + margem
        BigDecimal custoMaterial = BigDecimal.valueOf(mock.volumeCm3() * material.densidadeGcm3())
                .multiply(new BigDecimal(material.precoPorGrama()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal custoMaquina = CUSTO_MAQUINA_HORA
                .multiply(BigDecimal.valueOf(mock.horasImpressao()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal custoMaoDeObra = CUSTO_MAO_DE_OBRA_HORA
                .multiply(BigDecimal.valueOf(mock.horasMaoDeObra()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal custoTotal = custoMaterial.add(custoMaquina).add(custoMaoDeObra);
        BigDecimal precoFinal = custoTotal
                .multiply(BigDecimal.ONE.add(MARGEM_LUCRO))
                .setScale(2, RoundingMode.HALF_UP);

        Pedido pedido = new Pedido();

        pedido.setId(PREFIXO_ID_MOCK + String.format("%016x", 1000 + indice));
        pedido.setEquipeId(equipeId);
        pedido.setUsuarioId(usuarioId);

        User cliente = mock.cliente() >= 0 ? clientes.get(mock.cliente()) : null;
        if (cliente != null) {
            pedido.setClienteId(cliente.getId());
            pedido.setCliente(cliente.getNome());
        }

        pedido.setProjeto(mock.projeto());
        pedido.setDescricao("Pedido de exemplo para o dashboard (dados mock)");
        pedido.setMaterialId(idMaterial(mock.material()));
        pedido.setVolumeCm3(mock.volumeCm3());
        pedido.setTempoImpressaoHoras(mock.horasImpressao());
        pedido.setTempoMaoDeObraHoras(mock.horasMaoDeObra());
        pedido.setCustoMaquinaHora(CUSTO_MAQUINA_HORA);
        pedido.setCustoMaoDeObraHora(CUSTO_MAO_DE_OBRA_HORA);
        pedido.setMargemLucro(MARGEM_LUCRO);
        pedido.setCustoMaterial(custoMaterial);
        pedido.setCustoMaquina(custoMaquina);
        pedido.setCustoMaoDeObra(custoMaoDeObra);
        pedido.setCustoTotal(custoTotal);
        pedido.setPrecoFinal(precoFinal);
        pedido.setStatus(mock.status());
        pedido.setPrazo(LocalDate.now().plusDays(mock.prazoDias()));
        pedido.setCriadoEm(criadoEm);
        pedido.setAtualizadoEm(criadoEm);

        return pedido;
    }

    private static String idMaterial(int indice) {
        return PREFIXO_ID_MOCK + String.format("%016x", indice + 1);
    }
}
