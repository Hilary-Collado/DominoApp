package com.dominoapp.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

//@Route(value = "" , layout = MainLayout.class)
public class SetupGameView extends VerticalLayout {
    public SetupGameView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        getStyle().set("background", "#f3f4f6").set("padding", "24px");

        VerticalLayout card =  new VerticalLayout();
        card.setWidth("420px");
        card.setMaxWidth("100%");
        card.setSpacing(true);
        card.setSpacing(true);

        card.getStyle()
                .set("background", "white")
                .set("border-radius", "18px")
                .set("box-shadow", "0 10px 30px rgba(0,0,0,0.15)")
                .set("padding", "28px");

        H1 title = new H1("Nueva partida");
        title.getStyle().set("margin", "0");

        Paragraph subtitle = new Paragraph("Configura los jugadores y el modo de juego.");
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "#6b7280");

        ComboBox<Integer> players = new ComboBox<>("Cantidad de jugadores");
        players.setItems(2, 3, 4);
        players.setValue(4);
        players.setWidthFull();

        ComboBox<String> mode = new ComboBox<>("Modo de juego");
        mode.setItems("Individual", "Parejas");
        mode.setValue("Parejas");
        mode.setWidthFull();

        Button start = new Button("Crear partida", event ->
                getUI().ifPresent(ui -> ui.navigate("game"))
        );
        start.setWidthFull();
        start.getStyle()
                .set("height", "44px")
                .set("background", "#101820")
                .set("color", "white")
                .set("border-radius", "10px")
                .set("cursor", "pointer");

        card.add(title, subtitle, players, mode, start);
        add(card);
    }
}
