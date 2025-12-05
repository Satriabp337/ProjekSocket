package common;

//import java.io.Serializable;

public enum MessageType {
    
    // 1. Tipe Koneksi & Status
    CONNECT,            // Klien meminta koneksi ke server
    DISCONNECT,         // Klien meminta putus koneksi
    USER_LIST_UPDATE,   // Server mengirimkan daftar pengguna yang terkoneksi
    

    // 2. Tipe Chat & Aksi
    BROADCAST_CHAT,     // Pesan teks publik
    PRIVATE_CHAT,       // Pesan teks pribadi
    BUZZ,               // Fitur Window Shake

    // 3. Tipe File Transfer (Yang kita gunakan di ClientService)
    FILE_REQUEST,       // Klien meminta/menawarkan transfer file (Header)
    FILE_CHUNK,         // Data biner aktual (potongan file)
    FILE_COMPLETE,      // Sinyal bahwa pengiriman/penerimaan potongan file selesai
    FILE_REJECT         // Penolakan transfer file (oleh penerima)
}