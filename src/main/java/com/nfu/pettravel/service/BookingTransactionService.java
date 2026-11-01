package com.nfu.pettravel.service;

public interface BookingTransactionService {
    long createBooking(long orderId, long userId, long routeId, long petId);
}
