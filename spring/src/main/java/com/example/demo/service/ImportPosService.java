package com.example.demo.service;

import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigReader;
import com.example.demo.model.PosConfig;
import com.example.demo.model.PosDraft;
import org.springframework.stereotype.Service;

import java.io.InputStream;
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
    public PosDraft importExcel(InputStream in) throws Exception {
        PosConfig config = reader.read(in);
        String draftId = "dft_" + UUID.randomUUID();
        PosDraft draft = new PosDraft(draftId, config);
        draftRepository.save(draft);
        return draft;
    }
}
