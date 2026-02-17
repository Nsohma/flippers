package com.example.demo.dao;

import com.example.demo.model.PosDraft;
import com.example.demo.service.port.DraftRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDraftRepository implements DraftRepository {
    private static final Logger log = LoggerFactory.getLogger(InMemoryDraftRepository.class);

    private final ConcurrentHashMap<String, PosDraft> store = new ConcurrentHashMap<>();
    private final Path storageDir;

    public InMemoryDraftRepository(
            @Value("${draft.storage.dir:${java.io.tmpdir}/flippers-drafts}") String storageDirPath
    ) {
        this.storageDir = Paths.get(storageDirPath);
        initializeStorage();
        loadExistingDrafts();
    }

    @Override
    public void save(PosDraft draft) {
        store.put(draft.getDraftId(), draft);
        writeDraftFile(draft);
    }

    @Override
    public Optional<PosDraft> findById(String draftId) {
        PosDraft cached = store.get(draftId);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<PosDraft> loaded = readDraftFile(draftPath(draftId));
        loaded.ifPresent(d -> store.put(d.getDraftId(), d));
        return loaded;
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(storageDir);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to create draft storage directory: " + storageDir, ex);
        }
    }

    private void loadExistingDrafts() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDir, "*.ser")) {
            for (Path path : stream) {
                Optional<PosDraft> draft = readDraftFile(path);
                draft.ifPresent(d -> store.put(d.getDraftId(), d));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("failed to load drafts from storage: " + storageDir, ex);
        }
    }

    private void writeDraftFile(PosDraft draft) {
        Path target = draftPath(draft.getDraftId());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");

        try (ObjectOutputStream out = new ObjectOutputStream(
                Files.newOutputStream(
                        temp,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                )
        )) {
            out.writeObject(draft);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to write draft file: " + target, ex);
        }

        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception moveEx) {
                throw new IllegalStateException("failed to move draft file: " + target, moveEx);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("failed to move draft file: " + target, ex);
        }
    }

    private Optional<PosDraft> readDraftFile(Path path) {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            Object obj = in.readObject();
            if (obj instanceof PosDraft draft) {
                return Optional.of(draft);
            }
            log.warn("Skip non-draft file: {}", path);
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Skip unreadable draft file: {}", path, ex);
            return Optional.empty();
        }
    }

    private Path draftPath(String draftId) {
        return storageDir.resolve(sanitize(draftId) + ".ser");
    }

    private static String sanitize(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
