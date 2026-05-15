package com.dominoapp.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
    }

    private void createHeader() {
        H1 title = new H1("DominoApp");

        title.getStyle()
                .set("color", "white")
                .set("font-size", "22px")
                .set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(title);
        header.setWidthFull();
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);

        header.getStyle()
                .set("height", "64px")
                .set("padding", "0 24px")
                .set("background", "#101820")
                .set("box-shadow", "0 2px 10px rgba(0, 0, 0, 0.25)")
                .set("justify-content", "center");

        addToNavbar(header);
    }
}