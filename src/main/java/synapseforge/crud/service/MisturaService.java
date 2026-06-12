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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MisturaService {

    @Autowired
    private MisturaRepository repository;

    @Autowired
    private CorRepository corRepository;

    public MisturaResponseDTO criar(MisturaRequestDTO dto, String usuarioId) {
        Map<String, Cor> cores = validarECarregarCores(dto, usuarioId);

        Mistura mistura = new Mistura();
        mistura.setUsuarioId(usuarioId);
        mistura.setCriadoEm(LocalDateTime.now());
        aplicar(mistura, dto, cores);

        return toResponseDTO(repository.save(mistura), cores);
    }

    public List<MisturaResponseDTO> listar(String usuarioId) {
        return repository.findByUsuarioId(usuarioId).stream()
                .map(m -> toResponseDTO(m, carregarCores(m, usuarioId)))
                .toList();
    }

    public MisturaResponseDTO buscarPorId(String id, String usuarioId) {
        Mistura mistura = buscarEntidade(id, usuarioId);
        return toResponseDTO(mistura, carregarCores(mistura, usuarioId));
    }

    public MisturaResponseDTO atualizar(String id, String usuarioId, MisturaRequestDTO dto) {
        Mistura mistura = buscarEntidade(id, usuarioId);
        Map<String, Cor> cores = validarECarregarCores(dto, usuarioId);
        aplicar(mistura, dto, cores);
        return toResponseDTO(repository.save(mistura), cores);
    }

    public void deletar(String id, String usuarioId) {
        buscarEntidade(id, usuarioId);
        repository.deleteById(id);
    }

    private Mistura buscarEntidade(String id, String usuarioId) {
        return repository.findById(id)
                .filter(m -> usuarioId.equals(m.getUsuarioId()))
                .orElseThrow(() -> new RuntimeException("Mistura não encontrada"));
    }

    private Map<String, Cor> validarECarregarCores(MisturaRequestDTO dto, String usuarioId) {
        long distintas = dto.getItens().stream().map(ItemMisturaRequestDTO::getCorId).distinct().count();
        if (distintas != dto.getItens().size()) {
            throw new RuntimeException("A mesma cor não pode aparecer mais de uma vez na mistura");
        }

        double soma = dto.getItens().stream().mapToDouble(ItemMisturaRequestDTO::getProporcao).sum();
        if (Math.abs(soma - 100.0) > 0.01) {
            throw new RuntimeException("A soma das proporções deve ser 100%");
        }

        return dto.getItens().stream()
                .map(item -> corRepository.findById(item.getCorId())
                        .filter(c -> usuarioId.equals(c.getUsuarioId()))
                        .orElseThrow(() -> new RuntimeException("Cor não encontrada na paleta")))
                .collect(Collectors.toMap(Cor::getId, Function.identity()));
    }

    private Map<String, Cor> carregarCores(Mistura mistura, String usuarioId) {
        List<String> ids = mistura.getItens().stream().map(ItemMistura::getCorId).toList();
        return corRepository.findAllById(ids).stream()
                .filter(c -> usuarioId.equals(c.getUsuarioId()))
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
