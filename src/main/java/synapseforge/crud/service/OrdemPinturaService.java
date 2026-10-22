package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.OrdemPintura.OrdemPinturaRequestDTO;
import synapseforge.crud.DTO.OrdemPintura.OrdemPinturaResponseDTO;
import synapseforge.crud.DTO.OrdemPintura.TecnicoResumoDTO;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.entity.EtapaOrdemPintura;
import synapseforge.crud.infrastructure.entity.OrdemPintura;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.EquipeRepository;
import synapseforge.crud.infrastructure.repository.OrdemPinturaRepository;
import synapseforge.crud.infrastructure.repository.PedidoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.text.Collator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// Ordens de pintura são da equipe; pedido e cor referenciados precisam ser da mesma equipe.
@Service
@RequiredArgsConstructor
public class OrdemPinturaService {

    private final OrdemPinturaRepository repository;
    private final PedidoRepository pedidoRepository;
    private final CorRepository corRepository;
    private final PedidoService pedidoService;
    private final EquipeContexto equipeContexto;
    private final UserRepository userRepository;
    private final EquipeRepository equipeRepository;
    private final NotificacaoService notificacaoService;

    public List<OrdemPinturaResponseDTO> listar(String usuarioId) {
        List<OrdemPintura> ordens = equipeContexto.equipeDe(usuarioId)
                .map(repository::findByEquipeIdOrderByCriadoEmDesc)
                .orElse(List.of());
        // nomes dos técnicos numa consulta só, em vez de uma por ordem
        Map<String, String> nomes = nomesDosTecnicos(ordens);
        return ordens.stream()
                .map(ordem -> toResponseDTO(ordem, nomes))
                .toList();
    }

    /**
     * Quem pode receber uma ordem: técnicos ativos da equipe de quem pede e o gerente
     * dela (em oficina pequena é ele quem pinta), em ordem alfabética. Alimenta o select
     * do formulário no front.
     */
    public List<TecnicoResumoDTO> listarTecnicos(String usuarioId) {
        Collator alfabetica = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        return equipeContexto.equipeDe(usuarioId)
                .map(this::membrosDaEquipe)
                .orElse(List.of())
                .stream()
                .filter(this::podeReceberOrdem)
                .sorted(Comparator.comparing(User::getNome, Comparator.nullsLast(alfabetica)))
                .map(u -> new TecnicoResumoDTO(u.getId(), u.getNome()))
                .toList();
    }

    public OrdemPinturaResponseDTO criar(OrdemPinturaRequestDTO dto, String usuarioId) {
        String equipeId = equipeContexto.equipeObrigatoria(usuarioId);
        validarRelacionamentos(dto, equipeId);
        User tecnico = buscarTecnicoDaEquipe(dto.getTecnicoId(), equipeId);

        OrdemPintura ordem = new OrdemPintura();
        ordem.setEquipeId(equipeId);
        ordem.setUsuarioId(usuarioId);
        ordem.setPedidoId(dto.getPedidoId());
        ordem.setCorId(dto.getCorId());
        ordem.setTecnicoId(tecnico.getId());
        ordem.setTecnico(tecnico.getNome());
        ordem.setPrioridade(dto.getPrioridade());
        ordem.setPrazo(dto.getPrazo());
        ordem.setEtapa(EtapaOrdemPintura.AGUARDANDO);
        ordem.setCriadoEm(LocalDateTime.now());
        ordem.setAtualizadoEm(LocalDateTime.now());

        OrdemPinturaResponseDTO criada = toResponseDTO(repository.save(ordem));
        // só depois de salva: se a gravação falhar, o técnico não é avisado de algo que não existe
        avisarTecnico(criada, usuarioId);
        return criada;
    }

    public OrdemPinturaResponseDTO atualizarEtapa(
            String id,
            EtapaOrdemPintura etapa,
            String usuarioId
    ) {
        OrdemPintura ordem = buscarDaEquipe(id, equipeContexto.equipeObrigatoria(usuarioId));
        ordem.setEtapa(etapa);
        ordem.setAtualizadoEm(LocalDateTime.now());
        return toResponseDTO(repository.save(ordem));
    }

    public OrdemPinturaResponseDTO atualizar(
            String id,
            OrdemPinturaRequestDTO dto,
            String usuarioId
    ) {
        String equipeId = equipeContexto.equipeObrigatoria(usuarioId);
        OrdemPintura ordem = buscarDaEquipe(id, equipeId);
        validarRelacionamentos(dto, equipeId);

        // Manter o técnico que já estava na ordem é sempre permitido (mesmo que ele tenha
        // saído da equipe depois): só uma troca de técnico passa pela validação.
        boolean trocouTecnico = !dto.getTecnicoId().equals(ordem.getTecnicoId());
        boolean avisar = false;
        if (trocouTecnico) {
            User tecnico = buscarTecnicoDaEquipe(dto.getTecnicoId(), equipeId);
            // Ordem antiga (só com o nome digitado) que já era dessa pessoa ganha o vínculo
            // pelo id sem aviso de "nova ordem": ela já estava com a ordem.
            avisar = ordem.getTecnicoId() != null || !mesmoNome(ordem.getTecnico(), tecnico.getNome());
            ordem.setTecnicoId(tecnico.getId());
            ordem.setTecnico(tecnico.getNome());
        }

        ordem.setPedidoId(dto.getPedidoId());
        ordem.setCorId(dto.getCorId());
        ordem.setPrioridade(dto.getPrioridade());
        ordem.setPrazo(dto.getPrazo());
        ordem.setAtualizadoEm(LocalDateTime.now());

        OrdemPinturaResponseDTO atualizada = toResponseDTO(repository.save(ordem));
        // o novo responsável é avisado; editar outros campos não gera aviso
        if (avisar) {
            avisarTecnico(atualizada, usuarioId);
        }
        return atualizada;
    }

    // Usa o DTO já montado: projeto e cor saem dele, sem consultar o banco de novo
    private void avisarTecnico(OrdemPinturaResponseDTO ordem, String atribuidaPor) {
        notificacaoService.notificarOrdemPinturaAtribuida(
                ordem.getId(),
                ordem.getTecnicoId(),
                atribuidaPor,
                ordem.getPedidoProjeto(),
                ordem.getCorNome(),
                ordem.getPrazo()
        );
    }

    public void deletar(String id, String usuarioId) {
        OrdemPintura ordem = buscarDaEquipe(id, equipeContexto.equipeObrigatoria(usuarioId));
        repository.delete(ordem);
    }

    private OrdemPintura buscarDaEquipe(String id, String equipeId) {
        return repository.findByIdAndEquipeId(id, equipeId)
                .orElseThrow(() -> new RuntimeException("Ordem de pintura nao encontrada"));
    }

    private void validarRelacionamentos(OrdemPinturaRequestDTO dto, String equipeId) {
        pedidoRepository.findByIdAndEquipeId(dto.getPedidoId(), equipeId)
                .orElseThrow(() -> new RuntimeException("Pedido nao encontrado"));

        corRepository.findByIdAndEquipeId(dto.getCorId(), equipeId)
                .orElseThrow(() -> new RuntimeException("Cor nao encontrada"));

    }

    /** O id precisa ser de um técnico ativo da própria equipe ou do gerente dela; senão a ordem é recusada. */
    private User buscarTecnicoDaEquipe(String tecnicoId, String equipeId) {
        return userRepository.findById(tecnicoId)
                .filter(this::podeReceberOrdem)
                .filter(u -> equipeId.equals(u.getEquipeId()) || ehGerenteDa(equipeId, u))
                .orElseThrow(() -> new RuntimeException("Tecnico nao encontrado na equipe"));
    }

    private boolean podeReceberOrdem(User usuario) {
        return (usuario.getRole() == Role.TECNICO || usuario.getRole() == Role.GERENTE) && usuario.isAtivo();
    }

    // Usuários com equipeId da equipe mais o gerente dela, que pode não ter equipeId
    // gravado (quem criou a equipe é achado por Equipe.gerenteId, ver EquipeContexto)
    private List<User> membrosDaEquipe(String equipeId) {
        List<User> membros = new ArrayList<>(userRepository.findByEquipeId(equipeId));
        gerenteDa(equipeId)
                .filter(gerente -> membros.stream().noneMatch(m -> Objects.equals(m.getId(), gerente.getId())))
                .ifPresent(membros::add);
        return membros;
    }

    private Optional<User> gerenteDa(String equipeId) {
        return equipeRepository.findById(equipeId)
                .map(Equipe::getGerenteId)
                .flatMap(userRepository::findById);
    }

    private boolean ehGerenteDa(String equipeId, User usuario) {
        return usuario.getRole() == Role.GERENTE
                && equipeRepository.findById(equipeId)
                        .map(Equipe::getGerenteId)
                        .filter(gerenteId -> gerenteId.equals(usuario.getId()))
                        .isPresent();
    }

    private static boolean mesmoNome(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private OrdemPinturaResponseDTO toResponseDTO(OrdemPintura ordem) {
        return toResponseDTO(ordem, nomesDosTecnicos(List.of(ordem)));
    }

    private OrdemPinturaResponseDTO toResponseDTO(OrdemPintura ordem, Map<String, String> nomesDosTecnicos) {
        // pedido e cor lidos pela equipe da ordem: nunca exibe dados de outra oficina
        Pedido pedido = pedidoRepository.findByIdAndEquipeId(ordem.getPedidoId(), ordem.getEquipeId()).orElse(null);
        Cor cor = corRepository.findByIdAndEquipeId(ordem.getCorId(), ordem.getEquipeId()).orElse(null);

        List<String> referencias = pedido == null
                ? List.of()
                : pedidoService.toResponseDTO(pedido).getImagensReferenciaFileIds();

        return new OrdemPinturaResponseDTO(
                ordem.getId(),
                ordem.getPedidoId(),
                pedido == null ? "Pedido removido" : pedido.getProjeto(),
                pedido == null ? "" : pedido.getCliente(),
                ordem.getCorId(),
                cor == null ? "Cor removida" : cor.getNome(),
                cor == null ? "#D9D9D9" : cor.getHex(),
                cor == null || cor.getAcabamento() == null ? null : cor.getAcabamento().name(),
                ordem.getTecnicoId(),
                nomeDoTecnico(ordem, nomesDosTecnicos),
                ordem.getPrioridade(),
                ordem.getPrazo(),
                ordem.getEtapa(),
                referencias == null ? List.of() : referencias,
                ordem.getCriadoEm(),
                ordem.getAtualizadoEm()
        );
    }

    // Nome atual de cada técnico das ordens, por id (reflete troca de nome no perfil)
    private Map<String, String> nomesDosTecnicos(List<OrdemPintura> ordens) {
        Set<String> ids = ordens.stream()
                .map(OrdemPintura::getTecnicoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .filter(u -> u.getNome() != null)
                .collect(Collectors.toMap(User::getId, User::getNome, (a, b) -> a));
    }

    // Ordens antigas, sem tecnicoId, ou técnico removido do banco caem no nome gravado na ordem.
    private String nomeDoTecnico(OrdemPintura ordem, Map<String, String> nomesDosTecnicos) {
        if (ordem.getTecnicoId() == null) {
            return ordem.getTecnico();
        }
        return nomesDosTecnicos.getOrDefault(ordem.getTecnicoId(), ordem.getTecnico());
    }
}
