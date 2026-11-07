package com.fragrance.ecommerce.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * An administrator is a customer with additional management rights: they can shop,
 * keep a cart, and place orders like any customer, and also manage products and orders.
 */
@Entity
@Table(name = "administrators")
@PrimaryKeyJoinColumn(name = "user_id")
public class Administrator extends Customer {

    protected Administrator() {
    }

    public Administrator(String email, String passwordHash, String displayName,
                         Role customerRole, Role administratorRole) {
        super(email, passwordHash, displayName, customerRole);
        grantRole(administratorRole);
    }
}
