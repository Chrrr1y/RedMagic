package dev.lackluster.redmagichelper.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Bounded, app-private storage for events collected from hooked processes. */
public final class HookRuntimeLogStore {
    private static final String LOG_FILE = "hook-runtime.log";
    private static final String BACKUP_FILE = "hook-runtime.log.1";
    private static final long MAX_FILE_BYTES = 1024L * 1024L;
    private static final int MAX_LINES = 1200;
    private static final int MAX_LINE_LENGTH = 4096;
    private static final int MAX_RETURN_CHARS = 240_000;

    private HookRuntimeLogStore() {
    }

    public static synchronized void append(Context context, List<String> lines) throws Exception {
        if (context == null || lines == null || lines.isEmpty()) {
            return;
        }
        File file = new File(context.getFilesDir(), LOG_FILE);
        long incomingBytes = 0;
        for (String line : lines) {
            if (line != null) {
                incomingBytes += Math.min(line.length(), MAX_LINE_LENGTH) + 1L;
            }
        }
        if (file.exists() && file.length() + incomingBytes > MAX_FILE_BYTES) {
            File backup = new File(context.getFilesDir(), BACKUP_FILE);
            if (backup.exists() && !backup.delete()) {
                throw new IllegalStateException("cannot rotate previous hook log");
            }
            if (file.exists() && !file.renameTo(backup)) {
                throw new IllegalStateException("cannot rotate hook log");
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                if (line != null) {
                    writer.write(cleanLine(line));
                    writer.newLine();
                }
            }
        }
    }

    public static synchronized ArrayList<String> readRecent(Context context) throws Exception {
        ArrayDeque<String> recent = new ArrayDeque<>(MAX_LINES);
        int[] charCount = {0};
        if (context == null) {
            return new ArrayList<>();
        }
        readFile(new File(context.getFilesDir(), BACKUP_FILE), recent, charCount);
        readFile(new File(context.getFilesDir(), LOG_FILE), recent, charCount);
        return new ArrayList<>(recent);
    }

    public static synchronized boolean clear(Context context) {
        if (context == null) {
            return false;
        }
        boolean current = deleteIfPresent(new File(context.getFilesDir(), LOG_FILE));
        boolean backup = deleteIfPresent(new File(context.getFilesDir(), BACKUP_FILE));
        return current && backup;
    }

    private static void readFile(
            File file,
            ArrayDeque<String> recent,
            int[] charCount
    ) throws Exception {
        if (!file.isFile()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > MAX_RETURN_CHARS) {
                    line = line.substring(line.length() - MAX_RETURN_CHARS);
                }
                while (!recent.isEmpty()
                        && (recent.size() >= MAX_LINES
                        || charCount[0] + line.length() > MAX_RETURN_CHARS)) {
                    charCount[0] -= recent.removeFirst().length();
                }
                recent.addLast(line);
                charCount[0] += line.length();
            }
        }
    }

    private static boolean deleteIfPresent(File file) {
        return !file.exists() || file.delete();
    }

    private static String cleanLine(String line) {
        String clean = line.replace('\r', ' ').replace('\n', ' ');
        return clean.length() <= MAX_LINE_LENGTH
                ? clean : clean.substring(0, MAX_LINE_LENGTH);
    }
}
