package org.nextgate.nextgatebackend.authentication_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "account_table", indexes = {
        @Index(name = "idx_phone_number", columnList = "phoneNumber"),
        @Index(name = "idx_email", columnList = "email")
})
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String phoneNumber;
    private String userName;

    private String firstName;
    private String lastName;
    private String middleName;

    @JsonIgnore
    private String password;

    private String email;

    private boolean locked = false;

    private boolean twoFactorEnabled = false;

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime editedAt;

    private String twoFactorSecret;
    private Boolean isVerified;

    private Boolean isEmailVerified;

    private Boolean isPhoneVerified;

    private String lockedReason;

    @Column(name = "is_bucket_created", nullable = false)
    private boolean isBucketCreated;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "account_roles",
            joinColumns = @JoinColumn(name = "account_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "roleId"))
    private Set<Roles> roles;

//    @JsonIgnore
//    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<OrganisationEntity> userOrganisations;

    // Add the getAccountId() method that returns the id field
    public UUID getAccountId() {
        return this.id;
    }

}
