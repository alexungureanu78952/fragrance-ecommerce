package com.fragrance.ecommerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    protected Role() {
    }

    public Role(UUID id, String code) {
        this.id = id;
        this.code = code;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }
}
