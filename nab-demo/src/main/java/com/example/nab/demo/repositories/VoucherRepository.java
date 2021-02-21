package com.example.nab.demo.repositories;

import com.example.nab.demo.models.Voucher;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends CrudRepository<Voucher, String> {

    List<Voucher> findAllByUserPhoneNumber(String phoneNumber);
    Optional<Voucher> findByPaymentTransactionId(String paymentTransactionId);
    List<Voucher> findAllByIsSentPhoneMessageAndUserPhoneNumberNotNull(boolean isSentPhoneMessage);
}
