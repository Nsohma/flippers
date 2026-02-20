package com.example.demo.service;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.ItemMasterCatalog;
import com.example.demo.model.PosConfig;
import com.example.demo.model.PosConfigSource;
import com.example.demo.model.PosDraft;
import com.example.demo.service.command.ImportPosCommand;
import com.example.demo.service.exception.InvalidPosExcelException;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
public class ImportPosService implements ImportPosUseCase {

    private final PosConfigReader reader;
    private final DraftRepository draftRepository;

    public ImportPosService(PosConfigReader reader, DraftRepository draftRepository) {
        this.reader = reader;
        this.draftRepository = draftRepository;
    }

    @Override
    public PosDraft importExcel(ImportPosCommand command) {
        byte[] excelBytes = command.excelBytes();
        ParsedDraft parsedDraft = parseDraft(excelBytes);
        String draftId = "dft_" + UUID.randomUUID();
        PosDraft draft = new PosDraft(
                draftId,
                parsedDraft.config(),
                excelBytes,
                parsedDraft.itemCatalog(),
                parsedDraft.handyCatalog(),
                parsedDraft.itemMasterCatalog(),
                "インポート"
        );
        draftRepository.save(draft);
        return draft;
    }

    private ParsedDraft parseDraft(byte[] excelBytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(excelBytes)) {
            PosConfigSource source = reader.read(in);
            PosConfig config = PosConfig.fromSource(source);
            return new ParsedDraft(
                    config,
                    source.getItemCatalog(),
                    source.getHandyCatalog(),
                    source.getItemMasterCatalog()
            );
        } catch (IllegalArgumentException ex) {
            throw new InvalidPosExcelException(ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new InvalidPosExcelException("failed to parse excel file", ex);
        }
    }

    private record ParsedDraft(
            PosConfig config,
            ItemCatalog itemCatalog,
            ItemCatalog handyCatalog,
            ItemMasterCatalog itemMasterCatalog
    ) {
    }
}
