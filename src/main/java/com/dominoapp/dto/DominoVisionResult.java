package com.dominoapp.dto;

//public class DominoVisionResult {
//    private final int points;
//    private final String processedImage;
//
//    public DominoVisionResult(int points, String processedImage) {
//        this.points = points;
//        this.processedImage = processedImage;
//    }
//
//    public int getPoints() {
//        return points;
//    }
//
//    public String getProcessedImage() {
//        return processedImage;
//    }
//}


public class DominoVisionResult {
    private boolean success;
    private int points;
    private String debugImage;  // ← debe ser "debugImage", no "processedImage"

    public DominoVisionResult(int points, String debugImage) {
        this.success = points >= 0;
        this.points = Math.max(points, 0);
        this.debugImage = debugImage;
    }

    public boolean isSuccess() { return success; }
    public int getPoints() { return points; }
    public String getDebugImage() { return debugImage; }
}