package com.project.subing.domain.budget.entity;

import com.project.subing.domain.common.SoftDeletableEntity;
import com.project.subing.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "budgets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "year", "month"})
})
@SQLDelete(sql = "UPDATE budgets SET del_yn = 'Y' WHERE id = ?")
@SQLRestriction("del_yn = 'N'")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Budget extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "monthly_limit", nullable = false)
    private Long monthlyLimit;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    public void updateMonthlyLimit(Long newLimit) {
        this.monthlyLimit = newLimit;
    }
}
