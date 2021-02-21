package com.example.nab.demo.service;

import com.example.nab.demo.models.Voucher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class PhoneService {

    @Async
    // TODO: can apply retry - recover here when we actual implement it !!
    public void sendVoucherMessage(Voucher toBeSent) {
        Assert.isTrue(toBeSent.getUserPhoneNumber() != null, "Phone number should not be null");
    }

    @Async
    public void sendErrorMessage(Voucher toBeSent) {
        Assert.isTrue(toBeSent.getUserPhoneNumber() != null, "Phone number should not be null");
    }
}
