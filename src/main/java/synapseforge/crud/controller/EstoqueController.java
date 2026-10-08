package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

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

@Tag(name = "Estoque", description = "Saldo, entradas, ajustes e histórico de movimentação de materiais e tintas.")
@RestController
@RequestMapping("/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService service;

    @Operation(summary = "Registrar entrada", description = "Soma a quantidade ao saldo do insumo (compra, reposição).")
    @PostMapping("/entrada")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentoEstoqueResponseDTO registrarEntrada(@RequestBody @Valid EntradaEstoqueRequestDTO dto,
                                                        Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        return service.registrarEntrada(dto.getTipoInsumo(), dto.getInsumoId(), dto.getQuantidade(),
                dto.getUnidade(), dto.getMotivo(), usuarioId);
    }

    @Operation(summary = "Ajustar estoque", description = "Correção manual (inventário, perda). Quantidade negativa reduz o saldo.")
    @PostMapping("/ajuste")
    public MovimentoEstoqueResponseDTO ajustar(@RequestBody @Valid AjusteEstoqueRequestDTO dto,
                                               Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        return service.ajustar(dto.getTipoInsumo(), dto.getInsumoId(), dto.getQuantidade(),
                dto.getUnidade(), dto.getMotivo(), usuarioId);
    }

    @Operation(summary = "Insumos em alerta", description = "Lista materiais e tintas com saldo abaixo do mínimo.")
    @GetMapping("/alertas")
    public List<AlertaEstoqueResponseDTO> listarEmAlerta(Authentication auth) {
        return service.listarEmAlerta((String) auth.getPrincipal());
    }

    @Operation(summary = "Consultar saldo", description = "Saldo atual de um insumo e se está em alerta.")
    @GetMapping("/saldo")
    public SaldoInsumoResponseDTO consultarSaldo(@Parameter(description = "Tipo do insumo", example = "MATERIAL") @RequestParam TipoInsumo tipoInsumo,
                                                 @Parameter(description = "ID do material ou da cor", example = "6704a1c2e4b0f81a2c3d4e03") @RequestParam String insumoId,
                                                 Authentication auth) {
        return service.consultarSaldo(tipoInsumo, insumoId, (String) auth.getPrincipal());
    }

    @Operation(summary = "Histórico do insumo", description = "Movimentações do insumo (entradas, baixas, estornos e ajustes), da mais recente à mais antiga.")
    @GetMapping("/movimentos")
    public List<MovimentoEstoqueResponseDTO> listarMovimentos(@Parameter(description = "Tipo do insumo", example = "MATERIAL") @RequestParam TipoInsumo tipoInsumo,
                                                              @Parameter(description = "ID do material ou da cor", example = "6704a1c2e4b0f81a2c3d4e03") @RequestParam String insumoId,
                                                              Authentication auth) {
        return service.historicoPorInsumo(tipoInsumo, insumoId, (String) auth.getPrincipal());
    }
}
