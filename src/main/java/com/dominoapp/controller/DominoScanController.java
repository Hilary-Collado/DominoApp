package com.dominoapp.controller;

import com.dominoapp.dto.DominoScanResponse;
import com.dominoapp.dto.DominoVisionResult;
import com.dominoapp.service.DominoVisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/domino")
public class DominoScanController {

    private final DominoVisionService visionService;

    public DominoScanController(DominoVisionService visionService) {
        this.visionService = visionService;
    }

    @PostMapping("/scan-base64")
    public ResponseEntity<DominoScanResponse> scanBase64(@RequestBody java.util.Map<String, String> request) {
        String imageBase64 = request.get("image");

        if (imageBase64 == null || imageBase64.isBlank()) {
            return ResponseEntity.badRequest().body(
                    new DominoScanResponse(false, 0, "Imagen vacía", null)
            );
        }

        DominoVisionResult result = visionService.detectPointsWithDebugImage(imageBase64);

        return ResponseEntity.ok(
                new DominoScanResponse(
                        true,
                        result.getPoints(),
                        "Imagen procesada",
                        result.getProcessedImage()
                )
        );
    }



}