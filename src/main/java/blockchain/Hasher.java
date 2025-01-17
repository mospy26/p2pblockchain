package blockchain;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import java.util.ArrayList;

// Hasher class
public class Hasher {
    public static void main(String[] args) {
        ArrayList<String> messages = new ArrayList<>();
        messages.add("12345");
        messages.add("12345");
        System.out.println(hashMessages(messages));
        // you should see the hash value calculated is
        // 7ccQDEgOM+OWvega620WYCXJHs1w53wSumP329xFirw=
    }

    public static String hashMessages(ArrayList<String> messages) {
        String hashString = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            for (String message : messages) {
                dos.writeUTF(message);
            }

            byte[] bytes = baos.toByteArray();
            byte[] hash = digest.digest(bytes);
            hashString = Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
        } catch (IOException e) {
        }
        return hashString;
    }

}