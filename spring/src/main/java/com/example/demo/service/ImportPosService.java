package com.example.demo.service;

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
        PosConfig config = readConfig(excelBytes);
        String draftId = "dft_" + UUID.randomUUID();
        PosDraft draft = new PosDraft(draftId, config, excelBytes);
        draftRepository.save(draft);
        return draft;
    }

    private PosConfig readConfig(byte[] excelBytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(excelBytes)) {
            PosConfigSource source = reader.read(in);
            return PosConfig.fromSource(source);
        } catch (IllegalArgumentException ex) {
            throw new InvalidPosExcelException(ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new InvalidPosExcelException("failed to parse excel file", ex);
        }
    }
}
