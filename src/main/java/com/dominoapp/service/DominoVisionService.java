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
            // Limpiar cabecera base64
            String cleanBase64 = base64Image
                    .replace("data:image/png;base64,", "")
                    .replace("data:image/jpeg;base64,", "");

            // Decodificar imagen
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);
            Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);

            if (image.empty()) return new DominoVisionResult(0, null);

            // --- FASE 1: SEGMENTACIÓN ROBUSTA DE DOMINÓS ---

            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

            // Binarización automática con Otsu para encontrar objetos brillantes
            Mat binary = new Mat();
            // Invertimos porque los dominós son más claros que el fondo
            // Esto crea un fondo negro con dominós blancos.
            Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

            // Limpieza morfológica de las formas
            Mat kernel5 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

            // 1. Cierre (MORPH_CLOSE) para tapar agujeros negros dentro de las piezas
            Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, kernel5, new Point(-1,-1), 1);
            // 2. Apertura (MORPH_OPEN) para eliminar ruido pequeño del fondo
            Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_OPEN, kernel5, new Point(-1,-1), 1);

            // Encontrar contornos de las formas detectadas
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            Mat dominoMask = Mat.zeros(image.size(), CvType.CV_8UC1);
            List<Rect> validDominoRects = new ArrayList<>();

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);

                // Filtro de área más realista (las piezas son grandes)
                if (area < 1500) continue;

                // Aproximación de forma para suavizar bordes
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                double perimeter = Imgproc.arcLength(contour2f, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour2f, approx, 0.02 * perimeter, true);

                // Rectángulo contenedor para la geometría
                Rect rect = Imgproc.boundingRect(contour);
                double ratio = (double) rect.width / rect.height;

                // FILTRO GEOMÉTRICO ESTRICTO:
                // Un dominó es un cuadrado (ratio~1.0) o un rectángulo doble (ratio~0.5 o 2.0).
                if (ratio > 0.3 && ratio < 3.0) {
                    // Si pasa los filtros, lo pintamos en la máscara final como blanco puro
                    Imgproc.drawContours(dominoMask, List.of(contour), -1, new Scalar(255), -1);
                    validDominoRects.add(rect);
                    // Debug: Caja azul para la pieza detectada
                    Imgproc.rectangle(image, rect, new Scalar(255, 0, 0), 2);
                }
            }

            // --- FASE 2: CONTEO DE PUNTOS ---

            // Aislar los dominós en escala de grises
            Mat grayIsolated = new Mat();
            Core.bitwise_and(gray, dominoMask, grayIsolated);

            // Binarizar la máscara aislada para resaltar solo los puntos negros
            Mat binaryIsolated = new Mat();
            // Buscamos puntos muy oscuros (negros) en las zonas aisladas
            Imgproc.threshold(grayIsolated, binaryIsolated, 50, 255, Imgproc.THRESH_BINARY_INV);
            // Aplicar máscara otra vez para limpiar lo que no es pieza
            Core.bitwise_and(binaryIsolated, dominoMask, binaryIsolated);

            // Suavizado específico para círculos
            Mat blurForCircles = new Mat();
            Imgproc.medianBlur(binaryIsolated, blurForCircles, 7); // Funciona mejor para eliminar ruido de bordes

            // Detección de círculos (Parámetros ajustados)
            Mat circles = new Mat();
            Imgproc.HoughCircles(
                    blurForCircles,
                    circles,
                    Imgproc.HOUGH_GRADIENT,
                    1.2,   // dp: resolución
                    25,    // minDist: Distancia mínima entre puntos (más grande para evitar dobles)
                    100,   // param1: Sensibilidad de bordes (Canny)
                    12,    // param2: Umbral de detección (subir si detecta ruido, bajar si no detecta)
                    5,     // minRadius: los puntos no son diminutos
                    18     // maxRadius: los puntos no son gigantes
            );

            // --- FASE 3: DIBUJADO Y RESULTADOS ---
            int pointCount = 0;
            if (circles.cols() > 0) {
                pointCount = circles.cols();
                for (int i = 0; i < pointCount; i++) {
                    double[] c = circles.get(0, i);
                    Point center = new Point(Math.round(c[0]), Math.round(c[1]));
                    int radius = (int) Math.round(c[2]);

                    // Pintamos los puntos detectados en verde con centro rojo
                    Imgproc.circle(image, center, radius, new Scalar(0, 255, 0), 2);
                    Imgproc.circle(image, center, 2, new Scalar(0, 0, 255), 3);
                }
            }

            // Codificar imagen resultante
            MatOfByte output = new MatOfByte();
            Imgcodecs.imencode(".png", image, output);
            String processedImage = "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toArray());

            return new DominoVisionResult(pointCount, processedImage);

        } catch (Exception e) {
            e.printStackTrace();
            return new DominoVisionResult(0, null);
        }
    }
}