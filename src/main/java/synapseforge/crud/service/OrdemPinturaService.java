package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.OrdemPintura.OrdemPinturaRequestDTO;
import synapseforge.crud.DTO.OrdemPintura.OrdemPinturaResponseDTO;
import synapseforge.crud.DTO.OrdemPintura.TecnicoResumoDTO;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.entity.EtapaOrdemPintura;
import synapseforge.crud.infrastructure.entity.OrdemPintura;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.OrdemPinturaRepository;
import synapseforge.crud.infrastructure.repository.PedidoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.text.Collator;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    public List<OrdemPinturaResponseDTO> listar(String usuarioId) {
        return equipeContexto.equipeDe(usuarioId)
                .map(repository::findByEquipeIdOrderByCriadoEmDesc)
                .orElse(List.of())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Técnicos que podem receber uma ordem: usuários TECNICO da equipe de quem pede,
     * em ordem alfabética. Alimenta o select do formulário no front.
     */
    public List<TecnicoResumoDTO> listarTecnicos(String usuarioId) {
        Collator alfabetica = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        return equipeContexto.equipeDe(usuarioId)
                .map(userRepository::findByEquipeId)
                .orElse(List.of())
                .stream()
                .filter(this::ehTecnicoAtivo)
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
        return toResponseDTO(repository.save(ordem));
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
        if (!dto.getTecnicoId().equals(ordem.getTecnicoId())) {
            User tecnico = buscarTecnicoDaEquipe(dto.getTecnicoId(), equipeId);
            ordem.setTecnicoId(tecnico.getId());
            ordem.setTecnico(tecnico.getNome());
        }

        ordem.setPedidoId(dto.getPedidoId());
        ordem.setCorId(dto.getCorId());
        ordem.setPrioridade(dto.getPrioridade());
        ordem.setPrazo(dto.getPrazo());
        ordem.setAtualizadoEm(LocalDateTime.now());
        return toResponseDTO(repository.save(ordem));
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

    /** O id precisa ser de um usuário TECNICO ativo da própria equipe; senão a ordem é recusada. */
    private User buscarTecnicoDaEquipe(String tecnicoId, String equipeId) {
        return userRepository.findById(tecnicoId)
                .filter(this::ehTecnicoAtivo)
                .filter(u -> equipeId.equals(u.getEquipeId()))
                .orElseThrow(() -> new RuntimeException("Tecnico nao encontrado na equipe"));
    }

    private boolean ehTecnicoAtivo(User usuario) {
        return usuario.getRole() == Role.TECNICO && usuario.isAtivo();
    }

    private OrdemPinturaResponseDTO toResponseDTO(OrdemPintura ordem) {
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
                nomeDoTecnico(ordem),
                ordem.getPrioridade(),
                ordem.getPrazo(),
                ordem.getEtapa(),
                referencias == null ? List.of() : referencias,
                ordem.getCriadoEm(),
                ordem.getAtualizadoEm()
        );
    }

    // Nome atual do técnico (reflete troca de nome no perfil); ordens antigas, sem
    // tecnicoId, ou técnico removido do banco caem no nome gravado na ordem.
    private String nomeDoTecnico(OrdemPintura ordem) {
        if (ordem.getTecnicoId() == null) {
            return ordem.getTecnico();
        }
        return userRepository.findById(ordem.getTecnicoId())
                .map(User::getNome)
                .orElse(ordem.getTecnico());
    }
}
