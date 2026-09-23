package synapseforge.crud.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Mistura.ItemMisturaRequestDTO;
import synapseforge.crud.DTO.Mistura.ItemMisturaResponseDTO;
import synapseforge.crud.DTO.Mistura.MisturaRequestDTO;
import synapseforge.crud.DTO.Mistura.MisturaResponseDTO;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.entity.ItemMistura;
import synapseforge.crud.infrastructure.entity.Mistura;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.MisturaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// Misturas são da equipe e só podem usar cores da paleta da mesma equipe.
@Service
public class MisturaService {

    @Autowired
    private MisturaRepository repository;

    @Autowired
    private CorRepository corRepository;

    @Autowired
    private EquipeContexto equipeContexto;

    public MisturaResponseDTO criar(MisturaRequestDTO dto, String usuarioId) {
        String equipeId = equipeContexto.equipeObrigatoria(usuarioId);
        Map<String, Cor> cores = validarECarregarCores(dto, equipeId);

        Mistura mistura = new Mistura();
        mistura.setEquipeId(equipeId);
        mistura.setUsuarioId(usuarioId);
        mistura.setCriadoEm(LocalDateTime.now());
        aplicar(mistura, dto, cores);

        return toResponseDTO(repository.save(mistura), cores);
    }

    public List<MisturaResponseDTO> listar(String usuarioId) {
        Optional<String> equipe = equipeContexto.equipeDe(usuarioId);
        if (equipe.isEmpty()) {
            return List.of();
        }
        String equipeId = equipe.get();
        return repository.findByEquipeId(equipeId).stream()
                .map(m -> toResponseDTO(m, carregarCores(m, equipeId)))
                .toList();
    }

    public MisturaResponseDTO buscarPorId(String id, String usuarioId) {
        // leitura de um registro: sem equipe, simplesmente não existe
        String equipeId = equipeContexto.equipeDe(usuarioId)
                .orElseThrow(() -> new RuntimeException("Mistura não encontrada"));
        Mistura mistura = buscarEntidade(id, equipeId);
        return toResponseDTO(mistura, carregarCores(mistura, equipeId));
    }

    public MisturaResponseDTO atualizar(String id, String usuarioId, MisturaRequestDTO dto) {
        String equipeId = equipeContexto.equipeObrigatoria(usuarioId);
        Mistura mistura = buscarEntidade(id, equipeId);
        Map<String, Cor> cores = validarECarregarCores(dto, equipeId);
        aplicar(mistura, dto, cores);
        return toResponseDTO(repository.save(mistura), cores);
    }

    public void deletar(String id, String usuarioId) {
        buscarEntidade(id, equipeContexto.equipeObrigatoria(usuarioId));
        repository.deleteById(id);
    }

    private Mistura buscarEntidade(String id, String equipeId) {
        return repository.findByIdAndEquipeId(id, equipeId)
                .orElseThrow(() -> new RuntimeException("Mistura não encontrada"));
    }

    private Map<String, Cor> validarECarregarCores(MisturaRequestDTO dto, String equipeId) {
        long distintas = dto.getItens().stream().map(ItemMisturaRequestDTO::getCorId).distinct().count();
        if (distintas != dto.getItens().size()) {
            throw new RuntimeException("A mesma cor não pode aparecer mais de uma vez na mistura");
        }

        double soma = dto.getItens().stream().mapToDouble(ItemMisturaRequestDTO::getProporcao).sum();
        if (Math.abs(soma - 100.0) > 0.01) {
            throw new RuntimeException("A soma das proporções deve ser 100%");
        }

        return dto.getItens().stream()
                .map(item -> corRepository.findByIdAndEquipeId(item.getCorId(), equipeId)
                        .orElseThrow(() -> new RuntimeException("Cor não encontrada na paleta")))
                .collect(Collectors.toMap(Cor::getId, Function.identity()));
    }

    private Map<String, Cor> carregarCores(Mistura mistura, String equipeId) {
        List<String> ids = mistura.getItens().stream().map(ItemMistura::getCorId).toList();
        return corRepository.findByIdInAndEquipeId(ids, equipeId).stream()
                .collect(Collectors.toMap(Cor::getId, Function.identity()));
    }

    private void aplicar(Mistura mistura, MisturaRequestDTO dto, Map<String, Cor> cores) {
        mistura.setNome(dto.getNome());
        mistura.setItens(dto.getItens().stream()
                .map(i -> new ItemMistura(i.getCorId(), i.getProporcao()))
                .toList());
        mistura.setVolumeMl(dto.getVolumeMl());
        mistura.setHexResultado(calcularHexResultado(mistura.getItens(), cores));
        mistura.setCustoEstimado(calcularCustoEstimado(mistura.getItens(), cores, dto.getVolumeMl()));
        mistura.setAtualizadoEm(LocalDateTime.now());
    }

    private String calcularHexResultado(List<ItemMistura> itens, Map<String, Cor> cores) {
        double r = 0, g = 0, b = 0;
        for (ItemMistura item : itens) {
            Cor cor = cores.get(item.getCorId());
            if (cor == null) continue;
            int rgb = Integer.parseInt(cor.getHex().substring(1), 16);
            double peso = item.getProporcao() / 100.0;
            r += ((rgb >> 16) & 0xFF) * peso;
            g += ((rgb >> 8) & 0xFF) * peso;
            b += (rgb & 0xFF) * peso;
        }
        return String.format("#%02X%02X%02X", Math.round(r), Math.round(g), Math.round(b)).toUpperCase();
    }

    private Double calcularCustoEstimado(List<ItemMistura> itens, Map<String, Cor> cores, Integer volumeMl) {
        double custo = 0;
        for (ItemMistura item : itens) {
            Cor cor = cores.get(item.getCorId());
            if (cor == null) continue;
            custo += volumeMl * (item.getProporcao() / 100.0) * cor.getCustoMl();
        }
        return Math.round(custo * 100.0) / 100.0;
    }

    private MisturaResponseDTO toResponseDTO(Mistura mistura, Map<String, Cor> cores) {
        List<ItemMisturaResponseDTO> itens = mistura.getItens().stream()
                .map(item -> {
                    Cor cor = cores.get(item.getCorId());
                    int volumeItem = (int) Math.round(mistura.getVolumeMl() * item.getProporcao() / 100.0);
                    double custoItem = cor == null ? 0
                            : Math.round(volumeItem * cor.getCustoMl() * 100.0) / 100.0;
                    return new ItemMisturaResponseDTO(
                            item.getCorId(),
                            cor == null ? null : cor.getNome(),
                            cor == null ? null : cor.getFornecedor(),
                            cor == null ? null : cor.getHex(),
                            item.getProporcao(),
                            volumeItem,
                            custoItem
                    );
                })
                .toList();

        return new MisturaResponseDTO(
                mistura.getId(),
                mistura.getNome(),
                itens,
                mistura.getVolumeMl(),
                mistura.getHexResultado(),
                mistura.getCustoEstimado(),
                mistura.getCriadoEm(),
                mistura.getAtualizadoEm()
        );
    }
}
