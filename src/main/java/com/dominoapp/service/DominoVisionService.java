package com.dominoapp.service;

import com.dominoapp.dto.DominoVisionResult;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class DominoVisionService {

    static {
        OpenCV.loadLocally();
    }

    public int detectPoints(String base64Image) {
        try {
            String cleanBase64 = base64Image
                    .replace("data:image/png;base64,", "")
                    .replace("data:image/jpeg;base64,", "");

            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            Mat image = Imgcodecs.imdecode(
                    new MatOfByte(imageBytes),
                    Imgcodecs.IMREAD_COLOR
            );

            if (image.empty()) {
                return 0;
            }

            // ESCALA DE GRISES
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

            // MEJORAR CONTRASTE
            Mat equalized = new Mat();
            Imgproc.equalizeHist(gray, equalized);

            // BLUR
            Mat blur = new Mat();
            Imgproc.GaussianBlur(equalized, blur, new Size(7, 7), 2);

            // THRESHOLD
            Mat threshold = new Mat();

            Imgproc.adaptiveThreshold(
                    blur,
                    threshold,
                    255,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY_INV,
                    11,
                    2
            );

            // DETECTAR CÍRCULOS
            Mat circles = new Mat();

            Imgproc.HoughCircles(
                    blur,
                    circles,
                    Imgproc.HOUGH_GRADIENT,
                    1.2,
                    18,
                    120,
                    22,
                    4,
                    12
            );

//            int detected = circles.cols();
//
//            return Math.max(detected, 0);

            int count = 0;

            for (int i = 0; i < circles.cols(); i++) {
                double[] c = circles.get(0, i);

                if (c == null || c.length < 3) {
                    continue;
                }

                double radius = c[2];

                if (radius >= 4 && radius <= 12) {
                    count++;
                }
            }

            return count;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    public DominoVisionResult detectPointsWithDebugImage(String base64Image) {
        try {
            String cleanBase64 = base64Image
                    .replace("data:image/png;base64,", "")
                    .replace("data:image/jpeg;base64,", "");

            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            Mat image = Imgcodecs.imdecode(
                    new MatOfByte(imageBytes),
                    Imgcodecs.IMREAD_COLOR
            );

            if (image.empty()) {
                return new DominoVisionResult(0, null);
            }

            Mat hsv = new Mat();
            Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);

            Mat whiteMask = new Mat();

            Core.inRange(
                    hsv,
                    new Scalar(0, 0, 130),
                    new Scalar(180, 80, 255),
                    whiteMask
            );

            Mat kernel = Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT,
                    new Size(5, 5)
            );

            Imgproc.morphologyEx(whiteMask, whiteMask, Imgproc.MORPH_CLOSE, kernel);
            Imgproc.morphologyEx(whiteMask, whiteMask, Imgproc.MORPH_OPEN, kernel);

            Mat onlyDominoes = new Mat();
            image.copyTo(onlyDominoes, whiteMask);


            Mat gray = new Mat();
            Imgproc.cvtColor(onlyDominoes, gray, Imgproc.COLOR_BGR2GRAY);

            Mat equalized = new Mat();
            Imgproc.equalizeHist(gray, equalized);

            Mat blur = new Mat();
            Imgproc.GaussianBlur(equalized, blur, new Size(7, 7), 2);

            Mat circles = new Mat();

            Imgproc.HoughCircles(
                    blur,
                    circles,
                    Imgproc.HOUGH_GRADIENT,
                    1.2,
                    18,
                    120,
                    22,
                    4,
                    12
            );

            int count = 0;

            for (int i = 0; i < circles.cols(); i++) {
                double[] c = circles.get(0, i);

                if (c == null || c.length < 3) {
                    continue;
                }

                int x = (int) Math.round(c[0]);
                int y = (int) Math.round(c[1]);
                int radius = (int) Math.round(c[2]);

                if (radius >= 4 && radius <= 12) {
                    count++;

                    Imgproc.circle(
                            image,
                            new Point(x, y),
                            radius,
                            new Scalar(0, 255, 0),
                            3
                    );

                    Imgproc.circle(
                            image,
                            new Point(x, y),
                            2,
                            new Scalar(0, 0, 255),
                            3
                    );
                }
            }

            MatOfByte output = new MatOfByte();
            Imgcodecs.imencode(".png", image, output);

            String processedImage = "data:image/png;base64," +
                    Base64.getEncoder().encodeToString(output.toArray());

            return new DominoVisionResult(count, processedImage);

        } catch (Exception e) {
            e.printStackTrace();
            return new DominoVisionResult(0, null);
        }
    }
}