package com.workspace.api.util.domain;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum Category {
    WORK("trabalho"),
    STUDY("estudo"),
    HEALTH("saude"),
    MIND("mente"),
    WELLBEING("bem-estar"),
    PERSONAL("pessoal"),
    PROJECT("projetos");

    private final String id;

    Category(String id) {
        this.id = id;
    }

    public static Optional<Category> fromId(String id) {
        return Arrays.stream(values())
                .filter(category -> category.id.equals(id))
                .findFirst();
    }
}
