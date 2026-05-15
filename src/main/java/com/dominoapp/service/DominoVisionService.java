package com.dominoapp.service;

import com.dominoapp.dto.DominoVisionResult;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class DominoVisionService {

    static {
        OpenCV.loadLocally();
    }

    public DominoVisionResult detectPointsWithDebugImage(String base64Image) {
        try {
            // ── 1. DECODIFICAR ────────────────────────────────────────────────────
            String cleanBase64 = base64Image.substring(base64Image.indexOf(",") + 1);
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);
            Mat original = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
            if (original.empty()) return new DominoVisionResult(0, null);

            // Escalar a máximo 1000px para uniformidad
            Mat image = scaleIfNeeded(original, 1000);
            Mat debug = image.clone();

            // ── 2. DETECTAR FICHAS (zonas claras / beige) ─────────────────────────
            List<Rect> dominoRects = detectDominoRegions(image, debug);

            // ── 3. CONTAR PUNTOS DENTRO DE CADA FICHA ────────────────────────────
            int totalDots = 0;

            if (!dominoRects.isEmpty()) {
                for (Rect rect : dominoRects) {
                    // Dibujar borde de ficha (azul)
                    Imgproc.rectangle(debug, rect, new Scalar(255, 80, 0), 2);

                    Mat roiGray = extractGrayROI(image, rect);
                    int dots = countDots(roiGray, rect, debug);
                    totalDots += dots;
                }
            } else {
                // Fallback: analizar imagen completa
                Mat gray = new Mat();
                Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
                totalDots = countDots(gray, new Rect(0, 0, image.cols(), image.rows()), debug);
            }

            // ── 4. ETIQUETA FINAL ─────────────────────────────────────────────────
            String label = "Puntos detectados: " + totalDots;
            Imgproc.rectangle(debug, new Point(0, 0), new Point(320, 45),
                    new Scalar(0, 0, 0), -1);
            Imgproc.putText(debug, label, new Point(8, 32),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.9, new Scalar(0, 255, 100), 2);

            // ── 5. EXPORTAR ───────────────────────────────────────────────────────
            MatOfByte output = new MatOfByte();
            Imgcodecs.imencode(".jpg", debug, output);
            String debugBase64 = "data:image/jpeg;base64,"
                    + Base64.getEncoder().encodeToString(output.toArray());

            return new DominoVisionResult(totalDots, debugBase64);

        } catch (Exception e) {
            e.printStackTrace();
            return new DominoVisionResult(-1, null);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // DETECTAR REGIONES DE FICHAS
    // Estrategia: las fichas son zonas rectangulares claras (beige/blanco)
    // ══════════════════════════════════════════════════════════════════════════════
    private List<Rect> detectDominoRegions(Mat image, Mat debug) {
        List<Rect> result = new ArrayList<>();

        // Convertir a HSV para mejor separación de colores
        Mat hsv = new Mat();
        Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);

        // Rango de color beige/crema/blanco (fichas de dominó clásicas)
        // H: 0-40 (amarillo-beige), S: 0-80 (poco saturado), V: 120-255 (claro)
        Mat maskLight = new Mat();
        Core.inRange(hsv,
                new Scalar(0, 0, 130),    // Min: cualquier tono, poca saturación, bastante claro
                new Scalar(40, 110, 255), // Max: tono beige, saturación media, blanco
                maskLight);

        // También capturar fichas muy blancas/grises
        Mat maskWhite = new Mat();
        Core.inRange(hsv,
                new Scalar(0, 0, 160),
                new Scalar(180, 60, 255),
                maskWhite);

        Mat mask = new Mat();
        Core.bitwise_or(maskLight, maskWhite, mask);

        // Morfología para conectar regiones y rellenar huecos (puntos negros dentro)
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 9));
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN,
                Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mask, contours, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double imgArea = image.cols() * image.rows();

        for (MatOfPoint c : contours) {
            double area = Imgproc.contourArea(c);
            // Ficha debe ser entre 1.5% y 55% de la imagen
            if (area < imgArea * 0.015 || area > imgArea * 0.55) continue;

            Rect r = Imgproc.boundingRect(c);
            double ratio = (double) r.width / r.height;
            // Ratio entre 0.2 (muy vertical) y 5.0 (muy horizontal)
            if (ratio < 0.2 || ratio > 5.0) continue;

            result.add(r);
        }

        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // CONTAR PUNTOS EN UNA ROI
    // ══════════════════════════════════════════════════════════════════════════════
    private int countDots(Mat grayROI, Rect offset, Mat debug) {

        // Escalar parámetros según tamaño del ROI
        double roiArea = grayROI.cols() * grayROI.rows();
        double scale = Math.sqrt(roiArea) / 200.0;
        scale = Math.max(0.5, Math.min(scale, 4.0));

        int minDotArea = (int) (18 * scale * scale);
        int maxDotArea = (int) (700 * scale * scale);

        // ── Intento 1: Umbral adaptativo ─────────────────────────────────────────
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(grayROI, blurred, new Size(3, 3), 0);

        Mat adaptive = new Mat();
        int blockSize = computeBlockSize(grayROI, 11);
        Imgproc.adaptiveThreshold(blurred, adaptive, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                blockSize, 6);

        cleanBinaryBorder(adaptive);
        applyMorphOpen(adaptive, 2);

        int count = extractDots(adaptive, offset, debug, minDotArea, maxDotArea, new Scalar(0, 255, 0));

        // ── Intento 2: Umbral global fijo ────────────────────────────────────────
        if (count == 0) {
            Mat global = new Mat();
            // 110 captura grises oscuros típicos de puntos de dominó beige
            Imgproc.threshold(blurred, global, 110, 255, Imgproc.THRESH_BINARY_INV);
            cleanBinaryBorder(global);
            applyMorphOpen(global, 2);
            count = extractDots(global, offset, debug, minDotArea, maxDotArea, new Scalar(0, 200, 255));
        }

        // ── Intento 3: Threshold con Otsu ────────────────────────────────────────
        if (count == 0) {
            Mat otsu = new Mat();
            Imgproc.threshold(blurred, otsu, 0, 255,
                    Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
            cleanBinaryBorder(otsu);
            applyMorphOpen(otsu, 2);
            count = extractDots(otsu, offset, debug, minDotArea, maxDotArea, new Scalar(255, 0, 200));
        }

        // ── Intento 4: HoughCircles ───────────────────────────────────────────────
        if (count == 0) {
            count = houghFallback(grayROI, offset, debug, scale);
        }

        return count;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // EXTRAER CONTORNOS CIRCULARES Y MARCARLOS
    // ══════════════════════════════════════════════════════════════════════════════
    private int extractDots(Mat binary, Rect offset, Mat debug,
                            int minArea, int maxArea, Scalar color) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(binary.clone(), contours, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int count = 0;
        for (MatOfPoint c : contours) {
            double area = Imgproc.contourArea(c);
            if (area < minArea || area > maxArea) continue;

            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double perimeter = Imgproc.arcLength(c2f, true);
            if (perimeter <= 0) continue;

            double circularity = (4 * Math.PI * area) / (perimeter * perimeter);
            if (circularity < 0.50) continue;

            Rect bbox = Imgproc.boundingRect(c);
            double aspect = (double) Math.max(bbox.width, bbox.height)
                    / Math.min(bbox.width, bbox.height);
            if (aspect > 2.5) continue;

            count++;
            Point center = new Point(
                    offset.x + bbox.x + bbox.width / 2.0,
                    offset.y + bbox.y + bbox.height / 2.0);
            int r = Math.max(bbox.width, bbox.height) / 2 + 4;
            Imgproc.circle(debug, center, r, color, 2);
            Imgproc.putText(debug, String.valueOf(count),
                    new Point(center.x + r, center.y - r),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.45,
                    new Scalar(255, 255, 0), 1);
        }
        return count;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // HOUGH FALLBACK
    // ══════════════════════════════════════════════════════════════════════════════
    private int houghFallback(Mat grayROI, Rect offset, Mat debug, double scale) {
        Mat blur = new Mat();
        Imgproc.GaussianBlur(grayROI, blur, new Size(5, 5), 1.5);

        int minR = (int) Math.max(3, 5 * scale);
        int maxR = (int) Math.max(18, 28 * scale);

        Mat circles = new Mat();
        Imgproc.HoughCircles(blur, circles, Imgproc.HOUGH_GRADIENT,
                1, minR * 2.0, 55, 16, minR, maxR);

        if (circles.empty()) return 0;

        int count = 0;
        for (int i = 0; i < circles.cols(); i++) {
            double[] d = circles.get(0, i);
            int cx = (int) d[0], cy = (int) d[1];
            if (cx < 0 || cy < 0 || cx >= grayROI.cols() || cy >= grayROI.rows()) continue;
            // Verificar que el centro es oscuro
            if (grayROI.get(cy, cx)[0] > 155) continue;

            count++;
            Point center = new Point(offset.x + d[0], offset.y + d[1]);
            Imgproc.circle(debug, center, (int) d[2] + 4, new Scalar(0, 140, 255), 2);
        }
        return count;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ══════════════════════════════════════════════════════════════════════════════
    private Mat extractGrayROI(Mat image, Rect rect) {
        Rect safe = new Rect(
                Math.max(0, rect.x), Math.max(0, rect.y),
                Math.min(rect.width, image.cols() - rect.x),
                Math.min(rect.height, image.rows() - rect.y));
        Mat roi = new Mat(image, safe);
        Mat gray = new Mat();
        Imgproc.cvtColor(roi, gray, Imgproc.COLOR_BGR2GRAY);
        return gray;
    }

    private Mat scaleIfNeeded(Mat img, int maxDim) {
        int w = img.cols(), h = img.rows();
        if (w <= maxDim && h <= maxDim) return img;
        double s = (double) maxDim / Math.max(w, h);
        Mat out = new Mat();
        Imgproc.resize(img, out, new Size(w * s, h * s));
        return out;
    }

    /** blockSize para adaptiveThreshold — debe ser impar, ≥ 3 */
    private int computeBlockSize(Mat roi, int base) {
        int size = (int) (Math.min(roi.cols(), roi.rows()) / 12.0);
        size = Math.max(size, base);
        if (size % 2 == 0) size++;
        return size;
    }

    /** Poner a 0 el borde (evita que el contorno de la ficha cuente como punto) */
    private void cleanBinaryBorder(Mat m) {
        int b = 4;
        if (m.rows() <= b * 2 || m.cols() <= b * 2) return;
        Imgproc.rectangle(m, new Point(0, 0),
                new Point(m.cols() - 1, m.rows() - 1), new Scalar(0), b);
    }

    private void applyMorphOpen(Mat m, int kernelSize) {
        Mat k = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,
                new Size(kernelSize, kernelSize));
        Imgproc.morphologyEx(m, m, Imgproc.MORPH_OPEN, k);
    }
}