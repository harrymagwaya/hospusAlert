package com.shanalert.hospitalalert.security;

import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class OtpService {

    // Note: For production, consider moving this to application.properties
    private final String SYSTEM_SECRET = "HospusApp_Internal_Secret_Key_2026";

    public String generateStatelessOtp(String userSecret) {
        // We use a 15-minute time block (900,000 ms)
        long timeBlock = System.currentTimeMillis() / 900_000;
        return calculateHash(userSecret, timeBlock);
    }

    public boolean isOtpValid(String userSecret, String providedOtp) {
        long currentTimeBlock = System.currentTimeMillis() / 900_000;

        // We check the current block and the previous block
        // (to allow for a bit of lag/buffer time)
        return providedOtp.equals(calculateHash(userSecret, currentTimeBlock)) ||
                providedOtp.equals(calculateHash(userSecret, currentTimeBlock - 1));
    }

    private String calculateHash(String userSecret, long timeBlock) {
        try {
            String data = userSecret + SYSTEM_SECRET + timeBlock;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(SYSTEM_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String base64Hash = Base64.getEncoder().encodeToString(hash);

            // Extract only digits from the hash
            String rawDigits = base64Hash.replaceAll("\\D", "");

            // Fallback padding to prevent StringIndexOutOfBoundsException
            // If the hash somehow produces fewer than 6 digits, we pad it with zeros
            while (rawDigits.length() < 6) {
                rawDigits += "0";
            }

            // Safely return exactly the first 6 digits
            return rawDigits.substring(0, 6);

        } catch (Exception e) {
            throw new RuntimeException("Error generating OTP");
        }
    }
}