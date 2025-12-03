package common;

import java.io.Serializable;

public enum MessageType implements Serializable {
    // Tipe dasar untuk koneksi dan pesan
    LOGIN,
    LOGOUT,
    TEXT,
    FILE,
    BUZZ,

    // Tipe untuk transfer file
    FILE_TRANSFER_START,
    FILE_TRANSFER_DATA,
    FILE_TRANSFER_COMPLETE,
    FILE_TRANSFER_ERROR
}