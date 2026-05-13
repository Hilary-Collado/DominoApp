package com.dominoapp.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Route(value = "", layout = MainLayout.class)
@CssImport("./css/style.css")
public class GameBoardView extends VerticalLayout {

    private boolean targetSelectorOpen = false;
    private int selectedTarget = 50;
    private HorizontalLayout targetSelectorLayout;

    private boolean playersSelectorOpen = false;
    private int selectedPlayers = 2;
    private HorizontalLayout topControlsLayout;

    private Div playersGrid;

    private boolean gameFinished = false;
    private Playerstate winner = null;

    private final java.util.List<Playerstate> players = new java.util.ArrayList<>();

    private static class Playerstate {
        String name;
        int gamesWon;

        List<Integer> rounds = new ArrayList<>();

        Playerstate(String name) {
            this.name = name;
        }

        int totalScore() {
            return rounds.stream().mapToInt(Integer::intValue).sum();
        }
    }

    public GameBoardView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        getStyle().set("min-height", "calc(100vh - 64px)").set("background", "linear-gradient(135deg, #d8c7a3, #b8a77f)").set("padding", "18px").set("box-sizing", "border-box");
        add(topControls(), playersGrid(), bottomBar());
    }

    private Div playersGrid() {
        playersGrid = new Div();

        playersGrid.getStyle().set("display", "grid").set("width", "100%").set("max-width", "720px").set("margin", "0 auto").set("flex", "1");

        refreshPlayersGrid();

        playersGrid.addClassName("players-grid");

        return playersGrid;
    }

    private HorizontalLayout topBar() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        layout.setAlignItems(Alignment.CENTER);

        Button settings = new Button("⚙");
        styleIconButton(settings);

        layout.add(settings);
        return layout;
    }

    private HorizontalLayout targetSelector() {
        targetSelectorLayout = new HorizontalLayout();
        targetSelectorLayout.setWidthFull();
        targetSelectorLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        targetSelectorLayout.setAlignItems(Alignment.CENTER);

        targetSelectorLayout.getStyle().set("gap", "8px").set("margin-top", "18px").set("margin-bottom", "24px").set("flex-wrap", "wrap").set("background", "red");

        refreshTargetSelector();

        return targetSelectorLayout;
    }

    private HorizontalLayout bottomBar() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setAlignItems(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        layout.getStyle().set("margin-top", "24px");

        Button history = new Button("↶");
        styleIconButton(history);

        Button restart = new Button("RESTART");
        restart.getStyle().set("background", "transparent").set("color", "red").set("border", "3px solid red").set("border-radius", "14px").set("font-weight", "900").set("font-size", "24px").set("padding", "6px 28px").set("cursor", "pointer");

        layout.add(history, restart);
        return layout;
    }

    private VerticalLayout topControls() {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setWidthFull();
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setAlignItems(Alignment.CENTER);

        wrapper.getStyle().set("margin-top", "18px").set("margin-bottom", "60px");

        Button camera = cameraButton();

        topControlsLayout = new HorizontalLayout();
        topControlsLayout.setWidth("100%");
        topControlsLayout.setMaxWidth("260px");
        topControlsLayout.setAlignItems(Alignment.CENTER);
        topControlsLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        topControlsLayout.getStyle().set("margin-top", "18px").set("gap", "28px").set("box-sizing", "border-box");

        refreshTopControls();

        wrapper.add(camera, topControlsLayout);
        return wrapper;
    }

    private VerticalLayout playerCard(Playerstate player) {
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setWidthFull();

        wrapper.getStyle().set("align-items", "flex-end").set("background", "transparent");

        HorizontalLayout card = new HorizontalLayout();
        card.setWidthFull();
        card.setAlignItems(Alignment.CENTER);
        card.setJustifyContentMode(JustifyContentMode.BETWEEN);

        card.addClassName("player-card");

        card.getStyle().set("background", "rgba(255,255,255,0.82)").set("border-radius", "14px").set("box-shadow", "0 6px 16px rgba(0,0,0,0.16)").set("padding", "8px 12px").set("box-sizing", "border-box");

        Button nameButton = new Button(player.name);
        nameButton.getStyle().set("background", "transparent").set("border", "none").set("font-size", "22px").set("font-weight", "900").set("color", "#000").set("padding", "0").set("cursor", "pointer");

        nameButton.addClickListener(event -> openEditNameDialog(player));

        Button add = new Button("+");

        add.addClickListener(event -> {
            if (!gameFinished) {
                openAddPointsDialog(player);
            }
        });

        add.getStyle().set("background", "transparent").set("border", "none").set("font-size", "34px").set("font-weight", "400").set("color", "#000").set("cursor", "pointer");

        Span gamesWon = new Span(String.valueOf(player.gamesWon));
        gamesWon.getStyle().set("font-size", "52px").set("font-weight", "900").set("color", "rgba(0,0,0,0.25)").set("line-height", "1");

        card.add(nameButton, add, gamesWon);

        VerticalLayout roundsLayout = new VerticalLayout();
        roundsLayout.setPadding(false);
        roundsLayout.setSpacing(false);
        roundsLayout.setWidthFull();

        roundsLayout.getStyle().set("padding", "8px 0 0 0");

        int roundNumber = 1;

        for (Integer points : player.rounds) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setAlignItems(Alignment.CENTER);
            row.setJustifyContentMode(JustifyContentMode.BETWEEN);

            row.getStyle().set("border-bottom", "1px solid rgba(255,255,255,0.55)").set("padding", "10px 8px");

            Span number = new Span(String.valueOf(roundNumber++));
            number.getStyle().set("color", "rgba(255,255,255,0.75)").set("font-size", "18px");

            Span value = new Span(String.valueOf(points));
            value.getStyle().set("font-size", "34px").set("font-weight", "900").set("color", "#000");

            row.add(number, value);
            roundsLayout.add(row);
        }

        HorizontalLayout totalRow = new HorizontalLayout();
        totalRow.setWidthFull();
        totalRow.setAlignItems(Alignment.CENTER);
        totalRow.setJustifyContentMode(JustifyContentMode.CENTER);

        totalRow.getStyle().set("height", "80px").set("padding-top", "24px").set("box-sizing", "border-box").set("background", "transparent");

        Span total = new Span(player.totalScore() + " /" + selectedTarget);

//        AQUI ESTA EL TOTAL DE PUNTOS
        total.getStyle().set("font-size", "40px").set("font-weight", "900").set("line-height", "1").set("color", "blue");

        totalRow.add(total);

        wrapper.add(card, roundsLayout, totalRow);

        return wrapper;
    }

    private void refreshTopControls() {
        topControlsLayout.removeAll();

        targetSelectorLayout = new HorizontalLayout();
        targetSelectorLayout.setAlignItems(Alignment.CENTER);
        targetSelectorLayout.getStyle().set("gap", "8px").set("flex-wrap", "wrap");

        refreshTargetSelector();

        HorizontalLayout playersSelector = new HorizontalLayout();
        playersSelector.setAlignItems(Alignment.CENTER);
        playersSelector.getStyle().set("gap", "8px").set("flex-wrap", "wrap");

        if (!playersSelectorOpen) {
            Button players = new Button("👥 " + selectedPlayers + " ❯");
            styleDarkButton(players);

            players.addClickListener(event -> {
                playersSelectorOpen = true;
                refreshTopControls();
            });

            playersSelector.add(players);
        } else {
            for (int amount : new int[]{2, 3, 4}) {
                Button button = new Button(String.valueOf(amount));
                styleDarkButton(button);

                if (amount == selectedPlayers) {
                    button.getStyle().set("background", "#ffffff").set("color", "#111111");
                }

                button.addClickListener(event -> {
                    selectedPlayers = amount;
                    playersSelectorOpen = false;
                    refreshTopControls();
                    refreshPlayersGrid();
                });

                playersSelector.add(button);
            }
        }
        topControlsLayout.add(targetSelectorLayout, playersSelector);
    }

    private void refreshTargetSelector() {
        targetSelectorLayout.removeAll();

        if (!targetSelectorOpen) {
            Button selected = new Button(selectedTarget + " >");
            styleDarkButton(selected);

            selected.addClickListener(event -> {
                targetSelectorOpen = true;
//                refreshTargetSelector();
                refreshTopControls();
            });


            targetSelectorLayout.add(selected);
            return;
        }

        int[] targets = {50, 100, 120, 200};

        for (int target : targets) {
            Button button = new Button(String.valueOf(target));
            styleDarkButton(button);

            if (target == selectedTarget) {
                button.getStyle().set("background", "#ffffff").set("color", "#111111");
            }

            button.addClickListener(event -> {
                selectedTarget = target;
                targetSelectorOpen = false;
                refreshTopControls();
                refreshPlayersGrid();
            });
            targetSelectorLayout.add(button);
        }

        Button custom = new Button("...");
        styleDarkButton(custom);

        custom.addClickListener(event -> openCustomTargetDialog());

        targetSelectorLayout.add(custom);
    }

    private void refreshPlayersGrid() {
        if (playersGrid == null) {
            return;
        }

        syncPlayers();

        playersGrid.removeAll();

        playersGrid.getStyle().set("grid-template-columns", "repeat(2, minmax(0, 1fr))").set("gap", "20px").set("align-items", "start");

        for (Playerstate player : players) {
            playersGrid.add(playerCard(player));
        }
    }

    private void openCustomTargetDialog() {
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.setWidth("300px");

        com.vaadin.flow.component.textfield.IntegerField targetField = new com.vaadin.flow.component.textfield.IntegerField("Cantidad");

        targetField.setWidthFull();
        targetField.setMin(1);
        targetField.setStepButtonsVisible(true);
        targetField.setPlaceholder("Ej: 120");

        Button save = new Button("Aceptar", event -> {
            Integer value = targetField.getValue();

            if (value != null && value > 0) {
                selectedTarget = value;
                targetSelectorOpen = false;
                refreshTopControls();
                refreshPlayersGrid();
                dialog.close();
            }
        });

        save.setWidthFull();
        save.getStyle().set("background", "#101820").set("color", "#ffffff").set("border-radius", "10px");

        content.add(targetField, save);
        dialog.add(content);
        dialog.open();
    }

    private void styleDarkButton(Button button) {
        button.getStyle().set("background", "#000").set("color", "#fff").set("border-radius", "10px").set("font-size", "22px").set("font-weight", "900").set("padding", "2px 14px").set("height", "34px").set("min-width", "82px").set("cursor", "pointer").set("box-shadow", "0 2px 5px rgba(0,0,0,0.25)");
    }

    private void styleIconButton(Button button) {
        button.getStyle().set("background", "transparent").set("color", "#ffffff").set("font-size", "34px").set("border", "none").set("cursor", "pointer");
    }

    private void syncPlayers() {
        while (players.size() < selectedPlayers) {
            players.add(new Playerstate("Jugador " + (players.size() + 1)));
        }

        while (players.size() > selectedPlayers) {
            players.remove(players.size() - 1);
        }
    }

    private void openEditNameDialog(Playerstate player) {
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();

        VerticalLayout content = new VerticalLayout();
        content.setWidth("320px");
        content.setPadding(true);
        content.setSpacing(true);

        com.vaadin.flow.component.textfield.TextField nameField = new com.vaadin.flow.component.textfield.TextField("Nombre del jugador");

        nameField.setWidthFull();
        nameField.setValue(player.name);

        Button save = new Button("Guardar", event -> {
            String value = nameField.getValue();

            if (value != null && !value.trim().isEmpty()) {
                player.name = value.trim();
                refreshPlayersGrid();
                dialog.close();
            }
        });

        save.setWidthFull();
        save.getStyle().set("background", "#101820").set("color", "#ffffff").set("border-radius", "10px");

        content.add(nameField, save);
        dialog.add(content);
        dialog.open();
    }

    private void openAddPointsDialog(Playerstate player) {
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();

        VerticalLayout content = new VerticalLayout();
        content.setWidth("320px");
        content.setPadding(true);
        content.setSpacing(true);

        com.vaadin.flow.component.textfield.IntegerField pointsField = new com.vaadin.flow.component.textfield.IntegerField("Tantos");

        pointsField.setWidthFull();
        pointsField.setMin(0);
        pointsField.setStepButtonsVisible(true);
        pointsField.setPlaceholder("Ej: 50");

        Button save = new Button("Sumar puntos", event -> {
            Integer value = pointsField.getValue();

            if (value != null && value >= 0) {
//                player.rounds.add(value);
                addPoints(player, value);

//                if (player.totalScore() >= selectedTarget) {
//                    player.gamesWon++;
//                }
//
//                refreshPlayersGrid();
                dialog.close();
            }
        });

        save.setWidthFull();
        save.getStyle().set("background", "#101820").set("color", "#ffffff").set("border-radius", "10px");

        content.add(pointsField, save);
        dialog.add(content);
        dialog.open();
    }

    private void addPoints(Playerstate player, int points) {
        if (gameFinished || points <= 0) {
            return;
        }

        int currentTotal = player.totalScore();
        int newTotal = currentTotal + points;

        if (newTotal >= selectedTarget) {
            int allowedPoints = selectedTarget - currentTotal;

            if (allowedPoints > 0) {
                player.rounds.add(allowedPoints);
            }

            player.gamesWon++;
            winner = player;
            gameFinished = true;

            refreshPlayersGrid();
            showWinnerDialog(player);
            return;
        }

        player.rounds.add(points);
        refreshPlayersGrid();
    }

    private void showWinnerDialog(Playerstate player) {
        launchConfetti();

        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        VerticalLayout content = new VerticalLayout();
        content.setWidth("360px");
        content.setPadding(true);
        content.setSpacing(true);
        content.setAlignItems(Alignment.CENTER);

        H1 title = new H1("🎉 Ganador 🎉");
        title.getStyle().set("margin", "0").set("color", "#101820").set("font-size", "32px").set("text-align", "center");

        H2 winnerName = new H2(player.name);
        winnerName.getStyle().set("margin", "0").set("font-size", "28px").set("color", "#000");

        H3 score = new H3("Puntos: " + player.totalScore() + " / " + selectedTarget);
        score.getStyle().set("margin", "0").set("color", "#555");

        Span games = new Span("Partidas ganadas: " + player.gamesWon);
        games.getStyle().set("font-size", "18px").set("font-weight", "700");

        Button newRound = new Button("Nueva ronda", event -> {
            resetRound();
            dialog.close();
        });

        newRound.setWidthFull();
        newRound.getStyle().set("background", "#101820").set("color", "#ffffff").set("border-radius", "12px").set("font-weight", "900").set("font-size", "18px").set("height", "44px").set("cursor", "pointer");

        content.add(title, winnerName, score, games, newRound);
        dialog.add(content);
        dialog.open();
    }

    private void resetRound() {
        for (Playerstate player : players) {
            player.rounds.clear();
        }

        gameFinished = false;
        winner = null;

        refreshPlayersGrid();
    }

    private void launchConfetti() {
        getElement().executeJs("""
                    const colors = ['#ff0000', '#00c853', '#2962ff', '#ffd600', '#ff6d00', '#aa00ff'];
                    const confettiCount = 120;
                
                    for (let i = 0; i < confettiCount; i++) {
                        const confetti = document.createElement('div');
                
                        confetti.style.position = 'fixed';
                        confetti.style.top = '-20px';
                        confetti.style.left = Math.random() * 100 + 'vw';
                        confetti.style.width = '10px';
                        confetti.style.height = '16px';
                        confetti.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
                        confetti.style.zIndex = '99999';
                        confetti.style.opacity = '0.9';
                        confetti.style.borderRadius = '2px';
                        confetti.style.transform = 'rotate(' + Math.random() * 360 + 'deg)';
                        confetti.style.pointerEvents = 'none';
                
                        document.body.appendChild(confetti);
                
                        const fallDuration = 2500 + Math.random() * 2000;
                        const horizontalMove = (Math.random() - 0.5) * 300;
                
                        confetti.animate([
                            {
                                transform: 'translate(0, 0) rotate(0deg)',
                                opacity: 1
                            },
                            {
                                transform: 'translate(' + horizontalMove + 'px, 110vh) rotate(720deg)',
                                opacity: 0
                            }
                        ], {
                            duration: fallDuration,
                            easing: 'cubic-bezier(.2,.7,.4,1)'
                        });
                
                        setTimeout(() => {
                            confetti.remove();
                        }, fallDuration);
                    }
                """);
    }

    private Button cameraButton() {
        Button camera = new Button("📷 Cámara");

        camera.getStyle().set("background", "#ffffff").set("color", "#000").set("border-radius", "12px").set("font-weight", "900").set("font-size", "18px").set("padding", "8px 24px").set("height", "42px").set("cursor", "pointer").set("box-shadow", "0 4px 12px rgba(0,0,0,0.18)");

        camera.addClickListener(event -> openCameraDialog());

        return camera;
    }

    private void openCameraDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("430px");
        dialog.setMaxWidth("95vw");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.setWidthFull();

        Div cameraBox = new Div();
        cameraBox.setWidthFull();

        cameraBox.getElement().setProperty(
                "innerHTML",
                """
                        <video id="camera-video" autoplay playsinline 
                            style="width:100%; border-radius:14px; background:#000;"></video>
                        
                        <canvas id="camera-canvas" 
                            style="display:none;"></canvas>
                        
                        <img id="camera-preview" 
                            style="display:none; width:100%; border-radius:14px; margin-top:10px;" />
                        
                            <img id="processed-preview"
                                                style="display:none; width:100%; border-radius:14px; margin-top:10px;" />
                        """);

        Span result = new Span("Toma una foto para analizar la jugada.");
        result.getStyle().set("font-weight", "700").set("color", "#333");

        Button takePhoto = new Button("📸 Tomar foto");
        takePhoto.setWidthFull();
        takePhoto.getStyle().set("background", "#101820").set("color", "#fff").set("border-radius", "10px").set("font-weight", "800");

        takePhoto.addClickListener(event -> {
            getElement().executeJs("""
                        const video = document.getElementById('camera-video');
                        const canvas = document.getElementById('camera-canvas');
                        const preview = document.getElementById('camera-preview');
                    
                        if (!video || !canvas) {
                            return Promise.resolve({ success: false, message: 'No se encontró la cámara' });
                        }
                    
                        canvas.width = video.videoWidth;
                        canvas.height = video.videoHeight;
                    
                        const ctx = canvas.getContext('2d');
                        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
                    
                        const imageBase64 = canvas.toDataURL('image/png');
                    
                        preview.src = imageBase64;
                        preview.style.display = 'block';
                    
                        return fetch('/api/domino/scan-base64', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({
                                image: imageBase64
                            })
                        })
                        .then(response => response.json())
                        .then(data => data)
                        .catch(error => {
                            return {
                                success: false,
                                message: error.message
                            };
                        });
                    """).then(jsonValue -> {
                boolean success = jsonValue.get("success").asBoolean();

                if (success) {
                    int points = jsonValue.get("points").asInt();
                    result.setText("Puntos detectados: " + points);

                    String processedImage = jsonValue.has("processedImage")
                            && !jsonValue.get("processedImage").isNull()
                            ? jsonValue.get("processedImage").asText()
                            : null;

                    if (processedImage != null) {
                        getElement().executeJs("""
                                    const processed = document.getElementById('processed-preview');
                                    if (processed) {
                                        processed.src = $0;
                                        processed.style.display = 'block';
                                    }
                                """, processedImage);
                    }

                } else {
                    String message = jsonValue.has("message")
                            ? jsonValue.get("message").asText()
                            : "Error desconocido";

                    result.setText("Error: " + message);
                }
            });
    });

    Button close = new Button("Cerrar", event -> dialog.close());
        close.setWidthFull();

        Upload upload = imageUpload(result);
        content.add(cameraBox, result, takePhoto, upload, close);

        dialog.add(content);

        dialog.addOpenedChangeListener(event ->

    {
        if (event.isOpened()) {
            getElement().executeJs("""
                        setTimeout(() => {
                            const video = document.getElementById('camera-video');
                    
                            if (!video) {
                                console.error('No se encontró el video');
                                return;
                            }
                    
                            if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
                                alert('Este navegador no permite acceso a cámara. Usa HTTPS o Chrome.');
                                return;
                            }
                    
                            navigator.mediaDevices.getUserMedia({
                                video: {
                                    facingMode: { ideal: 'environment' },
                                    width: { ideal: 1280 },
                                    height: { ideal: 720 }
                                },
                                audio: false
                            }).then(stream => {
                                window.dominoCameraStream = stream;
                                video.srcObject = stream;
                            }).catch(error => {
                                alert('No se pudo abrir la cámara: ' + error.message);
                                console.error(error);
                            });
                        }, 300);
                    """);
        } else {
            getElement().executeJs("""
                        if (window.dominoCameraStream) {
                            window.dominoCameraStream.getTracks().forEach(track => track.stop());
                            window.dominoCameraStream = null;
                        }
                    """);
        }
    });

        dialog.open();
}



    private Upload imageUpload(Span result) {

        Upload upload = new Upload(
                UploadHandler.inMemory((metadata, data) -> {
                    try {
                        String mimeType = metadata.contentType();
                        String base64 = Base64.getEncoder().encodeToString(data);
                        String imageBase64 = "data:" + mimeType + ";base64," + base64;

                        getUI().ifPresent(ui -> ui.access(() -> processUploadedImage(imageBase64, result)));

                    } catch (Exception e) {
                        getUI().ifPresent(ui -> ui.access(() ->
                                result.setText("Error cargando imagen: " + e.getMessage())
                        ));
                    }
                })
        );

        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/jpg", "image/webp");
        upload.setMaxFiles(1);
        upload.setDropAllowed(true);

        upload.setUploadButton(new Button("📁 Cargar imagen"));
        upload.setDropLabel(new Span("Arrastra una imagen aquí"));

        upload.getStyle()
                .set("width", "100%")
                .set("border", "2px dashed #ccc")
                .set("border-radius", "12px")
                .set("padding", "10px")
                .set("box-sizing", "border-box");

        return upload;
    }

    private void processUploadedImage(String imageBase64, Span result) {
        getElement().executeJs("""
        const preview = document.getElementById('camera-preview');
        if (preview) {
            preview.src = $0;
            preview.style.display = 'block';
        }

        return fetch('/api/domino/scan-base64', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                image: $0
            })
        })
        .then(response => response.json())
        .then(data => data)
        .catch(error => {
            return {
                success: false,
                message: error.message
            };
        });
    """, imageBase64).then(jsonValue -> {
            boolean success = jsonValue.get("success").asBoolean();

            if (success) {
                int points = jsonValue.get("points").asInt();
                result.setText("Puntos detectados: " + points);

                String processedImage = jsonValue.has("processedImage")
                        && !jsonValue.get("processedImage").isNull()
                        ? jsonValue.get("processedImage").asText()
                        : null;

                if (processedImage != null) {
                    getElement().executeJs("""
                    const processed = document.getElementById('processed-preview');
                    if (processed) {
                        processed.src = $0;
                        processed.style.display = 'block';
                    }
                """, processedImage);
                }
            } else {
                String message = jsonValue.has("message")
                        ? jsonValue.get("message").asText()
                        : "Error desconocido";

                result.setText("Error: " + message);
            }
        });
    }

}
