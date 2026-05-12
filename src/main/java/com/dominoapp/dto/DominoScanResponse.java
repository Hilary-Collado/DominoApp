package com.dominoapp.dto;

public class DominoScanResponse {
    private boolean success;
    private int points;
    private String message;
    private String processedImage;

    public DominoScanResponse(boolean success, int points, String message, String processedImage){
        this.success = success;
        this.points = points;
        this.message = message;
        this.processedImage = processedImage;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getPoints() {
        return points;
    }

    public String getMessage() {
        return message;
    }

    public String getProcessedImage() {
        return processedImage;
    }
}
