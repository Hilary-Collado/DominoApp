package com.dominoapp.dto;

public class DominoVisionResult {
    private final int points;
    private final String processedImage;

    public DominoVisionResult(int points, String processedImage) {
        this.points = points;
        this.processedImage = processedImage;
    }

    public int getPoints() {
        return points;
    }

    public String getProcessedImage() {
        return processedImage;
    }
}
