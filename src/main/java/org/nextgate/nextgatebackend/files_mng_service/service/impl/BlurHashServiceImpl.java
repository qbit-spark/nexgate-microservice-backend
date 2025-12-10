package org.nextgate.nextgatebackend.files_mng_service.service.impl;

import io.trbl.blurhash.BlurHash;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.files_mng_service.service.BlurHashService;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

@Service
@Slf4j
public class BlurHashServiceImpl implements BlurHashService {

    private static final int COMPONENT_X = 4;
    private static final int COMPONENT_Y = 3;
    private static final int MAX_DIMENSION = 64;

    @Override
    public String generateBlurHash(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                log.warn("Could not read image bytes");
                return null;
            }

            BufferedImage scaled = scaleDown(image);
            return BlurHash.encode(scaled, COMPONENT_X, COMPONENT_Y);

        } catch (Exception e) {
            log.error("Failed to generate BlurHash", e);
            return null;
        }
    }

    private BufferedImage scaleDown(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return original;
        }

        double scale = Math.min((double) MAX_DIMENSION / width, (double) MAX_DIMENSION / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        scaled.createGraphics().drawImage(
                original.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH),
                0, 0, null
        );

        return scaled;
    }
}