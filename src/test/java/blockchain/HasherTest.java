package blockchain;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;

class HasherTest {
    @Test
    void basicHashTest() {
        assertEquals(Hasher.hashMessages(new ArrayList<String>(Arrays.asList("12345", "12345"))),
                "7ccQDEgOM+OWvega620WYCXJHs1w53wSumP329xFirw=", "Hashing is not correct");
    }
}
