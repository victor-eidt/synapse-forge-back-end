package synapseforge.crud.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Estoque.AjusteEstoqueRequestDTO;
import synapseforge.crud.DTO.Estoque.AlertaEstoqueResponseDTO;
import synapseforge.crud.DTO.Estoque.EntradaEstoqueRequestDTO;
import synapseforge.crud.DTO.Estoque.MovimentoEstoqueResponseDTO;
import synapseforge.crud.DTO.Estoque.SaldoInsumoResponseDTO;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.service.EstoqueService;

import java.util.List;

@RestController
@RequestMapping("/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService service;

    @PostMapping("/entrada")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentoEstoqueResponseDTO registrarEntrada(@RequestBody @Valid EntradaEstoqueRequestDTO dto,
                                                        Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        return service.registrarEntrada(dto.getTipoInsumo(), dto.getInsumoId(), dto.getQuantidade(),
                dto.getUnidade(), dto.getMotivo(), usuarioId);
    }

    @PostMapping("/ajuste")
    public MovimentoEstoqueResponseDTO ajustar(@RequestBody @Valid AjusteEstoqueRequestDTO dto,
                                               Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        return service.ajustar(dto.getTipoInsumo(), dto.getInsumoId(), dto.getQuantidade(),
                dto.getUnidade(), dto.getMotivo(), usuarioId);
    }

    @GetMapping("/alertas")
    public List<AlertaEstoqueResponseDTO> listarEmAlerta(Authentication auth) {
        return service.listarEmAlerta();
    }

    @GetMapping("/saldo")
    public SaldoInsumoResponseDTO consultarSaldo(@RequestParam TipoInsumo tipoInsumo,
                                                 @RequestParam String insumoId,
                                                 Authentication auth) {
        return service.consultarSaldo(tipoInsumo, insumoId);
    }

    @GetMapping("/movimentos")
    public List<MovimentoEstoqueResponseDTO> listarMovimentos(@RequestParam TipoInsumo tipoInsumo,
                                                              @RequestParam String insumoId,
                                                              Authentication auth) {
        return service.historicoPorInsumo(tipoInsumo, insumoId);
    }
}
