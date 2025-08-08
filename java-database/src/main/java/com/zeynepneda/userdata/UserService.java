package com.zeynepneda.userdata;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

class UserService {

    private int nextUserId = 1;
    private final String INDEX_ID_FILE = "index_by_id.idx";
    private final String INDEX_NAME_FILE = "index_by_name.idx";
    private final String LAST_ID_FILE = "last_id.dat";

    void findUser(Object input, File file) throws IOException {

        if (input instanceof Integer) {
            int id = (Integer) input;
            int offset = findById(id);
            if (offset != -1) {
                printUserAtOffset(file, offset);
            } else {
                System.out.println("User not found with ID: " + id);
            }
        } else if (input instanceof String) {
            String username = (String) input;
            int offset = findByName(username);
            if (offset != -1) {
                printUserAtOffset(file, offset);
            } else {
                System.out.println("User not found with username: " + username);
            }
        } else {
            System.out.println("Unsupported input type.");
        }
    }

    private void printUserAtOffset(File file, int offset) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);

            int nameLength = raf.readUnsignedByte();
            byte[] nameBytes = new byte[nameLength];
            raf.readFully(nameBytes);
            String username = new String(nameBytes, "UTF-8");

            int id = raf.readInt();

            int emailLength = raf.readUnsignedByte();
            byte[] emailBytes = new byte[emailLength];
            raf.readFully(emailBytes);
            String email = new String(emailBytes, "UTF-8");

            System.out.println("User Found:");
            System.out.println("Username: " + username);
            System.out.println("ID: " + id);
            System.out.println("Email: " + email);
        }
    }

    private int findById(int id) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(INDEX_ID_FILE, "r")) {
            long recordSize = 8;
            long low = 0;
            long high = raf.length() / recordSize - 1;
            while (low <= high) {
                long mid = (low + high) / 2;
                raf.seek(mid * recordSize);

                int currentId = raf.readInt();
                int offset = raf.readInt();

                if (currentId == id) {
                    return offset;
                } else if (currentId < id) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
        }
        return -1;
    }

    private int findByName(String targetName) throws IOException {
        try (DataInputStream din = new DataInputStream(new FileInputStream(INDEX_NAME_FILE))) {
            while (din.available() > 0) {
                int nameLength = din.readUnsignedByte();
                byte[] nameBytes = new byte[nameLength];
                din.readFully(nameBytes);
                String currentName = new String(nameBytes, "UTF-8");

                int offset = din.readInt();

                int compare = currentName.compareTo(targetName);
                if (compare == 0) {
                    return offset;
                } else if (compare > 0) {
                    break;
                }
            }
        }
        return -1;
    }

    void addUser(String username, String email, File file) throws IOException {
        nextUserId = readLastId();

        User user = new User(username, nextUserId, email);
        long offset = file.length();

        try (DataOutputStream dou = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, true)))) {
            byte[] nameBytes = user.getUsername().getBytes("UTF-8");
            byte[] emailBytes = user.getEmail().getBytes("UTF-8");

            if (nameBytes.length > 255) {
                throw new IllegalArgumentException("Username length is too long");
            }

            dou.writeByte(nameBytes.length);
            dou.write(nameBytes);

            dou.writeInt(user.getId());

            dou.writeByte(emailBytes.length);
            dou.write(emailBytes);
        }

        try (DataOutputStream idIndex = new DataOutputStream(new FileOutputStream(INDEX_ID_FILE, true))) {
            idIndex.writeInt(nextUserId);
            idIndex.writeInt((int) offset);
        }

        try (DataOutputStream nameIndex = new DataOutputStream(new FileOutputStream(INDEX_NAME_FILE, true))) {
            byte[] nameBytes = username.getBytes("UTF-8");
            nameIndex.writeByte(nameBytes.length);
            nameIndex.write(nameBytes);
            nameIndex.writeInt((int) offset);
        }

        System.out.println("User added: " + username + " \nID: " + nextUserId);

        nextUserId++;
        saveLastId(nextUserId);
    }

    private int readLastId() {
        File f = new File(LAST_ID_FILE);
        if (!f.exists() || f.length() == 0) {
            return 1;
        }
        try (DataInputStream din = new DataInputStream(new FileInputStream(f))) {
            return din.readInt();
        } catch (IOException e) {
            return 1;
        }
    }

    private void saveLastId(int lastId) {
        try (DataOutputStream dout = new DataOutputStream(new FileOutputStream(LAST_ID_FILE, false))) {
            dout.writeInt(lastId);
        } catch (IOException e) {
        }
    }

    void editUser(int targetId, String newUsername, String newEmail, File file) throws IOException {
        int oldOffset = findById(targetId);
        if (oldOffset == -1) {
            System.out.println("User not found with ID: " + targetId);
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(oldOffset);
            raf.writeByte(0);
        }
        long newOffset = file.length();
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file, true))) {
            byte[] nameBytes = newUsername.getBytes("UTF-8");
            byte[] emailBytes = newEmail.getBytes("UTF-8");

            out.writeByte(nameBytes.length);
            out.write(nameBytes);
            out.writeInt(targetId);
            out.writeByte(emailBytes.length);
            out.write(emailBytes);
        }

        updateIdIndex(targetId, (int) newOffset);
        updateNameIndex(newUsername, (int) newOffset);

        System.out.println("User with ID " + targetId + " updated");
    }

    private void updateIdIndex(int targetId, int newOffset) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(INDEX_ID_FILE, "rw")) {
            while (raf.getFilePointer() < raf.length()) {
                int id = raf.readInt();
                long pos = raf.getFilePointer();
                int offset = raf.readInt();
                if (id == targetId) {
                    raf.seek(pos);
                    raf.writeInt(newOffset);
                    return;
                }
            }
        }
    }

    private void updateNameIndex(String newUsername, int newOffset) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(INDEX_NAME_FILE, "rw")) {
            while (raf.getFilePointer() < raf.length()) {
                int nameLen = raf.readUnsignedByte();
                byte[] nameBytes = new byte[nameLen];
                raf.readFully(nameBytes);
                long pos = raf.getFilePointer();
                int offset = raf.readInt();

                String currentName = new String(nameBytes, "UTF-8");
                if (currentName.equals(newUsername)) {
                    raf.seek(pos);
                    raf.writeInt(newOffset);
                    return;
                }
            }
            byte[] nameBytes = newUsername.getBytes("UTF-8");
            raf.seek(raf.length());
            raf.writeByte(nameBytes.length);
            raf.write(nameBytes);
            raf.writeInt(newOffset);
        }
    }

    void deleteUser(int targetId, File file) throws IOException {

    }

    public static int getNextId(File file) throws IOException {
        int lastId = 0;

        try (DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            while (din.available() > 0) {
                int nameLen = din.readUnsignedByte();
                din.skipBytes(nameLen);
                lastId = din.readInt();
                int emailLen = din.readUnsignedByte();
                din.skipBytes(emailLen);
            }
        }
        return lastId + 1;
    }

}
