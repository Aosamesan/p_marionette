package com.example.aosamesan.projectfinalfinal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/**
 * Created by Aosamesan on 2015-06-16.
 */
public class PMarionette {
    public static final int UNDEFINED = -127;
    public static final int DISCONNECTED = -1;
    public static final int IMAGE = 0;
    public static final int LISTITEM = 1;
    public static final int SIGNAL = 2;
    public static final int LISTITEMREQUEST = 3;

    // Signal
    public static final int APPLCIATIONEXIT = -1;
    public static final int START = 0;
    public static final int STOP = 1;
    public static final int NEXT = 2;
    public static final int PREV = 3;


    public static class MessageSerializer {
        public static byte[] UnZip(byte[] bytes) {
            final int BUFFER_SIZE = 512;
            try {
                ByteArrayInputStream memoryInputSteram = new ByteArrayInputStream(bytes);
                GZIPInputStream gzipInputStream = new GZIPInputStream(memoryInputSteram);
                ByteArrayOutputStream memoryOutputStream = new ByteArrayOutputStream();

                int read = 0;
                byte[] buffer = new byte[BUFFER_SIZE];

                while (read >= 0) {
                    read = gzipInputStream.read(buffer, 0, BUFFER_SIZE);
                    if (read > 0) {
                        memoryOutputStream.write(buffer, 0, read);
                    }
                }
                memoryInputSteram.close();
                gzipInputStream.close();

                return memoryOutputStream.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        // From Android to C#
        // Serialize
        // Signal and List Item #
        public static byte[] Serialize(int msgType, int data) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                byteArrayOutputStream.write(ByteBuffer.allocate(4).putInt(msgType).array());
                byteArrayOutputStream.write(ByteBuffer.allocate(4).putInt(4).array());
                byteArrayOutputStream.write(ByteBuffer.allocate(4).putInt(data).array());

                return byteArrayOutputStream.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        // From C# to Android
        // Deserialize
        // Disconnect, List Items, Image
        public static Object Deserialize(int msgType, int size, byte[] bytes) {
            Object result = null;
            ByteArrayInputStream byteArrayInputStream;
            GZIPInputStream gzipInputStream;
            ByteArrayOutputStream byteArrayOutputStream;
            final int BUFFER_SIZE = 512;

            if (size != bytes.length)
                return result;

            try {
                byteArrayInputStream = new ByteArrayInputStream(bytes);
                gzipInputStream = new GZIPInputStream(byteArrayInputStream);
                byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[BUFFER_SIZE];
                int read = -1;
                int allread = 0;

                switch (msgType) {
                    case IMAGE: // compressed
                        result = UnZip(bytes);
                        break;
                    case LISTITEM:  // compressed
                        byte[] stringBytes = UnZip(bytes);

                        String pptListItemString = new String(stringBytes, "UTF-8");
                        System.out.println("************************" + pptListItemString);
                        HashMap<String, Integer> resultMap = new HashMap<>();
                        String[] pptListItems = pptListItemString.split("[@]");
                        for (String pptListItem : pptListItems) {
                            if (pptListItem.equals(""))
                                break;
                            System.out.println("********" + pptListItem);
                            String[] item = pptListItem.split("[|]");
                            for (String s : item) {
                                System.out.println("***" + item[0] + " / " + s);
                            }
                            System.out.println("**** 0 : " + item[0] + " 1 : " + item[1]);
                            int pptNum = Integer.parseInt(item[0]);
                            String pptPath = item[1];
                            resultMap.put(pptPath, pptNum);
                        }

                        result = resultMap;
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }
    }
}
