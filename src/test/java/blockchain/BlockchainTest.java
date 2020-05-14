// package blockchain;

// import org.junit.jupiter.api.Test;
// import static org.hamcrest.MatcherAssert.assertThat;
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import static org.hamcrest.CoreMatchers.*;
// import org.cactoos.matchers.RunsInThreads;
// import org.hamcrest.MatcherAssert;
// import java.util.ArrayList;
// import java.util.HashSet;
// import java.util.concurrent.atomic.AtomicInteger;
// import java.util.stream.Stream;

// // Thread safe tests
// public class BlockchainTest extends Setup {

//     static Blockchain blockchain = new Blockchain();

//     @Test
//     void testBlockchainAddTransactionSynchronized() {

//         blockchain = new Blockchain();
//         int threads = 100;

//         assertThat(
//             b -> {
//                 int a = b.getAndIncrement();
//                 String index = String.format("%03d", a);
//                 String transaction = "tx|test0" + index + "|helloworldfrom" + index;
//                 blockchain.addTransaction(transaction);
//                 return true;
//             },
//                 new RunsInThreads<>(new AtomicInteger(), threads)
//             );
        
//         assertTrue(blockchain.getPool().size() == threads, "Not synchronized");

//         // Check for same senders
//         ArrayList<String> expectedSenders = new ArrayList<>();
//         ArrayList<String> actualSenders = new ArrayList<>();

//         for (int i = 0; i < threads; i++) {
//             expectedSenders.add("test0" + String.format("%03d", i));
//         }

//         for (Transaction tx : blockchain.getPool()) {
//             actualSenders.add(tx.getSender());
//         }

//         HashSet<String> set = new HashSet<>(actualSenders);
//         assertTrue(set.size() == expectedSenders.size());
//     }

//     @Test
//     void testBlockchainCommitSynchronized() {
        
//         blockchain = new Blockchain();
//         int threads = 100;

//         assertThat(
//             b -> {
//                 int a = b.getAndIncrement();
//                 String index = String.format("%03d", a);
//                 String transaction = "tx|test0" + index + "|helloworldfrom" + index;
//                 System.out.println(transaction);
//                 blockchain.addTransaction(transaction);
//                 blockchain.commit(a);
//                 return true;
//             },
//                 new RunsInThreads<>(new AtomicInteger(), 100)
//             );

//         // Check for same senders
//         ArrayList<String> expectedSenders = new ArrayList<>();

//         for (int i = 0; i < threads; i++) {
//             expectedSenders.add("test0" + String.format("%03d", i));
//         }

//         // Get all senders across all committed blocks also
//         ArrayList<String> actualSenders = new ArrayList<>();

//         for (Transaction tx : blockchain.getPool()) {
//             actualSenders.add(tx.getSender());
//         }
        
//         Block head = blockchain.getHead();
        
//         while (head != null) {
//             for (Transaction tx : head.getTransactions()) {
//                 actualSenders.add(tx.getSender());
//             }
//             head = head.getPreviousBlock();
//         }

//         HashSet<String> set = new HashSet<>(actualSenders);
//         assertTrue(set.size() == expectedSenders.size());
//     }
// }