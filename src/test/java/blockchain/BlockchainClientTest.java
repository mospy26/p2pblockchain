// package blockchain;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNull;
// import static org.junit.jupiter.api.Assertions.assertTrue;

// import org.junit.jupiter.api.Test;

// public class BlockchainClientTest extends Setup {
    

//     // Testing each method individually

//     @Test
//     public void testBasicrmCommandCorrectIndex() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));

//         boolean result = client.rmCommandHandler(pl, "rm|0");

//         assertTrue(result, "Basic rm command is not working correctly");
//         assertNull(pl.getServerInfos().get(0), "Server Info List is not being updated");
//     }

//     @Test
//     public void testBasicrmCommandCorrectIndex2() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));

//         boolean result = client.rmCommandHandler(pl, "rm|1");

//         assertTrue(result, "Basic rm command is not working correctly");
//     }

//     @Test
//     public void testIncorrectrmCommandNegativeIndex() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));

//         boolean result = client.rmCommandHandler(pl, "rm|-1");

//         assertTrue(!result, "Incorrent index to rm command is working!");
//     }

//     @Test
//     public void testIncorrectrmCommandInvalidIndex() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));

//         boolean result = client.rmCommandHandler(pl, "rm|incorrectstuff");

//         assertTrue(!result, "Invalid arguments to rm command is working!");
//     }

//     @Test
//     public void testBasicupCommandValidParams() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));

//         boolean result = client.upCommandHandler(pl, "up|0|localhost|3333");

//         assertTrue(result, "Basic up command is not working correctly");
//     }

//     @Test
//     public void testBasicupCommandValidParams2() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));

//         boolean result = client.upCommandHandler(pl, "up|1|0.0.0.0|3333");

//         assertTrue(result, "Basic up command is not working correctly");
//     }

//     @Test
//     public void testIncorrectupCommandNegativeIndex() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));

//         boolean result = client.upCommandHandler(pl, "up|-1|localhost|1235");

//         assertTrue(!result, "Incorrent index to up command is working!");
//     }

//     @Test
//     public void testIncorrectupCommandInvalidIndex() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));

//         boolean result = client.upCommandHandler(pl, "up|incorrectstuff|localhost|1111");

//         assertTrue(!result, "Invalid arguments to up command is working!");
//     }

//     @Test
//     public void testIncorrectupCommandInvalidHostname() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));

//         boolean result = client.upCommandHandler(pl, "up|0|localhost=0|1111");

//         assertTrue(!result, "Invalid hostname to up command is working!");
//     }

//     @Test
//     public void testIncorrectupCommandIllegalPort() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));

//         boolean result = client.upCommandHandler(pl, "up|0|localhost|0001");

//         assertTrue(!result, "Illegal port to up command is working!");
//     }

//     @Test
//     public void testIncorrectupCommandInvalidPort() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));

//         boolean result = client.upCommandHandler(pl, "up|0|localhost|invalidport");

//         assertTrue(!result, "Invalid port to up command is working!");
//     }

//     @Test
//     public void testIncorrectupCommandIncompleteCommand() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));

//         boolean result = client.upCommandHandler(pl, "up|0|helloworld|");

//         assertTrue(!result, "Incomplete up command is working!");
//     }

//     @Test
//     public void testBasicclCommand() {
//         ServerInfoList pl = new ServerInfoList();
//         pl.addServerInfo(new ServerInfo("localhost", 1234));
//         pl.addServerInfo(new ServerInfo("localhost", 5000));
//         pl.removeServerInfo(0);

//         client.clCommandHandler(pl);

//         assertEquals(1, pl.size(), "cl command not clearing nulls properly");
//     }
// }