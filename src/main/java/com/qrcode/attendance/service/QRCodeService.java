package com.qrcode.attendance.service;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

@Service
public class QRCodeService {

    private static final Logger log = LoggerFactory.getLogger(QRCodeService.class);

    public String generateAttendanceQRCode(String attendanceCode, String baseUrl) {
        try {
            String qrContent = baseUrl + "/attendance/signin?code=" + attendanceCode;
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300);

            // 手动将BitMatrix转为BufferedImage（不依赖MatrixToImageWriter）
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = (Graphics2D) image.getGraphics();

            // 设置背景色（白色）
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            // 设置二维码颜色（黑色）
            graphics.setColor(Color.BLACK);

            // 逐像素绘制二维码
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (bitMatrix.get(x, y)) {
                        graphics.fillRect(x, y, 1, 1);
                    }
                }
            }
            graphics.dispose();

            // 转换为Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            log.info("二维码Base64生成成功，长度：{}", base64.length());
            return base64;

        } catch (Exception e) {
            log.error("生成二维码失败", e);
            return null;
        }
    }
}