package com.example.demo.service;

import com.example.demo.model.PosDraft;
import com.example.demo.service.exception.NotFoundException;
import com.example.demo.service.port.DraftRepository;
import com.example.demo.service.port.PosConfigExporter;
import org.springframework.stereotype.Service;

@Service
public class ExportPosService implements ExportPosUseCase {

    private final DraftRepository draftRepository;
    private final PosConfigExporter exporter;

    public ExportPosService(DraftRepository draftRepository, PosConfigExporter exporter) {
        this.draftRepository = draftRepository;
        this.exporter = exporter;
    }

    @Override
    public byte[] exportExcel(String draftId) {
        PosDraft draft = draftRepository
                .findById(draftId)
                .orElseThrow(() -> new NotFoundException("draft not found: " + draftId));
        try {
            return exporter.export(draft.getOriginalExcelBytes(), draft.getConfig());
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("failed to export excel", ex);
        }
    }
}
