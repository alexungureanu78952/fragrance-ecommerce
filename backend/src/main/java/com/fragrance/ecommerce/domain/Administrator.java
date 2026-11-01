package com.fragrance.ecommerce.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "administrators")
@PrimaryKeyJoinColumn(name = "user_id")
public class Administrator extends User {

    protected Administrator() {
    }

    public Administrator(String email, String passwordHash, String displayName, Role role) {
        super(email, passwordHash, displayName, role);
    }
}
