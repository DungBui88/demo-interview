package com.example.nab.demo.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Audited(withModifiedFlag = true)
public class Voucher implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "voucher_id", columnDefinition = "VARCHAR(255)")
    private String voucherId;

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

    @Version
    private Long version;
    @CreatedBy
    private String createdBy;
    @LastModifiedBy
    private String modifiedBy;

    public Voucher(String userName, String userPhoneNumber, String voucherType, String paymentTransactionId) {
        this.userName = userName;
        this.userPhoneNumber = userPhoneNumber;
        this.voucherType = voucherType;
        this.paymentTransactionId = paymentTransactionId;
        this.status = VoucherStatus.CREATING;
    }
}
