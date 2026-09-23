package synapseforge.crud.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import synapseforge.crud.infrastructure.entity.Acabamento;
import synapseforge.crud.infrastructure.entity.ConsumoPedido;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.entity.Evento;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.Mistura;
import synapseforge.crud.infrastructure.entity.MovimentoEstoque;
import synapseforge.crud.infrastructure.entity.Orcamento;
import synapseforge.crud.infrastructure.entity.OrdemPintura;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.EquipeRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final String EQUIPE_TESTE_ID = "equipe-teste-synapse";
    private static final String EQUIPE_TESTE_NOME = "Equipe Teste";
    private static final String EMAIL_GERENTE_TESTE = "gerente@teste.com";

    // Coleções de negócio isoladas por equipe (SYN-100)
    private static final List<Class<?>> ENTIDADES_DA_EQUIPE = List.of(
            Pedido.class,
            Orcamento.class,
            Cor.class,
            Mistura.class,
            OrdemPintura.class,
            Evento.class,
            Material.class,
            MovimentoEstoque.class,
            ConsumoPedido.class
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CorRepository corRepository;

    @Autowired
    private EquipeRepository equipeRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        migrarUsuariosExistentes();
        seedTestUsers();
        seedTestEquipe();
        migrarDadosSemEquipe();
        seedTestCores();
    }

    // Sets emailConfirmado=true for users created before the email confirmation feature
    private void migrarUsuariosExistentes() {
        List<User> naoConfirmados =
                userRepository.findByEmailConfirmadoFalseAndEmailConfirmTokenIsNull();

        for (User user : naoConfirmados) {
            user.setEmailConfirmado(true);
            userRepository.save(user);

            System.out.println(
                    "Migrado (emailConfirmado=true): "
                            + user.getEmail()
            );
        }
    }

    private void seedTestUsers() {

        List<User> testUsers = Arrays.asList(

                // CLIENTES
                createTestUser(
                        "Alice Silva",
                        "alice.silva@teste.com",
                        Role.CLIENTE
                ),

                createTestUser(
                        "Antonio Santos",
                        "antonio.santos@teste.com",
                        Role.CLIENTE
                ),

                // FUNCIONÁRIOS
                createTestUser(
                        "Funcionario Teste",
                        "funcionario@teste.com",
                        Role.TECNICO
                ),

                createTestUser(
                        "Funcionario Teste 2",
                        "funcionario2@teste.com",
                        Role.TECNICO
                ),

                createTestUser(
                        "Funcionario Teste 3",
                        "funcionario3@teste.com",
                        Role.TECNICO
                ),

                // GERENTE
                createTestUser(
                        "Gerente Teste",
                        "gerente@teste.com",
                        Role.GERENTE
                )
        );

        for (User user : testUsers) {

            if (!userRepository.findByEmail(user.getEmail()).isPresent()) {

                userRepository.save(user);

                System.out.println(
                        "Usuário de teste criado: "
                                + user.getNome()
                                + " | Role: "
                                + user.getRole()
                );

            } else {

                System.out.println(
                        "Usuário de teste já existe: "
                                + user.getNome()
                                + " | Role: "
                                + user.getRole()
                );
            }
        }
    }

    private User createTestUser(
            String nome,
            String email,
            Role role
    ) {

        User user = new User();

        user.setNome(nome);
        user.setEmail(email);
        user.setSenha(passwordEncoder.encode("1234"));
        user.setRole(role);
        user.setAtivo(true);
        user.setEmailConfirmado(true);
        user.setCriadoEm(LocalDateTime.now());

        return user;
    }

    private void seedTestEquipe() {

        garantirDocumentoEquipeTeste();

        List<String> emailsEquipeTeste = Arrays.asList(
                "funcionario@teste.com",
                "funcionario2@teste.com",
                "funcionario3@teste.com",
                "gerente@teste.com"
        );

        for (String email : emailsEquipeTeste) {

            userRepository.findByEmail(email)
                    .ifPresent(user -> {

                        if (!EQUIPE_TESTE_ID.equals(user.getEquipeId())) {

                            user.setEquipeId(EQUIPE_TESTE_ID);
                            user.setAtualizadoEm(LocalDateTime.now());

                            userRepository.save(user);

                            System.out.println(
                                    "Usuário adicionado à equipe de teste: "
                                            + user.getNome()
                                            + " | Equipe: "
                                            + EQUIPE_TESTE_ID
                            );

                        } else {

                            System.out.println(
                                    "Usuário já está na equipe de teste: "
                                            + user.getNome()
                            );
                        }
                    });
        }
    }

    // Cria o documento da equipe de teste se ainda não existir (os usuários de teste só
    // recebiam o equipeId, sem o documento, e /equipes/minha respondia 404). Idempotente:
    // se o documento já existe (inclusive criado à mão), só completa o gerenteId ausente.
    private void garantirDocumentoEquipeTeste() {

        String gerenteId = userRepository.findByEmail(EMAIL_GERENTE_TESTE)
                .map(User::getId)
                .orElse(null);

        Equipe equipe = equipeRepository.findById(EQUIPE_TESTE_ID).orElse(null);

        if (equipe == null) {

            LocalDateTime agora = LocalDateTime.now();

            equipe = new Equipe();
            equipe.setId(EQUIPE_TESTE_ID);
            equipe.setNome(EQUIPE_TESTE_NOME);
            equipe.setGerenteId(gerenteId);
            equipe.setCriadoEm(agora);
            equipe.setAtualizadoEm(agora);

            equipeRepository.save(equipe);

            System.out.println("Equipe de teste criada: " + EQUIPE_TESTE_ID);
            return;
        }

        // gerenteId é a chave de /equipes/minha para GERENTE; só preenche se o gerente
        // de teste ainda não administra outra equipe (evita duas equipes por gerente)
        if (equipe.getGerenteId() == null
                && gerenteId != null
                && equipeRepository.findByGerenteId(gerenteId).isEmpty()) {

            equipe.setGerenteId(gerenteId);
            equipe.setAtualizadoEm(LocalDateTime.now());

            equipeRepository.save(equipe);

            System.out.println("Equipe de teste recebeu o gerente: " + EMAIL_GERENTE_TESTE);
        } else {

            System.out.println("Equipe de teste já existe: " + EQUIPE_TESTE_ID);
        }
    }

    // SYN-100: registros criados antes do isolamento por equipe não têm equipeId e ficariam
    // invisíveis para todos. Atribui todos eles à equipe de teste. Idempotente: só toca em
    // documentos com equipeId nulo/ausente, então rodar de novo não altera nada.
    private void migrarDadosSemEquipe() {

        Query semEquipe = Query.query(Criteria.where("equipeId").is(null));
        Update atribuirEquipeTeste = Update.update("equipeId", EQUIPE_TESTE_ID);

        for (Class<?> entidade : ENTIDADES_DA_EQUIPE) {

            long alterados = mongoTemplate
                    .updateMulti(semEquipe, atribuirEquipeTeste, entidade)
                    .getModifiedCount();

            if (alterados > 0) {

                System.out.println(
                        "Migrado para a equipe de teste ("
                                + entidade.getSimpleName()
                                + "): "
                                + alterados
                );
            }
        }
    }

    // A paleta de teste agora é da equipe de teste (antes era semeada por usuário).
    private void seedTestCores() {

        String usuarioId = userRepository.findByEmail(EMAIL_GERENTE_TESTE)
                .map(User::getId)
                .orElse(null);

        Set<String> existentes = corRepository
                .findByEquipeId(EQUIPE_TESTE_ID)
                .stream()
                .map(Cor::getNome)
                .collect(Collectors.toSet());

        List<Cor> paleta = Arrays.asList(

                novaCor(
                        usuarioId,
                        "Vermelho Queimado",
                        "Coral Tintas",
                        "CT-204",
                        "#963A28",
                        Acabamento.FOSCO,
                        450,
                        500,
                        0.28
                ),

                novaCor(
                        usuarioId,
                        "Azul Cobalto",
                        "Sherwin-Williams",
                        "SW-118",
                        "#1E40AF",
                        Acabamento.BRILHANTE,
                        1250,
                        500,
                        0.36
                ),

                novaCor(
                        usuarioId,
                        "Verde Oliva",
                        "Coral Tintas",
                        "CT-331",
                        "#5E6B33",
                        Acabamento.CETIM,
                        300,
                        500,
                        0.30
                ),

                novaCor(
                        usuarioId,
                        "Bege Areia",
                        "Suvinil",
                        "SV-072",
                        "#CDBA98",
                        Acabamento.FOSCO,
                        980,
                        500,
                        0.25
                ),

                novaCor(
                        usuarioId,
                        "Preto Fosco",
                        "Weg Tintas",
                        "WG-001",
                        "#1E1E1E",
                        Acabamento.FOSCO,
                        620,
                        500,
                        0.31
                ),

                novaCor(
                        usuarioId,
                        "Branco Gelo",
                        "Suvinil",
                        "SV-010",
                        "#F1F0EA",
                        Acabamento.CETIM,
                        1800,
                        500,
                        0.22
                ),

                novaCor(
                        usuarioId,
                        "Terracota",
                        "Coral Tintas",
                        "CT-289",
                        "#C26A45",
                        Acabamento.FOSCO,
                        210,
                        500,
                        0.29
                ),

                novaCor(
                        usuarioId,
                        "Amarelo Mostarda",
                        "Sherwin-Williams",
                        "SW-447",
                        "#D9A227",
                        Acabamento.METALICO,
                        560,
                        500,
                        0.27
                )
        );

        int criadas = 0;

        for (Cor cor : paleta) {

            if (!existentes.contains(cor.getNome())) {

                corRepository.save(cor);
                criadas++;
            }
        }

        if (criadas > 0) {

            System.out.println(
                    "Cores de teste criadas para a equipe de teste: "
                            + criadas
            );

        } else {

            System.out.println(
                    "Cores de teste já existem na equipe de teste"
            );
        }
    }

    private Cor novaCor(
            String usuarioId,
            String nome,
            String fornecedor,
            String codigo,
            String hex,
            Acabamento acabamento,
            int estoqueMl,
            int estoqueMinimoMl,
            double custoMl
    ) {

        Cor cor = new Cor();

        cor.setEquipeId(EQUIPE_TESTE_ID);
        cor.setUsuarioId(usuarioId);
        cor.setNome(nome);
        cor.setFornecedor(fornecedor);
        cor.setCodigo(codigo);
        cor.setHex(hex);
        cor.setAcabamento(acabamento);
        cor.setEstoqueMl(estoqueMl);
        cor.setEstoqueMinimoMl(estoqueMinimoMl);
        cor.setCustoMl(custoMl);
        cor.setCriadoEm(LocalDateTime.now());
        cor.setAtualizadoEm(LocalDateTime.now());

        return cor;
    }
}
