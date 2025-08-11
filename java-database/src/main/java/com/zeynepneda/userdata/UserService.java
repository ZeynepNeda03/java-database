package com.zeynepneda.userdata;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

class UserService {

    private final String INDEX_ID_FILE = "index_by_id.idx";
    private final String INDEX_NAME_FILE = "index_by_name.idx";
    private final String LAST_ID_FILE = "last_id.dat";

    private FileService fs = new FileService();

    int findUser(Object input, File file) throws IOException {
        if (input instanceof Integer) {
            int id = (Integer) input;
            int offset = findById(id);
            if (offset > 0) {
                printUserAtOffset(file, offset);
                return id;
            } else {
                System.out.println("User not found with ID: " + id);
                return -1;
            }
        } else if (input instanceof String) {
            String username = (String) input;
            int offset = findByName(username);
            if (offset > 0) {
                printUserAtOffset(file, offset);
                try (RandomAccessFile raf = fs.getRandomAccessFile(file, "r")) {
                    raf.seek(offset + 1 + 1); // validFlag + nameLength
                    raf.skipBytes(username.getBytes("UTF-8").length);
                    return raf.readInt();
                }
            } else {
                System.out.println("User not found with username: " + username);
                return -1;
            }
        } else {
            System.out.println("Unsupported input type.");
            return -1;
        }
    }

    private void printUserAtOffset(File file, int offset) throws IOException {
        try (RandomAccessFile raf = fs.getRandomAccessFile(file, "r")) {
            raf.seek(offset);
            byte validFlag = raf.readByte();
            if (validFlag == 0) {
                System.out.println("User deleted.");
                return;
            }
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
        try (RandomAccessFile raf = fs.getRandomAccessFile(new File(INDEX_ID_FILE), "r")) {
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
        try (DataInputStream din = fs.getDataInputStream(new File(INDEX_NAME_FILE))) {
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
        int nextUserId = readLastId();
        long offset = fs.getFileLength(file);

        try (DataOutputStream dou = fs.getDataOutputStream(file, true)) {
            byte[] nameBytes = username.getBytes("UTF-8");
            byte[] emailBytes = email.getBytes("UTF-8");
            dou.writeByte(1);
            dou.writeByte(nameBytes.length);
            dou.write(nameBytes);
            dou.writeInt(nextUserId);
            dou.writeByte(emailBytes.length);
            dou.write(emailBytes);
        }

        try (DataOutputStream idIndex = fs.getDataOutputStream(new File(INDEX_ID_FILE), true)) {
            idIndex.writeInt(nextUserId);
            idIndex.writeInt((int) offset);
        }

        try (DataOutputStream nameIndex = fs.getDataOutputStream(new File(INDEX_NAME_FILE), true)) {
            byte[] nameBytes = username.getBytes("UTF-8");
            nameIndex.writeByte(nameBytes.length);
            nameIndex.write(nameBytes);
            nameIndex.writeInt((int) offset);
        }

        saveLastId(nextUserId + 1);
        System.out.println("User added: " + username + " (ID: " + nextUserId + ")");
    }

    void editUser(int targetId, String newUsername, String newEmail, File file) throws IOException {
        int oldOffset = findById(targetId);
        if (oldOffset <= 0) {
            System.out.println("User not found.");
            return;
        }
        fs.writeAtOffset(file, oldOffset, new byte[]{0});
        long newOffset = fs.getFileLength(file);

        try (DataOutputStream out = fs.getDataOutputStream(file, true)) {
            byte[] nameBytes = newUsername.getBytes("UTF-8");
            byte[] emailBytes = newEmail.getBytes("UTF-8");
            out.writeByte(1);
            out.writeByte(nameBytes.length);
            out.write(nameBytes);
            out.writeInt(targetId);
            out.writeByte(emailBytes.length);
            out.write(emailBytes);
        }

        updateIdIndex(targetId, (int) newOffset);
        updateNameIndex(newUsername, (int) newOffset);
        System.out.println("User updated: " + targetId);
    }

    void deleteUser(int targetId, File file) throws IOException {
        int offset = findById(targetId);
        if (offset <= 0) {
            System.out.println("User not found.");
            return;
        }
        fs.writeAtOffset(file, offset, new byte[]{0});

        try (RandomAccessFile raf = fs.getRandomAccessFile(new File(INDEX_ID_FILE), "rw")) {
            while (raf.getFilePointer() < raf.length()) {
                int id = raf.readInt();
                long pos = raf.getFilePointer();
                int idxOffset = raf.readInt();
                if (id == targetId) {
                    raf.seek(pos);
                    raf.writeInt(0);
                    break;
                }
            }
        }
        System.out.println("User deleted: ID " + targetId);
    }

    private void updateIdIndex(int targetId, int newOffset) throws IOException {
        try (RandomAccessFile raf = fs.getRandomAccessFile(new File(INDEX_ID_FILE), "rw")) {
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
        try (RandomAccessFile raf = fs.getRandomAccessFile(new File(INDEX_NAME_FILE), "rw")) {
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

    private int readLastId() {
        File f = new File(LAST_ID_FILE);
        if (!f.exists() || f.length() == 0) {
            return 1;
        }
        try (DataInputStream din = fs.getDataInputStream(f)) {
            return din.readInt();
        } catch (IOException e) {
            return 1;
        }
    }

    private void saveLastId(int lastId) {
        try (DataOutputStream dout = fs.getDataOutputStream(new File(LAST_ID_FILE), false)) {
            dout.writeInt(lastId);
        } catch (IOException ignored) {
        }
    }
}
