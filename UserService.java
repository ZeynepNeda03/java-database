
import java.io.*;
import java.util.*;

class UserService {

    private static int nextUserId = 1;
    private static final String INDEX_ID_FILE = "index_by_id.idx";
    private static final String INDEX_NAME_FILE = "index_by_name.idx";

    static void findUser(Object input, File file) throws IOException {

        try (DataInputStream din = new DataInputStream(new FileInputStream(file))) {
            String foundUsername = null;
            int foundID = 0;
            boolean found = false; //eğer bulduysak durmak için

            while (din.available() > 0) {
                int nameLength = din.readUnsignedByte();
                byte[] nameBytes = new byte[nameLength];

                din.readFully(nameBytes);
                String username = new String(nameBytes, "UTF-8");
                int id = din.readInt();
                if ((input instanceof String && username.equalsIgnoreCase((String) input))) {
                    foundUsername = username;
                    foundID = id;
                    found = true;
                    break;
                }
                if ((input instanceof Integer && id == (Integer) input)) {
                    foundUsername = username;
                    foundID = id;
                    found = true;
                    break;
                }
            }
            if (found) {
                System.out.println("\nUser Found:");
                System.out.println("Username: " + foundUsername);
                System.out.println("ID: " + foundID);
            } else {
                System.out.println("No matching user found.");
            }
        }
    }

    static void addUser(String username, int id, String email, File file) throws IOException {
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

    static void editUser(int targetId, String newUsername, String email, File file) {
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

    static void deleteUser(int targetId, File file) throws IOException {

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
}
