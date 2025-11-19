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
     * @param level 건물 레벨 (1-4)
     * @param cityName 도시 이름 (랜드마크용)
     */
    public static BufferedImage getBuildingImage(int level, String cityName) {
        switch (level) {
            case 1: return loadImage("house.png");
            case 2: return loadImage("building.png");
            case 3: return loadImage("tower.png");
            case 4: return getLandmarkImage(cityName);
            default: return null;
        }
    }

    /**
     * 도시별 랜드마크 이미지 로드
     */
    private static BufferedImage getLandmarkImage(String cityName) {
        String filename = getLandmarkFilename(cityName);
        if (filename != null) {
            // landmarks 서브디렉토리에서 로드
            try {
                InputStream stream = ImageLoader.class.getResourceAsStream(ASSET_PATH + "landmarks/" + filename);
                if (stream == null) {
                    System.err.println("랜드마크 이미지를 찾을 수 없습니다: " + ASSET_PATH + "landmarks/" + filename);
                    return loadImage("landmark.png"); // 기본 이미지 폴백
                }

                String cacheKey = "landmarks/" + filename;
                if (imageCache.containsKey(cacheKey)) {
                    return imageCache.get(cacheKey);
                }

                BufferedImage image = ImageIO.read(stream);
                imageCache.put(cacheKey, image);
                return image;
            } catch (IOException e) {
                System.err.println("랜드마크 이미지 로드 실패: " + filename);
                e.printStackTrace();
                return loadImage("landmark.png"); // 기본 이미지 폴백
            }
        }
        return loadImage("landmark.png"); // 기본 이미지 폴백
    }

    /**
     * 한글 도시 이름을 영문 파일명으로 매핑
     */
    private static String getLandmarkFilename(String cityName) {
        if (cityName == null) return null;

        switch (cityName) {
            case "방콕": return "bangkok.png";
            case "베이징": return "beijing.png";
            case "타이페이": return "taipei.png";
            case "두바이": return "dubai.png";
            case "카이로": return "cairo.png";
            case "도쿄": return "tokyo.png";
            case "시드니": return "sydney.png";
            case "퀘벡": return "quebec.png";
            case "상파울로": return "saopaulo.png";
            case "프라하": return "praha.png";
            case "베를린": return "berlin.png";
            case "모스크바": return "moscow.png";
            case "제네바": return "geneva.png";
            case "로마": return "rome.png";
            case "런던": return "london.png";
            case "파리": return "paris.png";
            case "뉴욕": return "newyork.png";
            case "서울": return "seoul.png";
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
