package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import synapseforge.crud.DTO.User.ClienteResumoDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.PedidoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder encoder;
    private final EmailService emailService;
    private final EquipeContexto equipeContexto;
    private final PedidoRepository pedidoRepository;


    // =========================================================
    // DTO -> ENTITY
    // =========================================================

    public User toEntity(UserRequestDTO dto) {

        User user = new User();

        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setSenha(dto.getSenha());
        user.setCpf(dto.getCpf());
        user.setTelefone(dto.getTelefone());
        user.setRole(dto.getRole());

        return user;
    }


    // =========================================================
    // ENTITY -> DTO
    // =========================================================

    public UserResponseDTO toResponseDTO(User user) {

        return new UserResponseDTO(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getCpf(),
                user.getTelefone(),
                user.getRole() != null
                        ? user.getRole().name()
                        : null,
                user.getEquipeId(),
                user.getFuncaoVisual()
        );
    }


    // =========================================================
    // CRIAR
    // =========================================================

    public User criar(User user) {

        if (repository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado");
        }

        user.setSenha(
                encoder.encode(user.getSenha())
        );

        user.setCriadoEm(
                LocalDateTime.now()
        );

        user.setAtivo(true);

        return repository.save(user);
    }


    // =========================================================
    // LISTAR
    // =========================================================
    //
    // Somente usuários da equipe de quem consulta (sem equipe -> vazio).
    // A listagem da plataforma inteira fica no AdminService.
    //

    public List<User> listar(String usuarioId) {

        return equipeContexto.equipeDe(usuarioId)
                .map(repository::findByEquipeId)
                .orElse(List.of());
    }


    // =========================================================
    // BUSCAR POR ID
    // =========================================================

    public Optional<User> buscarPorId(String id) {

        return repository.findById(id);
    }


    // =========================================================
    // ACESSO A OUTRO USUÁRIO (/users/{id})
    // =========================================================
    //
    // Permitido para o próprio usuário; fora isso, o alvo precisa ser da
    // equipe de quem pede. Na leitura, também os clientes vinculados à
    // equipe por pedidos. O resto se comporta como inexistente.
    //

    public Optional<User> buscarParaUsuario(
            String solicitanteId,
            String id
    ) {

        return alvoAcessivel(solicitanteId, id, true);
    }

    private Optional<User> alvoAcessivel(
            String solicitanteId,
            String alvoId,
            boolean incluirClientesDaEquipe
    ) {

        Optional<User> alvo = repository.findById(alvoId);

        if (alvo.isEmpty() || alvoId.equals(solicitanteId)) {
            return alvo;
        }

        Optional<String> equipe =
                equipeContexto.equipeDe(solicitanteId);

        if (equipe.isEmpty()) {
            return Optional.empty();
        }

        User usuario = alvo.get();

        if (equipe.equals(equipeContexto.equipeDe(usuario))) {
            return alvo;
        }

        if (incluirClientesDaEquipe
                && usuario.getRole() == Role.CLIENTE
                && pedidoRepository.existsByEquipeIdAndClienteId(
                        equipe.get(),
                        alvoId
                )) {
            return alvo;
        }

        return Optional.empty();
    }

    private User alvoEditavel(
            String solicitanteId,
            String id
    ) {

        return alvoAcessivel(solicitanteId, id, false)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"
                        )
                );
    }


    // =========================================================
    // ATUALIZAR
    // =========================================================

    public User atualizar(
            String solicitanteId,
            String id,
            UserRequestDTO dto
    ) {

        User user = alvoEditavel(solicitanteId, id);

        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setCpf(dto.getCpf());
        user.setTelefone(dto.getTelefone());


        // Só altera a role se uma nova role foi enviada
        if (dto.getRole() != null) {

            user.setRole(
                    dto.getRole()
            );
        }


        // Só altera a senha se uma nova senha foi enviada
        if (
                dto.getSenha() != null
                        && !dto.getSenha().isBlank()
        ) {

            user.setSenha(
                    encoder.encode(dto.getSenha())
            );
        }

        user.setAtualizadoEm(
                LocalDateTime.now()
        );

        return repository.save(user);
    }


    // =========================================================
    // DELETAR
    // =========================================================

    public void deletar(
            String solicitanteId,
            String id
    ) {

        alvoEditavel(solicitanteId, id);

        repository.deleteById(id);
    }


    // =========================================================
    // CRIAR VÁRIOS
    // =========================================================

    public List<User> criarVarios(
            List<User> users
    ) {

        users.forEach(user -> {

            user.setSenha(
                    encoder.encode(user.getSenha())
            );

            user.setCriadoEm(
                    LocalDateTime.now()
            );

            user.setAtivo(true);
        });

        return repository.saveAll(users);
    }


    // =========================================================
    // BUSCAR POR NOME
    // =========================================================

    public List<User> buscarPorNome(
            String usuarioId,
            String nome
    ) {

        if (
                nome == null
                        || nome.trim().length() < 3
        ) {

            throw new RuntimeException(
                    "Mínimo 3 caracteres para busca"
            );
        }

        return equipeContexto.equipeDe(usuarioId)
                .map(equipeId ->
                        repository.findByEquipeIdAndNomeIgnoreCaseContaining(
                                equipeId,
                                nome.trim()
                        )
                )
                .orElse(List.of());
    }


    // =========================================================
    // SOLICITAR MUDANÇA DE EMAIL
    // =========================================================

    // Só o próprio usuário troca o próprio email: trocar o de um colega
    // permitiria tomar a conta dele confirmando o email novo.
    public Map<String, String> solicitarMudancaEmail(
            String solicitanteId,
            String id,
            String novoEmail
    ) {

        if (id == null || !id.equals(solicitanteId)) {

            throw new RuntimeException(
                    "Usuário não encontrado"
            );
        }

        return solicitarMudancaEmail(id, novoEmail);
    }

    public Map<String, String> solicitarMudancaEmail(
            String id,
            String novoEmail
    ) {

        User user = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"
                        )
                );


        if (
                repository.findByEmail(novoEmail)
                        .isPresent()
        ) {

            throw new RuntimeException(
                    "Este email já está em uso"
            );
        }


        String token =
                UUID.randomUUID().toString();


        user.setEmailPendente(
                novoEmail
        );

        user.setEmailMudancaToken(
                token
        );

        user.setEmailMudancaTokenExpira(
                LocalDateTime.now().plusHours(1)
        );


        repository.save(user);


        emailService.enviarConfirmacaoMudancaEmail(
                novoEmail,
                user.getNome(),
                token
        );


        return Map.of(
                "mensagem",
                "Email de confirmação enviado para "
                        + novoEmail
        );
    }


    // =========================================================
    // CONFIRMAR MUDANÇA DE EMAIL
    // =========================================================

    public void confirmarMudancaEmail(
            String token
    ) {

        User user =
                repository.findByEmailMudancaToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Token inválido ou já utilizado"
                                )
                        );


        if (
                user.getEmailMudancaTokenExpira() == null
                        || user.getEmailMudancaTokenExpira()
                        .isBefore(LocalDateTime.now())
        ) {

            throw new RuntimeException(
                    "Token expirado"
            );
        }


        user.setEmail(
                user.getEmailPendente()
        );

        user.setEmailPendente(null);

        user.setEmailMudancaToken(null);

        user.setEmailMudancaTokenExpira(null);

        user.setAtualizadoEm(
                LocalDateTime.now()
        );


        repository.save(user);
    }


    // =========================================================
    // ATUALIZAR DADOS DO PERFIL
    // =========================================================

    public User atualizarProprioPerfil(
            String id,
            UserRequestDTO dto
    ) {

        User user = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"
                        )
                );

        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setCpf(dto.getCpf());
        user.setTelefone(dto.getTelefone());

        if (
                dto.getSenha() != null
                        && !dto.getSenha().isBlank()
        ) {

            user.setSenha(
                    encoder.encode(dto.getSenha())
            );
        }

        user.setAtualizadoEm(
                LocalDateTime.now()
        );

        return repository.save(user);
    }


    // =========================================================
    // LISTAGEM DE CLIENTES PARA PEDIDOS
    // =========================================================
    //
    // Clientes não pertencem a equipe: os "clientes da equipe" são os
    // clientes com pelo menos um pedido na equipe de quem consulta
    // (sem equipe -> vazio). Não existe listagem de todos os clientes
    // da plataforma fora do /admin.
    //

    public List<User> listarClientes(String usuarioId) {

        return equipeContexto.equipeDe(usuarioId)
                .map(this::clientesComPedidoNaEquipe)
                .orElse(List.of());
    }

    private List<User> clientesComPedidoNaEquipe(String equipeId) {

        Set<String> clienteIds =
                pedidoRepository.findClienteIdsByEquipeId(equipeId)
                        .stream()
                        .map(Pedido::getClienteId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        if (clienteIds.isEmpty()) {
            return List.of();
        }

        return repository.findAllById(clienteIds)
                .stream()
                .filter(user ->
                        user.getRole() == Role.CLIENTE
                )
                .sorted(Comparator.comparing(
                        User::getNome,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .toList();
    }


    // =========================================================
    // BUSCAR CLIENTE POR EMAIL (para vincular a um pedido)
    // =========================================================
    //
    // Email exato, sem diferenciar maiúsculas; só devolve usuários
    // CLIENTE (outros perfis se comportam como inexistentes). Exige
    // equipe: a busca só serve para vincular cliente a pedido da equipe.
    //

    public Optional<User> buscarClientePorEmail(
            String usuarioId,
            String email
    ) {

        equipeContexto.equipeObrigatoria(usuarioId);

        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        String padrao =
                "^" + Pattern.quote(email.trim()) + "$";

        return repository.findByEmailPadraoIgnorandoCaixa(padrao)
                .stream()
                .filter(user ->
                        user.getRole() == Role.CLIENTE
                )
                .findFirst();
    }

    public ClienteResumoDTO toClienteResumoDTO(User user) {

        return new ClienteResumoDTO(
                user.getId(),
                user.getNome(),
                user.getEmail()
        );
    }


    // =========================================================
    // VÍNCULO COM EQUIPE
    // =========================================================

    public User vincularEquipe(
            String usuarioId,
            String equipeId
    ) {

        User user = repository.findById(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"
                        )
                );

        user.setEquipeId(equipeId);

        user.setAtualizadoEm(
                LocalDateTime.now()
        );

        return repository.save(user);
    }


    public User desvincularEquipe(
            String usuarioId
    ) {

        User user = repository.findById(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"
                        )
                );

        user.setEquipeId(null);

        user.setAtualizadoEm(
                LocalDateTime.now()
        );

        return repository.save(user);
    }


    // =========================================================
    // CLIENTES DISPONÍVEIS PARA EQUIPE
    // =========================================================

    public List<User> listarClientesDisponiveisParaEquipe() {

        return repository.findByRoleAndEquipeIdIsNull(
                Role.CLIENTE
        );
    }


    // =========================================================
    // ENTRAR EM EQUIPE
    // =========================================================

    public User entrarNaEquipe(
            String usuarioId,
            String equipeId
    ) {

        User user = repository.findById(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"
                        )
                );


        // -----------------------------------------------------
        // O usuário precisa ser CLIENTE
        // -----------------------------------------------------

        if (user.getRole() != Role.CLIENTE) {

            throw new RuntimeException(
                    "Somente clientes podem entrar em uma equipe"
            );
        }


        // -----------------------------------------------------
        // O usuário não pode estar em outra equipe
        // -----------------------------------------------------

        if (user.getEquipeId() != null) {

            throw new RuntimeException(
                    "Este usuário já pertence a uma equipe"
            );
        }


        // -----------------------------------------------------
        // Vincula equipe
        // CLIENTE → TECNICO
        // -----------------------------------------------------

        user.setEquipeId(equipeId);

        user.setRole(Role.TECNICO);

        user.setAtualizadoEm(
                LocalDateTime.now()
        );


        return repository.save(user);
    }


    // =========================================================
    // SAIR DA EQUIPE
    // =========================================================

    public User sairDaEquipe(
            String usuarioId,
            String equipeId
    ) {

        User user = repository.findById(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"
                        )
                );


        // -----------------------------------------------------
        // O usuário precisa ser TECNICO
        // -----------------------------------------------------

        if (user.getRole() != Role.TECNICO) {

            throw new RuntimeException(
                    "O usuário não é um técnico"
            );
        }


        // -----------------------------------------------------
        // Confere se pertence à equipe informada
        // -----------------------------------------------------

        if (
                user.getEquipeId() == null
                        || !user.getEquipeId().equals(equipeId)
        ) {

            throw new RuntimeException(
                    "O usuário não pertence a esta equipe"
            );
        }


        // -----------------------------------------------------
        // Remove vínculo
        // TECNICO → CLIENTE
        // -----------------------------------------------------

        user.setEquipeId(null);

        user.setRole(Role.CLIENTE);

        user.setAtualizadoEm(
                LocalDateTime.now()
        );


        return repository.save(user);
    }


    // =========================================================
    // LISTAR USUÁRIOS DA EQUIPE
    // =========================================================

    public List<User> listarPorEquipeId(
            String equipeId
    ) {

        return repository.findByEquipeId(
                equipeId
        );
    }

    // =========================================================
    // ATUALIZAR FUNÇÃO VISUAL DO INTEGRANTE
    // =========================================================

    public User atualizarFuncaoVisual(
            String usuarioId,
            String funcaoVisual
    ) {

        User user = repository.findById(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"
                        )
                );

        if (funcaoVisual == null
                || funcaoVisual.isBlank()) {

            user.setFuncaoVisual(null);

        } else {

            user.setFuncaoVisual(
                    funcaoVisual.trim()
            );
        }

        user.setAtualizadoEm(
                LocalDateTime.now()
        );

        return repository.save(user);
    }
}