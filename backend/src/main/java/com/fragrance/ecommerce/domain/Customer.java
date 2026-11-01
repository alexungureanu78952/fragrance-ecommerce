package com.fragrance.ecommerce.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
@PrimaryKeyJoinColumn(name = "user_id")
public class Customer extends User {

    protected Customer() {
    }

    public Customer(String email, String passwordHash, String displayName, Role role) {
        super(email, passwordHash, displayName, role);
    }
}
