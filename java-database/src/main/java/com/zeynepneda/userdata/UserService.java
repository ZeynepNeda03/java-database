package com.zeynepneda.userdata;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

class UserService {

    private int nextUserId = 1;
    private final String INDEX_ID_FILE = "index_by_id.idx";
    private final String INDEX_NAME_FILE = "index_by_name.idx";

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
            long recordSize = 8; // int ID + int offset
            long numRecords = raf.length() / recordSize;
            long low = 0;
            long high = numRecords - 1;

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

    void addUser(String username, int id, String email, File file) throws IOException {
        User user = new User(username, id, email);
        long offset = file.length();

        try (DataOutputStream dou = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, true)))) {
            byte[] nameBytes = user.getUsername().getBytes("UTF-8");
            byte[] emailBytes = user.getEmail().getBytes("UTF-8");

            if (nameBytes.length > 255) {
                throw new IllegalArgumentException("Username length is too long");
            }
            //ismi ekle
            dou.writeByte(nameBytes.length);
            dou.write(nameBytes);

            //id ekle
            dou.writeInt(user.getId());

            //email ekle
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
    }

    void editUser(int targetId, String newUsername, String email, File file) {
        List<User> users = new ArrayList<>();

        try (DataInputStream din = new DataInputStream(new FileInputStream(file))) {
            while (din.available() > 0) {
                int nameLength = din.readUnsignedByte();
                byte[] nameBytes = new byte[nameLength];
                din.readFully(nameBytes);
                String username = new String(nameBytes, "UTF-8");
                int id = din.readInt();

                if (id == targetId) {
                    username = newUsername;
                }
                users.add(new User(username, id, email));
            }
        } catch (Exception e) {
            System.out.println("Read error: " + e.getMessage());
            return;
        }

        try (DataOutputStream dou = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, false)))) {
            for (User user : users) {
                byte[] nameBytes = user.username.getBytes("UTF-8");
                dou.writeByte(nameBytes.length);
                dou.write(nameBytes);
                dou.writeInt(user.id);
            }
        } catch (Exception e) {
            System.out.println("Write error: " + e.getMessage());
        }
    }

    void deleteUser(int targetId, File file) throws IOException {

    }

    /*public static int getNextId(File file) throws IOException {
        int lastId = 0;

        try (DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            while (din.available() > 0) {
                int len = din.readUnsignedByte();
                din.skipBytes(len);
                lastId = din.readInt();
            }
            return lastId + 1;
        }

    }*/
    //binary search sadece id araması için
    int binarySearch(String file, int x) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long low = 0;
            long high = file.length() / 4 - 1;

            while (low <= high) {
                long mid = low + (high - low) / 2;
                raf.seek(mid * 4);
                int value = raf.readInt();

                if (value == x) {
                    return (int) mid;
                } else if (value < x) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
        }
        return -1;
    }

}
