package com.zeynepneda.userdata;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

class FileService {

    public void appendToFile(File file, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file, true); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            bos.write(data);
        }
    }

    public void overwriteFile(File file, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file, false); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            bos.write(data);
        }
    }

    public byte[] readFile(File file) throws IOException {
        if (!file.exists()) {
            return new byte[0];
        }
        try (FileInputStream fis = new FileInputStream(file); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    public byte[] readFromOffset(File file, int offset, int length) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);
            byte[] data = new byte[length];
            raf.readFully(data);
            return data;
        }
    }

    public void writeAtOffset(File file, int offset, byte[] data) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(offset);
            raf.write(data);
        }
    }

    public long getFileLength(File file) {
        return file.exists() ? file.length() : 0;
    }

    public DataOutputStream getDataOutputStream(File file, boolean append) throws IOException {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, append)));
    }

    public DataInputStream getDataInputStream(File file) throws IOException {
        return new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
    }

    public RandomAccessFile getRandomAccessFile(File file, String mode) throws IOException {
        return new RandomAccessFile(file, mode);
    }
}
