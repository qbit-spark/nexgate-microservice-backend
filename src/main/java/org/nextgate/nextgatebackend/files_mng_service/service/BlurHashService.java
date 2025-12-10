package org.nextgate.nextgatebackend.files_mng_service.service;

public interface BlurHashService {

    String generateBlurHash(byte[] imageBytes);
}