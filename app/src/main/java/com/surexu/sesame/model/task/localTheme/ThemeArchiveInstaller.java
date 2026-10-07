package com.surexu.sesame.model.task.localTheme;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ThemeArchiveInstaller {
    private static final long MAX_BYTES = 33554432;
    private static final int MAX_FILES = 512;

    public interface Validity {
        boolean isCurrent();
    }

    private ThemeArchiveInstaller() {
    }

    public static final class Preview {
        public final long bytes;
        public final int replacements;

        private Preview(int i, long j) {
            this.replacements = i;
            this.bytes = j;
        }
    }

    public static synchronized Preview preview(File file, String str, File file2) throws IOException {
        Preview previewValidate;
        File fileTarget = target(file, str);
        ZipFile zipFile = new ZipFile(file2);
        try {
            previewValidate = validate(zipFile, fileTarget);
            zipFile.close();
        } catch (Throwable th) {
            try {
                zipFile.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
        return previewValidate;
    }

    public static synchronized Preview apply(File file, String str, File file2, Validity validity) throws IOException {
        Preview previewValidate;
        File fileTarget = target(file, str);
        File fileBackup = backup(file, str);
        if (fileBackup.exists()) {
            throw new IOException("backup already exists; restore before applying again");
        }
        File fileTemporaryDirectory = temporaryDirectory(file);
        boolean z = false;
        Throwable th = null;
        try {
            ZipFile zipFile = new ZipFile(file2);
            try {
                previewValidate = validate(zipFile, fileTarget);
                copyTree(fileTarget, fileTemporaryDirectory, new Budget(), 0);
                Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
                while (enumerationEntries.hasMoreElements()) {
                    ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
                    File fileEntryFile = entryFile(fileTarget, zipEntryNextElement.getName());
                    if (!zipEntryNextElement.isDirectory() && fileEntryFile.isFile()) {
                        File fileEntryFile2 = entryFile(fileTemporaryDirectory, zipEntryNextElement.getName());
                        InputStream inputStream = zipFile.getInputStream(zipEntryNextElement);
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(fileEntryFile2);
                            try {
                                copy(inputStream, fileOutputStream, new Budget());
                                fileOutputStream.getFD().sync();
                                fileOutputStream.close();
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                            } catch (Throwable thInner) {
                                try {
                                    fileOutputStream.close();
                                } catch (Throwable th2) {
                                    thInner.addSuppressed(th2);
                                }
                                throw thInner;
                            }
                        } catch (Throwable th3) {
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (Throwable th4) {
                                    th3.addSuppressed(th4);
                                }
                            }
                            throw th3;
                        }
                    }
                }
                if (!validity.isCurrent() || Thread.currentThread().isInterrupted()) {
                    throw new IOException("operation cancelled");
                }
                if (!fileTarget.renameTo(fileBackup)) {
                    throw new IOException("cannot preserve original theme");
                }
                try {
                    if (!fileTemporaryDirectory.renameTo(fileTarget)) {
                        throw new IOException("cannot activate staged theme");
                    }
                    zipFile.close();
                    deleteTree(fileTemporaryDirectory);
                } catch (Throwable th5) {
                    z = true;
                    try {
                        zipFile.close();
                    } catch (Throwable th6) {
                        th5.addSuppressed(th6);
                    }
                    throw th5;
                }
            } catch (Throwable th7) {
                zipFile.close();
                throw th7;
            }
        } catch (Throwable th8) {
            if (z && !fileBackup.renameTo(fileTarget)) {
                throw new IOException("activation failed; original preserved in backup");
            }
            deleteTree(fileTemporaryDirectory);
            throw th8;
        }
        return previewValidate;
    }

    public static synchronized void restore(File file, String str, Validity validity) throws IOException {
        boolean z;
        checkId(str);
        safeDirectory(file);
        File fileChild = child(file, str);
        File fileBackup = backup(file, str);
        safeDirectory(fileBackup);
        if (fileChild.exists()) {
            safeDirectory(fileChild);
        }
        if (!validity.isCurrent() || Thread.currentThread().isInterrupted()) {
            throw new IOException("operation cancelled");
        }
        File fileTemporaryDirectory = temporaryDirectory(file);
        if (!fileTemporaryDirectory.delete()) {
            throw new IOException("cannot reserve restore path");
        }
        try {
            if (!fileChild.exists()) {
                z = false;
            } else {
                if (!fileChild.renameTo(fileTemporaryDirectory)) {
                    throw new IOException("cannot stage current theme");
                }
                z = true;
            }
            if (!fileBackup.renameTo(fileChild)) {
                if (z && !fileTemporaryDirectory.renameTo(fileChild)) {
                    throw new IOException("restore failed; both copies preserved");
                }
                throw new IOException("cannot restore original theme");
            }
            deleteTree(fileTemporaryDirectory);
        } catch (IOException e) {
            throw e;
        }
    }

    private static Preview validate(ZipFile zipFile, File file) throws IOException {
        HashSet hashSet = new HashSet();
        Budget budget = new Budget();
        Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
        int i = 0;
        int i2 = 0;
        while (enumerationEntries.hasMoreElements()) {
            ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
            i2++;
            if (i2 > 512) {
                throw new IOException("too many archive entries");
            }
            File fileEntryFile = entryFile(file, zipEntryNextElement.getName());
            if (!hashSet.add(fileEntryFile.getAbsolutePath())) {
                throw new IOException("duplicate archive entry");
            }
            if (!zipEntryNextElement.isDirectory()) {
                if (zipEntryNextElement.getSize() > MAX_BYTES) {
                    throw new IOException("archive entry too large");
                }
                InputStream inputStream = zipFile.getInputStream(zipEntryNextElement);
                try {
                    copy(inputStream, null, budget);
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (fileEntryFile.isFile()) {
                        i++;
                    } else if (fileEntryFile.exists()) {
                        throw new IOException("archive file conflicts with directory");
                    }
                } catch (Throwable th) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            }
        }
        if (i == 0) {
            throw new IOException("no matching installed theme resources");
        }
        return new Preview(i, budget.bytes);
    }

    private static File target(File file, String str) throws IOException {
        checkId(str);
        safeDirectory(file);
        File fileChild = child(file, str);
        safeDirectory(fileChild);
        return fileChild;
    }

    private static void checkId(String str) throws IOException {
        if (str == null || !str.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}") || str.equals("temp_skin")) {
            throw new IOException("invalid theme ID");
        }
    }

    private static File backup(File file, String str) throws IOException {
        return child(file, ".sesame-backup-" + str);
    }

    private static File temporaryDirectory(File file) throws IOException {
        File fileCreateTempFile = File.createTempFile(".sesame-stage-", "", file);
        if (fileCreateTempFile.delete() && fileCreateTempFile.mkdir()) {
            return fileCreateTempFile;
        }
        throw new IOException("cannot create staging directory");
    }

    private static File child(File file, String str) throws IOException {
        File absoluteFile = new File(file, str).getAbsoluteFile();
        if (absoluteFile.equals(absoluteFile.getCanonicalFile())) {
            return absoluteFile;
        }
        throw new IOException("symbolic or noncanonical path");
    }

    private static File entryFile(File file, String str) throws IOException {
        if (str == null || str.isEmpty() || str.startsWith("/") || str.contains("\\") || str.contains(":")) {
            throw new IOException("invalid archive path");
        }
        if (str.endsWith("/")) {
            str = str.substring(0, str.length() - 1);
        }
        String[] strArrSplit = str.split("/", -1);
        if (strArrSplit.length > 12) {
            throw new IOException("archive nesting too deep");
        }
        for (String str2 : strArrSplit) {
            if (str2.isEmpty() || str2.equals(".") || str2.equals("..")) {
                throw new IOException("archive path traversal");
            }
        }
        File fileChild = child(file, str);
        if (fileChild.getPath().startsWith(file.getAbsolutePath() + File.separator)) {
            return fileChild;
        }
        throw new IOException("path outside theme");
    }

    private static void safeDirectory(File file) throws IOException {
        if (!file.isDirectory() || !file.getAbsoluteFile().equals(file.getCanonicalFile())) {
            throw new IOException("theme directory missing or symbolic");
        }
    }

    private static void copyTree(File file, File file2, Budget budget, int i) throws IOException {
        safeDirectory(file);
        if (i > 12) {
            throw new IOException("installed theme nesting too deep");
        }
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            throw new IOException("cannot list installed theme");
        }
        for (File file3 : fileArrListFiles) {
            int i2 = budget.files + 1;
            budget.files = i2;
            if (i2 > 512) {
                throw new IOException("installed theme too large");
            }
            if (!file3.getAbsoluteFile().equals(file3.getCanonicalFile())) {
                throw new IOException("symbolic theme resource");
            }
            File file4 = new File(file2, file3.getName());
            if (file3.isDirectory()) {
                if (!file4.mkdir()) {
                    throw new IOException("cannot create resource directory");
                }
                copyTree(file3, file4, budget, i + 1);
            } else if (file3.isFile()) {
                FileInputStream fileInputStream = new FileInputStream(file3);
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file4);
                    try {
                        copy(fileInputStream, fileOutputStream, budget);
                        fileOutputStream.close();
                        fileInputStream.close();
                    } catch (Throwable th) {
                        try {
                            fileOutputStream.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    try {
                        fileInputStream.close();
                    } catch (Throwable th4) {
                        th3.addSuppressed(th4);
                    }
                    throw th3;
                }
            } else {
                throw new IOException("unsupported theme entry");
            }
        }
    }

    private static void copy(InputStream inputStream, OutputStream outputStream, Budget budget) throws IOException {
        byte[] bArr = new byte[8192];
        while (true) {
            int i = inputStream.read(bArr);
            if (i == -1) {
                return;
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("cancelled");
            }
            budget.bytes += (long) i;
            if (budget.bytes > MAX_BYTES) {
                throw new IOException("decompressed data too large");
            }
            if (outputStream != null) {
                outputStream.write(bArr, 0, i);
            }
        }
    }

    private static void deleteTree(File file) throws IOException {
        if (file.exists()) {
            if (!file.getAbsoluteFile().equals(file.getCanonicalFile())) {
                throw new IOException("refuse symbolic cleanup");
            }
            File[] fileArrListFiles = file.listFiles();
            if (fileArrListFiles != null) {
                for (File file2 : fileArrListFiles) {
                    deleteTree(file2);
                }
            }
            if (!file.delete()) {
                throw new IOException("cannot remove staging resource");
            }
        }
    }

    private static final class Budget {
        long bytes;
        int files;

        private Budget() {
        }
    }
}
