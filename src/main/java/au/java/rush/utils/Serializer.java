package au.java.rush.utils;

import difflib.Patch;

import java.io.*;

/**
 * Created by andy on 9/26/16.
 */
public class Serializer {
    public static void serialize(Object obj, String fileName) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(fileName);
        ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(obj);
        }
    }

    public static Object deserialize(String fileName) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(fileName);
        ObjectInputStream ois = new ObjectInputStream(fis)) {
            return ois.readObject();
        }
    }
}
