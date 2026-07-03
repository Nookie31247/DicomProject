package com.allegro.dicomback.entity;

import lombok.Getter;

@Getter
public enum UserType {
    MEDICAL("의료진"),
    RESEARCHER("연구원");

    private final String typeString;

    UserType(String typeString) {
        this.typeString = typeString;
    }

    public static UserType fromTypeString(String typeString) {
        for (UserType userType : UserType.values()) {
            if (userType.typeString.equals(typeString)) {
                return userType;
            }
        }

        throw new IllegalArgumentException("Unknown UserType: " + typeString);
    }
}
