package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.FormaEnvio.FormaEnvioRequestDTO;
import synapseforge.crud.DTO.FormaEnvio.FormaEnvioResponseDTO;
import synapseforge.crud.exception.RecursoNaoEncontradoException;
import synapseforge.crud.infrastructure.entity.FormaEnvio;
import synapseforge.crud.infrastructure.repository.FormaEnvioRepository;

import java.util.List;
import java.util.Optional;

/**
 * Formas de envio: as padrão do sistema (sem equipe, semeadas pelo FormaEnvioSeeder)
 * valem para todas as oficinas; as cadastradas por uma equipe só aparecem para ela.
 */
@Service
@RequiredArgsConstructor
public class FormaEnvioService {

    private final FormaEnvioRepository repository;
    private final EquipeContexto equipeContexto;

    public List<FormaEnvioResponseDTO> listarAtivas(String usuarioId) {
        // cliente (sem equipe) vê só as padrão: o suficiente para entender o envio do pedido dele
        List<FormaEnvio> formas = equipeContexto.equipeDe(usuarioId)
                .map(repository::findAtivasDisponiveisPara)
                .orElseGet(repository::findByAtivoTrueAndEquipeIdIsNull);
        return formas.stream().map(this::toResponseDTO).toList();
    }

    public FormaEnvioResponseDTO criar(FormaEnvioRequestDTO dto, String usuarioId) {
        FormaEnvio forma = new FormaEnvio();
        forma.setEquipeId(equipeContexto.equipeObrigatoria(usuarioId));
        forma.setNome(dto.getNome().trim());
        forma.setTransportadora(limpar(dto.getTransportadora()));
        forma.setUrlRastreioTemplate(limpar(dto.getUrlRastreioTemplate()));
        forma.setAtivo(true);
        return toResponseDTO(repository.save(forma));
    }

    /**
     * Forma que um pedido da equipe pode usar: ativa e padrão do sistema ou da própria equipe.
     * Forma inexistente, inativa ou de outra oficina -> 404.
     */
    public FormaEnvio buscarDisponivel(String id, String equipeId) {
        return repository.findAtivaDisponivelPara(id, equipeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Forma de envio não encontrada"));
    }

    /** Leitura tolerante para montar a resposta do pedido: forma apagada não derruba a tela. */
    public Optional<FormaEnvio> buscarPorId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return repository.findById(id);
    }

    private static String limpar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private FormaEnvioResponseDTO toResponseDTO(FormaEnvio forma) {
        return new FormaEnvioResponseDTO(
                forma.getId(),
                forma.getNome(),
                forma.getTransportadora(),
                forma.getUrlRastreioTemplate(),
                forma.getEquipeId() == null,
                forma.getAtivo()
        );
    }
}
