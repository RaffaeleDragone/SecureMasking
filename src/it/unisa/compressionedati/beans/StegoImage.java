package it.unisa.compressionedati.beans;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 *
 * @author raffaeledragone
 */
public class StegoImage {
    private int offset;
    private int width;
    private int height;
    private byte[] img;

    public StegoImage() {
    }

    public StegoImage(int offset, int width, int height, byte[] carrier) {
        this.offset = offset;
        this.width = width;
        this.height = height;
        this.img = carrier;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public byte[] getImg() {
        return img;
    }

    public void setImg(byte[] img) throws IOException {
        this.img = img;

    }


}
