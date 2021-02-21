package com.example.nab.demo.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "VARCHAR(255)")
    private String id;

    private String userName;
    private String userPhoneNumber;
    private String voucherType;
    private String serialNumbers;
    private String paymentTransactionId;
    private LocalDateTime createdTime;
    private LocalDateTime expiredTime;

    /**
     * utility property
     */

    private VoucherStatus status;
    private boolean isSentPhoneMessage = false;

    public Voucher(String userName, String userPhoneNumber, String voucherType, String paymentTransactionId) {
        this.userName = userName;
        this.userPhoneNumber = userPhoneNumber;
        this.voucherType = voucherType;
        this.paymentTransactionId = paymentTransactionId;
        this.status = VoucherStatus.CREATING;
    }
}
