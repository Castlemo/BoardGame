package com.marblegame.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 이미지 리소스 로더 및 캐시 관리
 */
public class ImageLoader {
    private static final Map<String, BufferedImage> imageCache = new HashMap<>();
    private static final String ASSET_PATH = "/com/marblegame/asset/";

    /**
     * 이미지 로드 (캐싱)
     */
    public static BufferedImage loadImage(String filename) {
        if (imageCache.containsKey(filename)) {
            return imageCache.get(filename);
        }

        try {
            InputStream stream = ImageLoader.class.getResourceAsStream(ASSET_PATH + filename);
            if (stream == null) {
                System.err.println("이미지를 찾을 수 없습니다: " + ASSET_PATH + filename);
                return null;
            }
            BufferedImage image = ImageIO.read(stream);
            imageCache.put(filename, image);
            return image;
        } catch (IOException e) {
            System.err.println("이미지 로드 실패: " + filename);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 이미지를 특정 크기로 스케일링
     */
    public static BufferedImage scaleImage(BufferedImage original, int width, int height) {
        if (original == null) return null;

        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();

        // 고품질 렌더링 설정
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.drawImage(original, 0, 0, width, height, null);
        g2.dispose();

        return scaled;
    }

    /**
     * 이미지를 ImageIcon으로 로드
     */
    public static ImageIcon loadIcon(String filename, int width, int height) {
        BufferedImage image = loadImage(filename);
        if (image == null) return null;

        BufferedImage scaled = scaleImage(image, width, height);
        return new ImageIcon(scaled);
    }

    /**
     * 건물 레벨 이미지 로드
     */
    public static BufferedImage getBuildingImage(int level) {
        switch (level) {
            case 1: return loadImage("house.png");
            case 2: return loadImage("building.png");
            case 3: return loadImage("tower.png");
            case 4: return loadImage("landmark.png");
            default: return null;
        }
    }

    /**
     * 타일 및 기타 아이콘 이미지 로드
     */
    public static BufferedImage getTileImage(String tileName) {
        switch (tileName.toUpperCase()) {
            case "START": return loadImage("start.png");
            case "CHANCE": return loadImage("chance.png");
            case "ISLAND": return loadImage("island.png");
            case "OLYMPIC": return loadImage("olympic.png");
            case "TAX": return loadImage("tax.png");
            case "WORLD_TOUR": return loadImage("worldtour.png");
            case "LOCK": return loadImage("lock.png");
            case "MONEY": return loadImage("money.png");
            case "DICE": return loadImage("dice.png");
            // 관광지 아이콘
            case "DOKDO": return loadImage("dokdo.png");
            case "BALI": return loadImage("bali.png");
            case "HAWAII": return loadImage("hawaii.png");
            case "PUKET": return loadImage("puket.png");
            case "TAHITI": return loadImage("tahiti.png");
            default: return null;
        }
    }

    /**
     * 캐시 초기화
     */
    public static void clearCache() {
        imageCache.clear();
    }
}
