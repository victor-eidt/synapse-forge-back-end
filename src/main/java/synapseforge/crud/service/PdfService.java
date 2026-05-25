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

            Paragraph assinatura = new Paragraph(
                    "__________________________________\nResponsável Técnico",
                    valorFont
            );

            assinatura.setAlignment(Element.ALIGN_CENTER);

            document.add(assinatura);

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