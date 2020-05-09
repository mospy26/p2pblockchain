package blockchain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;

class BlockTest extends Setup {

    @Test
    void testBasicBlockHash() {
        byte[] hash = block.calculateHash();
        String actualHash = Base64.getEncoder().encodeToString(hash);
        String expectedHash = "aJv2wil5gHvaAbAnKxfjt3oposL68/WTkWcNoCKR8io=";
        assertEquals(expectedHash, actualHash, "Hashes are not equal");
    }

    @Test
    void testBasicBlockHashWithNonceTest() {
        byte[] hash = block.calculateHashWithNonce(33);
        String actualHash = Base64.getEncoder().encodeToString(hash);
        String expectedHash = "VHtMCgz1oy7d4MBWasGuERxHX94tR90Bjg47DMS5KDE=";
        assertEquals(expectedHash, actualHash, "Hashes are not equal");
    }
}
