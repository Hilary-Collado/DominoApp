package com.dominoapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/domino")
public class DominoScanController {

    @PostMapping("/scan-base64")
    public ResponseEntity<Map<String, Object>> scanBase64(@RequestBody Map<String, String> request) {
        String imageBase64 = request.get("image");

        if (imageBase64 == null || imageBase64.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Imagen Vacia"
            ));
        }

        int detectedPoints = 0;

        return ResponseEntity.ok(Map.of(
                "success", true,
                "points", detectedPoints,
                "message", "Imagen recibida correctamente"
        ));
    }
}
