package com.iotera.cba9handler.util;

import com.iotera.cba9handler.MainActivity;

public class SerialUtil extends MainActivity {
    public static String calculateCRC16(final byte[] bytes) {
        int CRC = 0xFFFF;
        int POLYNOMIAL = 0x18005;
        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((CRC >> 15 & 1) == 1);
                CRC <<= 1;
                if (c15 ^ bit) {
                    CRC ^= POLYNOMIAL;
                }
            }
        }
        CRC &= 0xffff;
        String crcHexString = Integer.toHexString(CRC);
        if (crcHexString.length() == 3) {
            crcHexString = "0" + crcHexString;
        }
        String reversedCrcHexString = crcHexString.substring(2, 4) + crcHexString.substring(0, 2);
        return reversedCrcHexString;
    }


}
