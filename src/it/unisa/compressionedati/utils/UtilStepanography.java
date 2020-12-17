package it.unisa.compressionedati.utils;

import com.pngencoder.PngEncoder;


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

    private final String FINGERPRINT_MESSAGE = "DRBC";
    private int offset;
    private int width;
    private int height;
    private byte[] carrier;
    private String hiddenMessage;
    private boolean encryption;
    private boolean compression;

    public String getDecodedMessage() {
        return hiddenMessage;
    }

    public boolean isEncryption() {
        return encryption;
    }

    public void setEncryption(boolean encrypt) {
        this.encryption = encrypt;
    }

    public boolean isCompression() {
        return compression;
    }

    public void setCompression(boolean compression) {
        this.compression = compression;
    }


    public ArrayList<byte[]> hideNew(ArrayList<byte[]> imgsBytes, String secretFile, char[] password) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalStateException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        ArrayList<byte[]> listOut=new ArrayList<>();

        if (secretFile == null) {
            throw new FileNotFoundException("");
        }

        if (encryption) {
            if (password == null) {
                throw new IllegalArgumentException("Encryption cannot be done with no password");
            }
        }

        byte[] payload = secretFile.getBytes();
        byte[] fingerprinMsg = FINGERPRINT_MESSAGE.getBytes();

        String imageFileNameWithoutExt = null;

        File imageFile = null;
        int payloadSize = payload.length;
        int freeSpaceInCarrier = 0;
        int _bytesWritten;
        int payloadOffset = 0;


        //System.out.println("Encryption:" + encryption);
        //System.out.println("Compression:" + compression);
        //System.out.println("Payload Size:" + payloadSize);
        if (compression) {
            payload = compressPayload(payload);
            payloadSize = payload.length;
            //System.out.println("Compressed Size:" + payloadSize);
        }
        if (encryption) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.reset();
            md.update(new String(password).getBytes());

            payload = encryptPayload(payload, md.digest());
            payloadSize = payload.length;
            //System.out.println("Encrypted Size:" + payloadSize);
        }

        //System.out.println("Files Found: " + carriers.length);
        //System.out.println(sectretFname);
        for (int i = 0; i < imgsBytes.size(); i++) {
            offset = 0;
            _bytesWritten = 0;
            //imageFile = new File(carrierDir + "/temp" + i+".png");


            //System.out.println(imageFileNameWithoutExt);
            ByteArrayInputStream bais = new ByteArrayInputStream(imgsBytes.get(i));

            BufferedImage image = ImageIO.read(bais);
            imageFileNameWithoutExt = getFilenameWithoutExtension(i+"");
            carrier = convertImageToRGBPixels(image);

            freeSpaceInCarrier = carrier.length / 8;
            //System.out.println("FreeSpace In Carrier: " + freeSpaceInCarrier);
            freeSpaceInCarrier -= encode(fingerprinMsg, 4, 0);

            //freeSpaceInCarrier -= encode(getBytes(i), 4, 0);

            if (i == 0) {
                freeSpaceInCarrier -= encode(getBytes(payloadSize), 4, 0);

                //freeSpaceInCarrier -= encode(getBytes(fnameLen), 4, 0);

                //freeSpaceInCarrier -= encode(sectretFname.getBytes(), sectretFname.getBytes().length, 0);

                //freeSpaceInCarrier -= encode(getBytes(message.getBytes().length), 4, 0);
            }


            if (freeSpaceInCarrier < payloadSize) {
                _bytesWritten = encode(payload, freeSpaceInCarrier, payloadOffset);
            } else {
                _bytesWritten = encode(payload, payloadSize, payloadOffset);
            }
            freeSpaceInCarrier -= _bytesWritten;
            //System.out.println("(Payload)Bytes Written: " + _bytesWritten);
            payloadSize -= _bytesWritten;
            payloadOffset += _bytesWritten;
            //System.out.println("Bytes Remaining: " + (payloadSize));
            //System.out.println("Payload Offset: " + payloadOffset);
            byte[] bytes = new PngEncoder()
                    .withBufferedImage(convertRGBPixelsToImage(carrier))
                    .withCompressionLevel(1)
                    .toBytes();
            listOut.add(bytes);
            if (payloadSize > 0) {
                //System.out.println("@continue");
                continue;
            } else {
                break;
            }
        }
        if (payloadSize > 0) {
            throw new IllegalArgumentException("Not enough cover images");
        }
        return listOut;
    }


    private int encode(byte[] payload, int bytesToWrite, int payloadOffset) {
        int bytesWritten = 0;
        for (int i = 0; i < bytesToWrite; i++, payloadOffset++) {
            int payloadByte = payload[payloadOffset];
            bytesWritten++;
            for (int bit = 7; bit >= 0; --bit, ++offset) {
                //assign an integer to b,shifted by bit spaces AND 1
                //a single bit of the current byte
                int b = (payloadByte >>> bit) & 1;
                //assign the bit by taking[(previous byte value) AND 0xfe]
                //or bit to
                try {
                    carrier[offset] = (byte) ((carrier[offset] & 0xFE) | b);
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


    public String revealNew(ArrayList<byte[]> imgsBytes, char[] password) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalStateException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        byte payload[] = null;
        byte[] tmp = null;
        int payloadRemaining = 0;
        int payloadSize  = 0;

        int msgLen = 0;
        int bytesToDecodeFromCarrier = 0;
        ArrayList<byte[]> payloadData = new ArrayList<byte[]>();

        String output="";
        for (int i = 0; i < imgsBytes.size(); i++) {
            offset = 0;
            ByteArrayInputStream bais = new ByteArrayInputStream(imgsBytes.get(i));

            BufferedImage image = ImageIO.read(bais);
            carrier = convertImageToRGBPixels(image);


            if (!isStegnographed(carrier)) {
                continue;
            }
            //System.out.println("Encryption:" + encryption);
            //System.out.println("Compression:" + compression);
            bytesToDecodeFromCarrier = carrier.length / 8 - 4;// - 4 bcoz we have already decoded the fingerprint
            //System.out.println("Bytes to Decode: " + bytesToDecodeFromCarrier);
            if (i == 0) {
                tmp = decode(carrier, 4); //extracting the payload size
                payloadSize = toInteger(tmp);
                payloadRemaining = payloadSize;
                bytesToDecodeFromCarrier -= 4;
                //System.out.println("Bytes to Decode: " + bytesToDecodeFromCarrier);
                //System.out.println("Payload Size: " + payloadSize);
            }
            if (payloadRemaining > bytesToDecodeFromCarrier) {
                payload = decode(carrier, bytesToDecodeFromCarrier);
                payloadRemaining = payloadRemaining - bytesToDecodeFromCarrier;
            } else {
                payload = decode(carrier, payloadRemaining);
                payloadRemaining = payloadRemaining - payloadRemaining;
            }


            //System.out.println("payload Remaining " + payloadRemaining);
            payloadData.add(payload);
            //payloadData.put(i, payload);
            if (payloadRemaining == 0) {
                break;
            }


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
            if (encryption) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.reset();
                md.update(new String(password).getBytes());
                secretData = decryptPayload(secretData, md.digest());
                payloadSize = secretData.length;
                //System.out.println("Decrypted Size:" + payloadSize);
            }

            if (compression) {
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
            hiddenMessage = new String(message);
            String res = new String(secretFile, StandardCharsets.UTF_8);
            output+=res;
        }
        return output;
    }

    private byte[] decode(byte[] carrier, int bytesToRead) {
        byte[] _decode = new byte[bytesToRead];
        for (int i = 0; i < _decode.length; ++i) {
            for (int bit = 0; bit < 8; ++bit, ++offset) {
                try {
                    _decode[i] = (byte) ((_decode[i] << 1) | (carrier[offset] & 1));
                } catch (ArrayIndexOutOfBoundsException aiobe) {
                    //System.err.println("OK" + aiobe.getMessage());
                }
            }
        }
        return _decode;
    }

    /**
     * Converts a byte array with RGB pixel values to
     * a bufferedImage
     * @param carrier byte array of RGB pixels
     * @return BufferedImage
     */
    private BufferedImage convertRGBPixelsToImage(byte[] carrier) {
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

    /**
     * Converts an Image to RG pixel array
     * @param filename image to convert
     * @return byte array
     * @throws IOException
     */
    private byte[] convertImageToRGBPixels(File filename) throws IOException {
        BufferedImage image = ImageIO.read(filename);
        width = image.getWidth();
        height = image.getHeight();
        BufferedImage clone = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = clone.createGraphics();
        graphics.drawRenderedImage(image, null);
        graphics.dispose();
        image.flush();
        WritableRaster raster = clone.getRaster();
        DataBufferByte buff = (DataBufferByte) raster.getDataBuffer();
        return buff.getData();
    }

    public byte[] convertImageToRGBPixels(BufferedImage image) throws IOException {
        //BufferedImage image = ImageIO.read(filename);
        width = image.getWidth();
        height = image.getHeight();
        BufferedImage clone = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = clone.createGraphics();
        graphics.drawRenderedImage(image, null);
        graphics.dispose();
        image.flush();
        WritableRaster raster = clone.getRaster();
        DataBufferByte buff = (DataBufferByte) raster.getDataBuffer();
        return buff.getData();
    }

    private byte[] compressPayload(byte[] payload) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream zos = new GZIPOutputStream(bos);
        zos.write(payload);
        zos.finish();
        zos.close();
        bos.close();
        return bos.toByteArray();
    }

    private byte[] decompressPayload(byte[] payload) throws IOException {
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

    private byte[] encryptPayload(byte[] payload, byte[] password) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalStateException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
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


    private byte[] decryptPayload(byte[] payload, byte[] password) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalStateException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
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


    private String getFilenameWithoutExtension(String name) {
        return name.replaceFirst("[.][^.]+$", "");
    }


    private int toInteger(byte[] b) {
        return (b[0] << 24 | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | (b[3] & 0xFF));
    }


    private byte[] getBytes(File file) throws java.io.IOException {
        InputStream is = new FileInputStream(file);
        // Get the size of the file
        long length = file.length();
        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];
        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        // Close the input stream and return bytes
        is.close();
        return bytes;
    }


    private byte[] getBytes(int i) {
        return (new byte[]{(byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i});
    }


    private boolean isStegnographed(byte[] carrier) {
        byte[] tmp = new byte[4];
        String fingerPrint = null;
        tmp = decode(carrier, 4);
        fingerPrint = new String(tmp);
        if (!fingerPrint.equals(FINGERPRINT_MESSAGE)) {
            return false;
        }
        return true;
    }

}
