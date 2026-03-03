package org.echoflow.core.rag;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PDF 文件加载器：基于 Apache PDFBox 提取每一页的纯文本内容。
 * 每一页独立成为一个 Document，并在 metadata 中标记页码和来源。
 */
public class PdfDocumentLoader implements DocumentLoader{

    @Override
    public List<Document> load(String uri) {
        List<Document> documents = new ArrayList<>();
        try (PDDocument pdf = PDDocument.load(new File(uri))) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = pdf.getNumberOfPages();
            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(pdf).trim();
                if (!text.isEmpty()) {
                    documents.add(new Document(text, Map.of(
                            "source", uri,
                            "page", page,
                            "type", "pdf"
                    )));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("[EchoFlow RAG] 读取 PDF 文件失败: " + uri, e);
        }
        return documents;
    }
    }
}
