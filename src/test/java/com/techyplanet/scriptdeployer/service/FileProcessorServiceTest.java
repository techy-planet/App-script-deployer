package com.techyplanet.scriptdeployer.service;

import com.techyplanet.scriptdeployer.component.AppSettings;
import com.techyplanet.scriptdeployer.entity.ScriptHistory;
import com.techyplanet.scriptdeployer.repository.ScriptHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class FileProcessorServiceTest {

    @TempDir
    Path tmp;

    private AppSettings settings;
    private InMemoryScriptHistoryRepository repo;
    private FileProcessorService service;

    @BeforeEach
    public void setup() throws Exception {
        settings = new AppSettings();
        ReflectionTestUtils.setField(settings, "filePatternDelimiter", ",");
        ReflectionTestUtils.setField(settings, "oneTimeFilePattern", "S_<seq_num>_.+\\.sql");
        ReflectionTestUtils.setField(settings, "repeatableFilePattern", "R_<seq_num>_.+\\.sql");
        ReflectionTestUtils.setField(settings, "preRunFilePattern", "RA_.+\\.sql");
        ReflectionTestUtils.setField(settings, "postRunFilePattern", "");
        ReflectionTestUtils.setField(settings, "filePatternConflict", "error");
        ReflectionTestUtils.setField(settings, "consoleCommand", "echo <script>");
        ReflectionTestUtils.setField(settings, "consoleCommandLogging", "<script>");
        ReflectionTestUtils.setField(settings, "consoleCommandOutputEnabled", true);
        ReflectionTestUtils.setField(settings, "stopOnScriptFail", false);
        ReflectionTestUtils.setField(settings, "validateScriptFileSize", false);
        ReflectionTestUtils.setField(settings, "fileModifyError", "reset-hash");
        ReflectionTestUtils.setField(settings, "logSkipScriptEnabled", true);
        ReflectionTestUtils.setField(settings, "scriptVariables", "");
        ReflectionTestUtils.setField(settings, "logDir", tmp.toAbsolutePath().toString());
        ReflectionTestUtils.setField(settings, "reqNumber", "REQ-1");

        repo = new InMemoryScriptHistoryRepository();

        service = new FileProcessorService();
        ReflectionTestUtils.setField(service, "appSettings", settings);
        ReflectionTestUtils.setField(service, "scriptVariablesSubstitutor", new org.apache.commons.text.StringSubstitutor(Collections.emptyMap()));
        ReflectionTestUtils.setField(service, "scriptHistoryRepository", repo);
    }

    @Test
    public void oneTime_firstRun_createsEntries_thenModify_resetsHashIncrementsVersion() throws Exception {
        Path dirPath = Files.createDirectory(tmp.resolve("onetime"));
        File dir = dirPath.toFile();
        File s1 = new File(dir, "S_1_alpha.sql");
        File s2 = new File(dir, "S_2_beta.sql");
        write(s1, "create table a(id int);");
        write(s2, "create table b(id int);");

        service.processOneTimeFiles(dir);
        assertEquals(2, repo.count());
        ScriptHistory h1 = repo.findFirstByFileIdPathOrderByFileIdUpdateDateDesc(rel(dir, s1));
        ScriptHistory h2 = repo.findFirstByFileIdPathOrderByFileIdUpdateDateDesc(rel(dir, s2));
        assertEquals(Long.valueOf(1L), h1.getVersion());
        assertEquals(Long.valueOf(1L), h2.getVersion());

        // modify S_1 content -> should create new entry with version+1 due to reset-hash
        Thread.sleep(5); // ensure updateDate differs
        write(s1, "create table a(id int, v int);");
        service.processOneTimeFiles(dir);

        ScriptHistory h1new = repo.findFirstByFileIdPathOrderByFileIdUpdateDateDesc(rel(dir, s1));
        assertEquals(Long.valueOf(2L), h1new.getVersion());
        assertEquals("S_1_alpha.sql", new File(h1new.getFileId().getPath()).getName());
        // S_2 should remain at version 1
        ScriptHistory h2new = repo.findFirstByFileIdPathOrderByFileIdUpdateDateDesc(rel(dir, s2));
        assertEquals(Long.valueOf(1L), h2new.getVersion());
    }

    @Test
    public void repeatable_firstRun_creates_thenReRunWithChange_incrementsVersionOnlyOnChange() throws Exception {
        Path dirPath = Files.createDirectory(tmp.resolve("repeatable"));
        File dir = dirPath.toFile();
        File r1 = new File(dir, "R_10_view.sql");
        File r2 = new File(dir, "R_20_fn.sql");
        write(r1, "create view v as select 1;");
        write(r2, "create function f() returns int as $$ select 1; $$ language sql;");

        service.processRepeatableFiles(dir);
        assertEquals(2, repo.count());
        ScriptHistory hr1 = repo.findFirstByFileIdPathOrderByFileIdUpdateDateDesc(rel(dir, r1));
        ScriptHistory hr2 = repo.findFirstByFileIdPathOrderByFileIdUpdateDateDesc(rel(dir, r2));
        assertEquals(Long.valueOf(1L), hr1.getVersion());
        assertEquals(Long.valueOf(1L), hr2.getVersion());

        // change only r2
        Thread.sleep(5);
        write(r2, "create function f() returns int as $$ select 2; $$ language sql;");
        service.processRepeatableFiles(dir);

        ScriptHistory hr1new = repo.findFirstByFileIdPathOrderByFileIdUpdateDateDesc(rel(dir, r1));
        ScriptHistory hr2new = repo.findFirstByFileIdPathOrderByFileIdUpdateDateDesc(rel(dir, r2));
        assertEquals(Long.valueOf(1L), hr1new.getVersion());
        assertEquals(Long.valueOf(2L), hr2new.getVersion());
    }

    @Test
    public void runAllTime_prePattern_runsEveryTime_andIncrementsVersionOnlyIfChanged() throws Exception {
        Path dirPath = Files.createDirectory(tmp.resolve("runall"));
        File dir = dirPath.toFile();
        File ra = new File(dir, "RA_hello.sql");
        write(ra, "select 1;");

        // first run -> create
        service.processPreRunFiles(dir);
        ScriptHistory first = repo.findFirstByFileIdPathOrderByFileIdUpdateDateDesc(rel(dir, ra));
        assertNotNull(first);
        assertEquals(Long.valueOf(1L), first.getVersion());

        // second run no change -> version stays same (but new entry written)
        Thread.sleep(5);
        service.processPreRunFiles(dir);
        ScriptHistory second = repo.findFirstByFileIdPathOrderByFileIdUpdateDateDesc(rel(dir, ra));
        assertNotNull(second);
        assertEquals(Long.valueOf(1L), second.getVersion());

        // third run with change -> version increments
        Thread.sleep(5);
        write(ra, "select 2;");
        service.processPreRunFiles(dir);
        ScriptHistory third = repo.findFirstByFileIdPathOrderByFileIdUpdateDateDesc(rel(dir, ra));
        assertEquals(Long.valueOf(2L), third.getVersion());
    }

    private static String rel(File root, File f) {
        return root.toURI().relativize(f.toURI()).getPath();
    }

    private static void write(File f, String content) throws Exception {
        try (FileWriter fw = new FileWriter(f)) { fw.write(content); }
    }

    private static class InMemoryScriptHistoryRepository implements ScriptHistoryRepository {
        private final Map<String, List<ScriptHistory>> byPath = new ConcurrentHashMap<>();

        @Override
        public ScriptHistory findBySequenceAndPattern(Long sequence, String pattern) {
            return byPath.values().stream()
                    .flatMap(List::stream)
                    .filter(h -> Objects.equals(h.getSequence(), sequence) && pattern.equals(h.getPattern()))
                    .max(Comparator.comparing(h -> h.getFileId().getUpdateDate()))
                    .orElse(null);
        }

        @Override
        public ScriptHistory findFirstByFileIdPathOrderByFileIdUpdateDateDesc(String path) {
            List<ScriptHistory> list = byPath.get(path);
            if (list == null || list.isEmpty()) return null;
            return list.stream().max(Comparator.comparing(h -> h.getFileId().getUpdateDate())).orElse(null);
        }

        @Override
        public <S extends ScriptHistory> S save(S entity) {
            byPath.computeIfAbsent(entity.getFileId().getPath(), k -> new ArrayList<>()).add(entity);
            return entity;
        }

        // --- below are unneeded for tests; provide minimal implementations ---
        @Override public <S extends ScriptHistory> Iterable<S> saveAll(Iterable<S> entities) { for (S e : entities) save(e); return entities; }
        @Override public Optional<ScriptHistory> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public Iterable<ScriptHistory> findAll() { return byPath.values().stream().flatMap(List::stream)::iterator; }
        @Override public Iterable<ScriptHistory> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return byPath.values().stream().mapToLong(List::size).sum(); }
        @Override public void deleteById(Long aLong) { }
        @Override public void delete(ScriptHistory entity) { }

        @Override
        public void deleteAllById(Iterable<? extends Long> longs) { }

        @Override public void deleteAll(Iterable<? extends ScriptHistory> entities) { }
        @Override public void deleteAll() { byPath.clear(); }
    }
}
