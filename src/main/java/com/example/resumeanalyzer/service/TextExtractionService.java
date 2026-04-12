package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.exception.ApiException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

@Service
public class TextExtractionService {

    private static final int MAX_CHARS = 500_000;
    private final Parser parser = new AutoDetectParser();

    public String extractText(MultipartFile file) {
        Path temp = null;
        try {
            temp = Files.createTempFile("resume-upload-", ".bin");
            file.transferTo(temp);
            try (InputStream in = Files.newInputStream(temp)) {
                BodyContentHandler handler = new BodyContentHandler(MAX_CHARS);
                Metadata metadata = new Metadata();
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());
                parser.parse(in, handler, metadata, new ParseContext());
                String text = handler.toString();
                if (text == null || text.isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Could not extract text from file (empty or unsupported)");
                }
                return text.trim();
            }
        } catch (ApiException e) {
            throw e;
        } catch (TikaException | SAXException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not parse file: " + e.getMessage());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read uploaded file");
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
