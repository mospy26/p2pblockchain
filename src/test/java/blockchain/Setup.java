// package blockchain;

// import org.junit.jupiter.api.BeforeAll;

// import java.io.ByteArrayInputStream;
// import java.io.ByteArrayOutputStream;
// import java.io.PrintStream;
// import java.util.ArrayList;

// public class Setup {
//     static BlockchainClient client;
//     static BlockchainServer server;
//     static BlockchainServerRunnable serverRunnable;
//     static Block block;
//     static ServerInfoList serverInfoList;
//     static ArrayList<Transaction> validTransactions;
//     static ArrayList<Transaction> invalidTransactions;
//     static Blockchain blockchain;
//     static ByteArrayInputStream toServer;
//     static ByteArrayOutputStream fromServer;

//     @BeforeAll
//     public static void beforeAll() {
//         client = new BlockchainClient();
//         server = new BlockchainServer();
//         setupValidTransactions();
//         setupInvalidTransactions();
//         setupBlock();
//         setupBlockchain();
//         setupServerInfoList();
//         serverRunnable = new BlockchainServerRunnable(null, blockchain);
//         fromServer = new ByteArrayOutputStream();
//     }

//     public static void setupValidTransactions() {
//         validTransactions = new ArrayList<>();
//         for (int i = 0; validTransactions.size() <= 3; i++) {
//             Transaction transaction = new Transaction();
//             transaction.setSender("test000" + i);
//             transaction.setContent("Some random content");
//             validTransactions.add(transaction);
//         }
//     }

//     public static void setupInvalidTransactions() {
//         invalidTransactions = new ArrayList<>();
//         for (int i = 0; invalidTransactions.size() <= 5; i++) {
//             Transaction transaction = new Transaction();
//             if (i % 2 == 0)
//                 transaction.setSender("thisisaninvalidsender");
//             else
//                 transaction.setSender(null);
//             if (i % 2 != 0)
//                 transaction.setContent("This is valid content");
//             else
//                 transaction.setContent(null);
//             invalidTransactions.add(transaction);
//         }
//     }

//     public static void setupBlock() {
//         block = new Block();
//         block.setPreviousBlock(null);
//         block.setPreviousHash(new byte[32]);

//         for (Transaction tx : validTransactions) {
//             block.addTransaction(tx);
//         }
//     }

//     public static void setupBlockchain() {
//         blockchain = new Blockchain();
//     }

//     public static void setupServerInfoList() {
//         serverInfoList = new ServerInfoList();
//         serverInfoList.addServerInfo(new ServerInfo("localhost", 5000));
//         serverInfoList.addServerInfo(new ServerInfo("localhost", 1234));
//     }
// }
