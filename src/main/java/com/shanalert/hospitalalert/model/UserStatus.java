package com.shanalert.hospitalalert.model;

public enum UserStatus {
    NEW,
    ACTIVE, // Normal user, fully enabled
    INACTIVE, // Created but not yet activated
    PENDING_VERIFICATION, // Waiting for email/SMS verification
    SUSPENDED, // Temporarily disabled by admin
    BANNED, // Permanently blocked
    LOCKED, // Locked due to failed login attempts
    DELETED // Soft-deleted account
}
