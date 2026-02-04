package com.project.subing.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "del_yn", nullable = false, length = 1)
    private String delYn = "N";

    public void softDelete() {
        this.delYn = "Y";
    }

    public void restore() {
        this.delYn = "N";
    }

    public boolean isDeleted() {
        return "Y".equals(this.delYn);
    }
}
