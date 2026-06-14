package synapseforge.crud.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.List;
import com.lowagie.text.ListItem;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;

import com.mongodb.client.gridfs.model.GridFSFile;

import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Service;

import synapseforge.crud.infrastructure.entity.Pedido;

import java.awt.Color;

import java.io.ByteArrayOutputStream;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    @Autowired
    private GridFsTemplate gridFsTemplate;

    public byte[] gerarOrdemServico(Pedido pedido) {

        try {

            Document document = new Document(PageSize.A4, 30, 30, 30, 25);

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            PdfWriter.getInstance(document, out);

            document.open();

            // =====================================================
            // CORES
            // =====================================================

            Color corPrimaria = new Color(25, 35, 60);
            Color corSecundaria = new Color(240, 240, 240);
            Color corTexto = new Color(60, 60, 60);

            // =====================================================
            // FONTES
            // =====================================================

            Font tituloFont = new Font(Font.HELVETICA, 22, Font.BOLD, Color.WHITE);

            Font secaoFont = new Font(Font.HELVETICA, 14, Font.BOLD, corPrimaria);

            Font labelFont = new Font(Font.HELVETICA, 11, Font.BOLD, corTexto);

            Font valorFont = new Font(Font.HELVETICA, 11, Font.NORMAL, corTexto);

            Font footerFont = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY);

            // =====================================================
            // HEADER CLEAN CORPORATIVO
            // =====================================================

            // LOGO
            Image logo = Image.getInstance(
                    getClass().getClassLoader().getResource("static/logo.png")
            );

            // tamanho da logo
            logo.scaleToFit(170, 170);

            // centraliza
            logo.setAlignment(Element.ALIGN_CENTER);

            // adiciona logo
            document.add(logo);

            // espaço
            document.add(new Paragraph(" "));

            // subtítulo
            Font subtituloFont = new Font(
                    Font.HELVETICA,
                    18,
                    Font.BOLD,
                    corPrimaria
            );

            Paragraph subtitulo = new Paragraph(
                    "ORDEM DE SERVIÇO",
                    subtituloFont
            );

            subtitulo.setAlignment(Element.ALIGN_CENTER);

            document.add(subtitulo);

            String numeroOS = "OS-" + pedido.getId().substring(0, 8).toUpperCase();

            Paragraph numeroDocumento = new Paragraph(
                    numeroOS,
                    new Font(Font.HELVETICA, 12, Font.BOLD, Color.GRAY)
            );

            numeroDocumento.setAlignment(Element.ALIGN_CENTER);

            document.add(numeroDocumento);

            // linha fina decorativa
            Paragraph linha = new Paragraph(
                    "____________________________________________",
                    new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(180,180,180))
            );

            linha.setAlignment(Element.ALIGN_CENTER);

            document.add(linha);

            document.add(new Paragraph(" "));


            // =====================================================
            // INFORMAÇÕES GERAIS
            // =====================================================

            Paragraph secao1 = new Paragraph(
                    "INFORMAÇÕES DO PEDIDO",
                    secaoFont
            );

            document.add(secao1);

            document.add(new Paragraph(" "));

            PdfPTable infoTable = new PdfPTable(2);

            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10f);
            infoTable.setSpacingAfter(10f);

            infoTable.setWidths(new int[]{1, 3});

            adicionarLinha(infoTable, "Cliente", pedido.getCliente(), labelFont, valorFont);
            adicionarLinha(infoTable, "Projeto", pedido.getProjeto(), labelFont, valorFont);
            adicionarLinha(infoTable, "Status", pedido.getStatus().toString(), labelFont, valorFont);
            adicionarLinha(infoTable, "Prazo", pedido.getPrazo().toString(), labelFont, valorFont);

            document.add(infoTable);

            String prazoFormatado = pedido.getPrazo()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            Paragraph prazoDestaque = new Paragraph(
                    "Prazo de Entrega: " + prazoFormatado,
                    new Font(Font.HELVETICA, 12, Font.BOLD, Color.RED)
            );

            prazoDestaque.setAlignment(Element.ALIGN_CENTER);

            document.add(prazoDestaque);
            document.add(new Paragraph(" "));

            // =====================================================
            // DESCRIÇÃO
            // =====================================================

            Paragraph secao2 = new Paragraph(
                    "DESCRIÇÃO TÉCNICA",
                    secaoFont
            );

            document.add(secao2);

            document.add(new Paragraph(" "));

            PdfPCell descricaoCell = new PdfPCell(
                    new Phrase(
                            pedido.getDescricao() != null
                                    ? pedido.getDescricao()
                                    : "Sem descrição.",
                            valorFont
                    )
            );

            descricaoCell.setPadding(15);
            descricaoCell.setBackgroundColor(corSecundaria);
            descricaoCell.setBorderColor(new Color(220, 220, 220));

            PdfPTable descricaoTable = new PdfPTable(1);

            descricaoTable.setWidthPercentage(100);
            descricaoTable.addCell(descricaoCell);

            document.add(descricaoTable);

            document.add(new Paragraph(" "));



            // =====================================================

            Paragraph secaoModelo = new Paragraph(
                    "INFORMAÇÕES DO MODELO 3D",
                    secaoFont
            );

            document.add(secaoModelo);

            document.add(new Paragraph(" "));

            PdfPTable modeloTable = new PdfPTable(2);

            modeloTable.setWidthPercentage(100);
            modeloTable.setWidths(new int[]{1, 3});

            // =========================================
            // OBJETO 3D
            // =========================================

            if (pedido.getObjeto3DFileId() != null) {

                GridFSFile arquivo3D = gridFsTemplate.findOne(
                        Query.query(
                                Criteria.where("_id")
                                        .is(new ObjectId(pedido.getObjeto3DFileId()))
                        )
                );

                if (arquivo3D != null) {

                    String nomeArquivo = arquivo3D.getFilename();

                    String formato = "Desconhecido";

                    if (nomeArquivo.contains(".")) {
                        formato = nomeArquivo.substring(
                                nomeArquivo.lastIndexOf(".") + 1
                        ).toUpperCase();
                    }

                    adicionarLinha(
                            modeloTable,
                            "Arquivo",
                            nomeArquivo,
                            labelFont,
                            valorFont
                    );

                    adicionarLinha(
                            modeloTable,
                            "Formato",
                            formato,
                            labelFont,
                            valorFont
                    );
                }

            } else {

                adicionarLinha(
                        modeloTable,
                        "Arquivo",
                        "Nenhum objeto enviado",
                        labelFont,
                        valorFont
                );
            }

            document.add(modeloTable);

            document.add(new Paragraph(" "));

            // =====================================================
            // IMAGENS DE REFERÊNCIA
            // =====================================================

            if (pedido.getImagensReferenciaFileIds() != null
                    && !pedido.getImagensReferenciaFileIds().isEmpty()) {

                com.lowagie.text.pdf.PdfPTable imagensSection = new PdfPTable(1);
                imagensSection.setWidthPercentage(100);
                imagensSection.setKeepTogether(true);

                // Título
                PdfPCell tituloCell = new PdfPCell(
                        new Phrase(
                                "IMAGENS DE REFERÊNCIA",
                                secaoFont
                        )
                );

                tituloCell.setBorder(Rectangle.NO_BORDER);
                tituloCell.setPaddingBottom(10);

                imagensSection.addCell(tituloCell);

                // Adiciona cada imagem
                for (String imagemId : pedido.getImagensReferenciaFileIds()) {

                    GridFSFile imagemFile = gridFsTemplate.findOne(
                            new Query(
                                    Criteria.where("_id")
                                            .is(new ObjectId(imagemId))
                            )
                    );

                    if (imagemFile != null) {

                        GridFsResource resource =
                                gridFsTemplate.getResource(imagemFile);

                        byte[] imagemBytes =
                                resource.getInputStream().readAllBytes();

                        Image imagem = Image.getInstance(imagemBytes);

                        // tamanho máximo
                        imagem.scaleToFit(450, 300);

                        imagem.setAlignment(Element.ALIGN_CENTER);

                        PdfPCell imagemCell = new PdfPCell(imagem);

                        imagemCell.setBorder(Rectangle.NO_BORDER);
                        imagemCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        imagemCell.setPaddingBottom(20);
                        imagemCell.setPaddingTop(10);

                        imagensSection.addCell(imagemCell);
                    }
                }

                document.add(imagensSection);

                document.add(new Paragraph(" "));
            }
            // =====================================================
            // GUIA DE PRODUÇÃO
            // =====================================================

            Paragraph secao3 = new Paragraph(
                    "GUIA DE PRODUÇÃO",
                    secaoFont
            );

            document.add(secao3);

            document.add(new Paragraph(" "));

            List lista = new List(List.UNORDERED);

            lista.add(new ListItem("Verificar integridade do modelo 3D", valorFont));
            lista.add(new ListItem("Conferir escala e proporções", valorFont));
            lista.add(new ListItem("Aplicar pintura conforme conceito aprovado", valorFont));
            lista.add(new ListItem("Realizar acabamento e inspeção final", valorFont));

            document.add(lista);

            document.add(new Paragraph(" "));

            // =====================================================


//            Paragraph secaoCores = new Paragraph(
//                    "GUIA DE CORES",
//                    secaoFont
//            );
//
//            document.add(secaoCores);
//
//            document.add(new Paragraph(" "));
//
//            PdfPTable coresTable = new PdfPTable(2);
//
//            coresTable.setWidthPercentage(100);
//            coresTable.setWidths(new int[]{1, 3});
//
//            adicionarLinha(
//                    coresTable,
//                    "Armadura",
//                    "Preto Fosco (70%)",
//                    labelFont,
//                    valorFont
//            );
//
//            adicionarLinha(
//                    coresTable,
//                    "Detalhes",
//                    "Laranja Metálico (20%)",
//                    labelFont,
//                    valorFont
//            );
//
//            adicionarLinha(
//                    coresTable,
//                    "Acabamento",
//                    "Cinza Grafite (10%)",
//                    labelFont,
//                    valorFont
//            );
//
//            document.add(coresTable);
//
//            document.add(new Paragraph(" "));
//
//            document.add(new Paragraph(" "));

            // ====================================================
            // RESUMO DO ORÇAMENTO
            // ====================================================

            Paragraph secaoOrcamento = new Paragraph(
                    "RESUMO DO ORÇAMENTO",
                    secaoFont
            );

            document.add(secaoOrcamento);

            document.add(new Paragraph(" "));


// Valores temporários.
// Quando a feature de orçamento estiver pronta,
// basta trocar por pedido.get...
            String valorMaterial = "Não informado";
            String valorImpressao = "Não informado";
            String valorPintura = "Não informado";
            String valorAcabamento = "Não informado";
            String valorTotal = "Não informado";

//            String valorMaterial = pedido.getOrcamento().getMaterial();
//            String valorImpressao = pedido.getOrcamento().getImpressao();
//            String valorPintura = pedido.getOrcamento().getPintura();
//            String valorAcabamento = pedido.getOrcamento().getAcabamento();
//            String valorTotal = pedido.getOrcamento().getTotal();

            PdfPTable orcamentoTable = new PdfPTable(2);

            orcamentoTable.setWidthPercentage(100);
            orcamentoTable.setWidths(new int[]{1, 3});

            adicionarLinha(
                    orcamentoTable,
                    "Material",
                    valorMaterial,
                    labelFont,
                    valorFont
            );

            adicionarLinha(
                    orcamentoTable,
                    "Impressão",
                    valorImpressao,
                    labelFont,
                    valorFont
            );

            adicionarLinha(
                    orcamentoTable,
                    "Pintura",
                    valorPintura,
                    labelFont,
                    valorFont
            );

            adicionarLinha(
                    orcamentoTable,
                    "Acabamento",
                    valorAcabamento,
                    labelFont,
                    valorFont
            );


// Linha TOTAL destacada
            Font totalFont = new Font(
                    Font.HELVETICA,
                    11,
                    Font.BOLD,
                    corPrimaria
            );

            PdfPCell totalLabel = new PdfPCell(
                    new Phrase("TOTAL", totalFont)
            );

            totalLabel.setPadding(10);
            totalLabel.setBorderColor(new Color(230, 230, 230));
            totalLabel.setBackgroundColor(corSecundaria);

            PdfPCell totalValor = new PdfPCell(
                    new Phrase(valorTotal, totalFont)
            );

            totalValor.setPadding(10);
            totalValor.setBorderColor(new Color(230, 230, 230));
            totalValor.setBackgroundColor(corSecundaria);

            orcamentoTable.addCell(totalLabel);
            orcamentoTable.addCell(totalValor);

            document.add(orcamentoTable);

            document.add(new Paragraph(" "));

            // =====================================================
            // DATA
            // =====================================================

            String dataAtual = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            Paragraph data = new Paragraph(
                    "Documento emitido em: " + dataAtual,
                    valorFont
            );

            data.setAlignment(Element.ALIGN_RIGHT);

            document.add(data);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));


            // =====================================================
            // ASSINATURA
            // =====================================================

            PdfPTable assinaturaTable = new PdfPTable(2);

            assinaturaTable.setWidthPercentage(100);

            PdfPCell cliente = new PdfPCell(
                    new Phrase(
                            "\n\n____________________________\nCliente",
                            valorFont
                    )
            );

            PdfPCell tecnico = new PdfPCell(
                    new Phrase(
                            "\n\n____________________________\nResponsável Técnico",
                            valorFont
                    )
            );

            cliente.setBorder(Rectangle.NO_BORDER);
            tecnico.setBorder(Rectangle.NO_BORDER);

            cliente.setHorizontalAlignment(Element.ALIGN_CENTER);
            tecnico.setHorizontalAlignment(Element.ALIGN_CENTER);

            assinaturaTable.addCell(cliente);
            assinaturaTable.addCell(tecnico);

            document.add(assinaturaTable);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // =====================================================
            // FOOTER
            // =====================================================

            Paragraph footer = new Paragraph(
                    "Synapse Forge • Sistema de Gerenciamento de Produção 3D",
                    footerFont
            );

            footer.setAlignment(Element.ALIGN_CENTER);

            document.add(footer);

            document.close();

            return out.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException("Erro ao gerar PDF: " + e.getMessage());
        }
    }

    private void adicionarLinha(
            PdfPTable tabela,
            String label,
            String valor,
            Font labelFont,
            Font valorFont
    ) {

        PdfPCell cell1 = new PdfPCell(
                new Phrase(label, labelFont)
        );

        cell1.setPadding(10);
        cell1.setBorderColor(new Color(230, 230, 230));

        PdfPCell cell2 = new PdfPCell(
                new Phrase(valor, valorFont)
        );

        cell2.setPadding(10);
        cell2.setBorderColor(new Color(230, 230, 230));

        tabela.addCell(cell1);
        tabela.addCell(cell2);
    }
}



