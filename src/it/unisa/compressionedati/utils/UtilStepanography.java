package it.unisa.compressionedati.utils;

import it.unisa.compressionedati.beans.StegoImage;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class UtilStepanography {

    private static final String MARKER_START_STEGANOMESS = "DRBC";


    public static byte[] hide(byte[] data, String secretMessage, char[] password, boolean compress, boolean encrypt) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalStateException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        StegoImage stegoImage=new StegoImage();
        stegoImage.setImg(data);

        ByteArrayInputStream bais = new ByteArrayInputStream(stegoImage.getImg());

        BufferedImage image = ImageIO.read(bais);
        stegoImage.setWidth(image.getWidth());
        stegoImage.setHeight(image.getHeight());

        if (encrypt) {
            if (password == null) {
                throw new IllegalArgumentException("Encryption cannot be done with no password");
            }
        }

        byte[] payload = secretMessage.getBytes();
        byte[] fingerprinMsg = MARKER_START_STEGANOMESS.getBytes();
        String imageFileNameWithoutExt = null;
        File imageFile = null;
        int payloadSize = payload.length;
        int freeSpaceInCarrier = 0;
        int _bytesWritten;
        int payloadOffset = 0;

        //System.out.println("Encryption:" + encryption);
        //System.out.println("Compression:" + compression);
        //System.out.println("Payload Size:" + payloadSize);
        if (compress) {
            payload = compressPayload(payload);
            payloadSize = payload.length;
            //System.out.println("Compressed Size:" + payloadSize);
        }
        if (encrypt) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.reset();
            md.update(new String(password).getBytes());

            payload = encryptPayload(payload, md.digest());
            payloadSize = payload.length;
            //System.out.println("Encrypted Size:" + payloadSize);
        }

        //System.out.println("Files Found: " + carriers.length);
        //System.out.println(sectretFname);

        stegoImage.setOffset(0);
        _bytesWritten = 0;
        imageFile = new File("outputimage.png");
        imageFileNameWithoutExt = "outputimage";
        //System.out.println(imageFileNameWithoutExt);
        stegoImage.setImg(convertImageToRGBPixels(stegoImage,stegoImage.getImg()));

        freeSpaceInCarrier = stegoImage.getImg().length / 8;
        //System.out.println("FreeSpace In frame: " + freeSpaceInCarrier);
        freeSpaceInCarrier -= encode(stegoImage,fingerprinMsg, 4, 0);
        freeSpaceInCarrier -= encode(stegoImage,getBytes(payloadSize), 4, 0);


        if (freeSpaceInCarrier < payloadSize) {
            _bytesWritten = encode(stegoImage,payload, freeSpaceInCarrier, payloadOffset);
        } else {
            _bytesWritten = encode(stegoImage,payload, payloadSize, payloadOffset);
        }
        freeSpaceInCarrier -= _bytesWritten;
        //System.out.println("(Payload)Bytes Written: " + _bytesWritten);
        payloadSize -= _bytesWritten;
        payloadOffset += _bytesWritten;
        //System.out.println("Bytes Remaining: " + (payloadSize));
        //System.out.println("Payload Offset: " + payloadOffset);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(convertRGBPixelsToImage(stegoImage.getImg(),stegoImage.getWidth(),stegoImage.getHeight()), "png", bos);
        byte[] bytes = bos.toByteArray();

        if (payloadSize > 0) {
            //System.out.println("File non inserito del tutto");
        } else {
            //System.out.println("File steganografato correttamente");
        }

        if (payloadSize > 0) {
            throw new IllegalArgumentException("Not enough cover images");
        }

        return bytes;

    }


    private static int encode(StegoImage stegoImage, byte[] payload, int bytesToWrite, int payloadOffset) {

        int bytesWritten = 0;
        for (int i = 0; i < bytesToWrite; i++, payloadOffset++) {
            int payloadByte = payload[payloadOffset];
            bytesWritten++;
            for (int bit = 7; bit >= 0; --bit, stegoImage.setOffset(stegoImage.getOffset()+1)) {
                //assign an integer to b,shifted by bit spaces AND 1
                //a single bit of the current byte
                int b = (payloadByte >>> bit) & 1;
                //assign the bit by taking[(previous byte value) AND 0xfe]
                //or bit to
                try {
                    int offset = stegoImage.getOffset();
                    stegoImage.getImg()[offset] = (byte) ((stegoImage.getImg()[offset] & 0xFE) | b);
                } catch (ArrayIndexOutOfBoundsException aiobe) {
                    //System.err.println(aiobe.getMessage());
                }
            }
        }
        return bytesWritten;
    }

    private byte[] addMessageToPayload(byte[] payload, byte[] msgBytes) {
        int totalSize = payload.length + msgBytes.length;
        byte[] _payload = new byte[totalSize];
        for (int i = 0; i < payload.length; i++) {
            _payload[i] = payload[i];
        }
        for (int i = 0; i < totalSize - payload.length; i++) {
            _payload[i + payload.length] = msgBytes[i];
        }
        return _payload;
    }


    public static String reveal(byte[] data, char[] password, boolean compress, boolean encrypt) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalStateException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        String out=null;

        StegoImage stegoImage=new StegoImage();
        stegoImage.setImg(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(stegoImage.getImg());

        BufferedImage image = ImageIO.read(bais);
        stegoImage.setWidth(image.getWidth());
        stegoImage.setHeight(image.getHeight());


        byte payload[] = null;
        byte[] tmp = null;
        int payloadRemaining = 0;
        int fnameSize = 0;
        int payloadSize  = 0;
        String fname = null;
        String[] carriers = null;
        int msgLen = 0;
        int bytesToDecodeFromCarrier = 0;
        ArrayList<byte[]> payloadData = new ArrayList<byte[]>();
        FileOutputStream fOutStream;

        stegoImage.setOffset(0);

        stegoImage.setImg(convertImageToRGBPixels(stegoImage,stegoImage.getImg()));
        if (!isStegnographed(stegoImage)) {
            System.out.println("Not stegano image!");
            return null;
        }
        //System.out.println("Encryption:" + encryption);
        //System.out.println("Compression:" + compression);
        bytesToDecodeFromCarrier = stegoImage.getImg().length / 8 - 4;// - 4 bcoz we have already decoded the fingerprint
        //System.out.println("Bytes to Decode: " + bytesToDecodeFromCarrier);
        tmp = decode(stegoImage, 4); //extracting the payload size
        payloadSize = toInteger(tmp);
        payloadRemaining = payloadSize;
        bytesToDecodeFromCarrier -= 4;

        if (payloadRemaining > bytesToDecodeFromCarrier) {
            payload = decode(stegoImage, bytesToDecodeFromCarrier);
            payloadRemaining = payloadRemaining - bytesToDecodeFromCarrier;
        } else {
            payload = decode(stegoImage, payloadRemaining);
            payloadRemaining = payloadRemaining - payloadRemaining;
        }


        //System.out.println("payload Remaining " + payloadRemaining);
        payloadData.add(payload);
        //payloadData.put(i, payload);
        if (payloadRemaining == 0) {
            System.out.println("Finished success");

        }


        if (payloadRemaining > 0) {
            throw new IllegalArgumentException("Some Stego Files missing!");
        }

        if (!payloadData.isEmpty()) {
            byte[] secretData = new byte[payloadSize];
            byte[] message;// = new byte[msgLen];
            byte[] secretFile;// = new byte[payloadSize - msgLen];
            int ptr = 0;
            for (int i = 0; i < payloadData.size(); i++) {
                byte[] tmpArray = payloadData.get(i);
                for (int j = 0; j < tmpArray.length; j++, ptr++) {
                    secretData[ptr] = tmpArray[j];
                }
            }
            if (encrypt) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.reset();
                md.update(new String(password).getBytes());
                secretData = decryptPayload(secretData, md.digest());
                payloadSize = secretData.length;
                //System.out.println("Decrypted Size:" + payloadSize);
            }

            if (compress) {
                secretData = decompressPayload(secretData);
                payloadSize = secretData.length;
                //System.out.println("Uncompressed Size:" + payloadSize);
            }
            message = new byte[msgLen];
            secretFile = new byte[payloadSize - msgLen];
            //System.out.println("Data Extracted!!!");
            for (int i = 0; i < payloadSize - msgLen; i++) {
                secretFile[i] = secretData[i];
            }
            //System.out.println("Got the File");
            for (int j = 0; j < (msgLen); j++) {
                message[j] = secretData[j + (payloadSize - msgLen)];
            }

            out = new String(secretFile, StandardCharsets.UTF_8);

        }
        return out;
    }


    private static byte[] decode(StegoImage stegoImage, int bytesToRead) {
        byte[] _decode = new byte[bytesToRead];
        for (int i = 0; i < _decode.length; ++i) {
            for (int bit = 0; bit < 8; ++bit, stegoImage.setOffset(stegoImage.getOffset()+1)) {
                try {
                    int offset = stegoImage.getOffset();
                    _decode[i] = (byte) ((_decode[i] << 1) | (stegoImage.getImg()[offset] & 1));
                } catch (ArrayIndexOutOfBoundsException aiobe) {
                    //System.err.println("OK" + aiobe.getMessage());
                }
            }
        }
        return _decode;
    }

    private static BufferedImage convertRGBPixelsToImage(byte[] carrier, int width, int height) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] nBits = {8, 8, 8};
        int[] bOffs = {2, 1, 0}; // band offsets r g b
        int pixelStride = 3; //assuming r, g, b, skip, r, g, b, skip..
        ColorModel colorModel = new ComponentColorModel(
                cs, nBits, false, false,
                Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE);
        WritableRaster raster = Raster.createInterleavedRaster(
                new DataBufferByte(carrier, carrier.length), width, height, width * 3, pixelStride, bOffs, null);

        return new BufferedImage(colorModel, raster, false, null);
    }

    private static byte[] convertImageToRGBPixels(StegoImage stegoImage,byte[] img) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(img);

        BufferedImage image = ImageIO.read(bais);
        int width = stegoImage.getWidth();
        int height = stegoImage.getHeight();
        BufferedImage clone = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = clone.createGraphics();
        graphics.drawRenderedImage(image, null);
        graphics.dispose();
        image.flush();
        WritableRaster raster = clone.getRaster();
        DataBufferByte buff = (DataBufferByte) raster.getDataBuffer();
        return buff.getData();
    }


    private static byte[] compressPayload(byte[] payload) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream zos = new GZIPOutputStream(bos);
        zos.write(payload);
        zos.finish();
        zos.close();
        bos.close();
        return bos.toByteArray();
    }

    private static byte[] decompressPayload(byte[] payload) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(payload);
        GZIPInputStream zis = new GZIPInputStream(bis);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] dataBuf = new byte[4096];
        int bytes_read = 0;
        while ((bytes_read = zis.read(dataBuf)) > 0) {
            out.write(dataBuf, 0, bytes_read);
        }
        payload = out.toByteArray();
        out.close();
        zis.close();
        bis.close();
        return payload;
    }

    private static byte[] encryptPayload(byte[] payload, byte[] password) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalStateException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec key = new SecretKeySpec(password, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] cipherText = new byte[cipher.getOutputSize(payload.length)];
        int ctLength = cipher.update(payload, 0, payload.length, cipherText, 0);
        ctLength += cipher.doFinal(cipherText, ctLength);
        //System.out.println(new String(cipherText));
        //System.out.println(ctLength);
        return cipherText;
    }

    private static byte[] decryptPayload(byte[] payload, byte[] password) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalStateException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec key = new SecretKeySpec(password, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = new byte[cipher.getOutputSize(payload.length)];
        int ptLength = cipher.update(payload, 0, payload.length, plainText, 0);
        ptLength += cipher.doFinal(plainText, ptLength);
        //System.out.println(new String(plainText));
        //System.out.println(ptLength);
        //payloadSize = ptLength;
        return plainText;
    }


    private static int toInteger(byte[] b) {
        return (b[0] << 24 | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | (b[3] & 0xFF));
    }

    private static byte[] getBytes(int i) {
        return (new byte[]{(byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i});
    }

    private static boolean isStegnographed(StegoImage stegoImage) {
        byte[] tmp = new byte[4];
        String fingerPrint = null;
        tmp = decode(stegoImage, 4);
        fingerPrint = new String(tmp);
        if (!fingerPrint.equals(MARKER_START_STEGANOMESS)) {
            return false;
        }
        return true;
    }

}
