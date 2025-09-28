package org.nextgate.nextgatebackend.payment_methods.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "payment_methods", indexes = {

})

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID paymentMethodId;


}
