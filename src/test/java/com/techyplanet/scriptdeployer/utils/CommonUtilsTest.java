package com.techyplanet.scriptdeployer.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

public class CommonUtilsTest {

    @TempDir
    Path tmp;

    @Test
    public void getFileSequence_parsesSequence_whenApplicable() {
        String pattern = "S_(\\d+_)?.*"; // not used directly; use API that assumes applicable=true
        assertEquals(Long.valueOf(42L), CommonUtils.getFileSequence("S_(\\d+)_.*", "S_42_any.sql"));
        assertEquals(Long.valueOf(0L), CommonUtils.getFileSequence("S_(\\d+)_.*", "random.sql"));
    }

    @Test
    public void getFileSequence_returnsZero_whenSeqNotApplicable() {
        assertEquals(Long.valueOf(0L), CommonUtils.getFileSequence("R_(\\d+)_.*", "R_5_view.sql", false));
        assertEquals(Long.valueOf(0L), CommonUtils.getFileSequence("RA_(\\d+)_.*", "any.sql", false));
    }

    @Test
    public void sorter_ordersBySequenceThenName_andDisallowsSameSeqWhenRequested() throws Exception {
        Path aPath = tmp.resolve("S_1_a.sql");
        Path bPath = tmp.resolve("S_2_b.sql");
        Files.createFile(aPath);
        Files.createFile(bPath);
        File a = aPath.toFile();
        File b = bPath.toFile();

        Comparator<File> comp = CommonUtils.scriptPrioritySorter("S_(\\d+)_.*", true, false);
        File[] sorted = Arrays.asList(b, a).stream().sorted(comp).toArray(File[]::new);
        assertEquals("S_1_a.sql", sorted[0].getName());
        assertEquals("S_2_b.sql", sorted[1].getName());

        // duplicate sequence must throw when sameSequenceAllowed=false
        Path a2Path = tmp.resolve("S_1_b.sql");
        Files.createFile(a2Path);
        File a2 = a2Path.toFile();
        assertThrows(RuntimeException.class,
                () -> Arrays.asList(a, a2).stream().sorted(comp).toArray(File[]::new));
    }

    @Test
    public void sorter_disallowsSameNameEvenAcrossDirs() throws Exception {
        Path dir1 = Files.createDirectory(tmp.resolve("dir1"));
        Path dir2 = Files.createDirectory(tmp.resolve("dir2"));
        Path f1Path = Files.createFile(dir1.resolve("S_1_same.sql"));
        Path f2Path = Files.createFile(dir2.resolve("S_1_same.sql"));
        File f1 = f1Path.toFile();
        File f2 = f2Path.toFile();

        assertThrows(RuntimeException.class, () ->
                Arrays.asList(f1, f2).stream()
                        .sorted(CommonUtils.scriptPrioritySorter("S_(\\d+)_.*", true, true))
                        .toArray(File[]::new));
    }

    @Test
    public void generateFileChecksum_md5Deterministic() throws Exception {
        Path fPath = Files.createFile(tmp.resolve("data.txt"));
        try (FileWriter fw = new FileWriter(fPath.toFile())) {
            fw.write("hello world\n");
        }
        Path p = fPath;
        String c1 = CommonUtils.generateFileChecksum(p);
        String c2 = CommonUtils.generateFileChecksum(p);
        assertNotNull(c1);
        assertEquals(c1, c2);
        assertTrue(c1.matches("[A-F0-9]{32}"));
    }
}
