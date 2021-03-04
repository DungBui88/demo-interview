package com.example.nab.demo.repositories;

import com.example.nab.demo.models.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String>, RevisionRepository<Voucher, String, Integer> {

    List<Voucher> findAllByUserPhoneNumber(String phoneNumber);
    Optional<Voucher> findByPaymentTransactionId(String paymentTransactionId);
}
