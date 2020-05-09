package blockchain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class BlockchainServerTest extends Setup {

    @AfterEach
    public void closeStreams() {
        closeServerInput();
        closeServerOutput();
        fromServer = new ByteArrayOutputStream();
    }

    @Test
    public void testValidTransaction() {

        BlockchainServerRunnable serverRunnable = new BlockchainServerRunnable(null, new Blockchain());
        byte[] data = "tx|test0000|helloworld".getBytes();
        resetServerInput(data);
        serverRunnable.serverHandler(toServer, fromServer);

        byte[] output = fromServer.toByteArray();

        assertTrue("Accepted\n\n".equals(new String(output)), "Correct transaction does not work with serverHandler");
    }

    @Test
    public void testInvalidTransactionSender() {

        BlockchainServerRunnable serverRunnable = new BlockchainServerRunnable(null, new Blockchain());
        byte[] data = "tx|tes|helloworld".getBytes();
        resetServerInput(data);
        serverRunnable.serverHandler(toServer, fromServer);

        byte[] output = fromServer.toByteArray();
        assertTrue("Rejected\n\n".equals(new String(output)), "Incorrect transaction sender is working with serverHandler");
    }

    @Test
    public void testInvalidTransactionContent() {

        BlockchainServerRunnable serverRunnable = new BlockchainServerRunnable(null, new Blockchain());
        byte[] data = ("tx|test0000|thisisveryveryinvalid..........." + 
                        "thisisveryveryinvalid..........." + 
                        "thisisveryveryinvalid..........." + 
                        "thisisveryveryinvalid..........." + 
                        "thisisveryveryinvalid..........." + 
                        "thisisveryveryinvalid..........." + 
                        "thisisveryveryinvalid..........." + 
                        "thisisveryveryinvalid...........").getBytes();
        resetServerInput(data);
        serverRunnable.serverHandler(toServer, fromServer);

        byte[] output = fromServer.toByteArray();
        assertTrue("Rejected\n\n".equals(new String(output)), "Transaction with content > 70 chars is working with serverHandler");
    }

    @Test
    public void testInvalidTransactionFormat() {

        BlockchainServerRunnable serverRunnable = new BlockchainServerRunnable(null, new Blockchain());
        byte[] data = "txtxtxtx".getBytes();
        resetServerInput(data);
        serverRunnable.serverHandler(toServer, fromServer);

        byte[] output = fromServer.toByteArray();
        assertTrue("Rejected\n\n".equals(new String(output)), "Incorrect transaction is working with serverHandler");
    }

    @Test
    public void testInvalidTransactionFormat2() {

        BlockchainServerRunnable serverRunnable = new BlockchainServerRunnable(null, new Blockchain());
        byte[] data = "tx|est0000helloworld".getBytes();
        resetServerInput(data);
        serverRunnable.serverHandler(toServer, fromServer);

        byte[] output = fromServer.toByteArray();
        assertTrue("Rejected\n\n".equals(new String(output)), "Incorrectly formatted transaction is working with serverHandler");
    }

    @Test
    public void testInvalidString1() {

        BlockchainServerRunnable serverRunnable = new BlockchainServerRunnable(null, new Blockchain());
        byte[] data = ("invalidstringyay").getBytes();
        resetServerInput(data);
        serverRunnable.serverHandler(toServer, fromServer);

        byte[] output = fromServer.toByteArray();
        assertTrue("Error\n\n".equals(new String(output)), "Incorrect transaction is working with serverHandler");
    }

    @Test
    public void testInvalidString2() {

        BlockchainServerRunnable serverRunnable = new BlockchainServerRunnable(null, new Blockchain());
        byte[] data = ("1234").getBytes();
        resetServerInput(data);
        serverRunnable.serverHandler(toServer, fromServer);

        byte[] output = fromServer.toByteArray();
        assertTrue("Error\n\n".equals(new String(output)), "Incorrect transaction is working with serverHandler");
    }

    @Test
    public void testInvalidString3() {

        BlockchainServerRunnable serverRunnable = new BlockchainServerRunnable(null, new Blockchain());
        byte[] data = ("-----").getBytes();
        resetServerInput(data);
        serverRunnable.serverHandler(toServer, fromServer);

        byte[] output = fromServer.toByteArray();
        assertTrue("Error\n\n".equals(new String(output)), "Incorrect transaction is working with serverHandler");
    }

    @Test
    public void testEmptyString() {

        BlockchainServerRunnable serverRunnable = new BlockchainServerRunnable(null, new Blockchain());
        byte[] data = ("\n").getBytes();
        resetServerInput(data);
        serverRunnable.serverHandler(toServer, fromServer);

        byte[] output = fromServer.toByteArray();
        assertTrue("Error\n\n".equals(new String(output)), "Empty string to server does not produce an error");
    }

    @Test
    public void testpbWithOneTransaction() {

        String transaction = "tx|test0000|helloworld";
        String pb = "pb\n";

        Blockchain blockchain = new Blockchain();
        blockchain.addTransaction(new String(transaction));
        BlockchainServerRunnable serverRunnable = new BlockchainServerRunnable(null, new Blockchain());

        byte[] data = (transaction + "\n" + pb).getBytes();
        resetServerInput(data);
        serverRunnable.serverHandler(toServer, fromServer);

        byte[] output = fromServer.toByteArray();
        System.out.println("Accepted\n\n" + blockchain.toString());
        System.out.println(new String(output));
        assertTrue(("Accepted\n\n" + blockchain.toString()).trim().equals(new String(output).trim()), "pb command to server is not producing the correct blockchain");
    }


    @Test
    public void testpbWiththreeTransaction() {

        String transaction = "tx|test0000|helloworld0\ntx|test0001|helloworld1\ntx|test0002|helloworld2";
        String pb = "pb\n";

        Blockchain blockchain = new Blockchain();
        
        for(String tx : transaction.split("\n")) {
            blockchain.addTransaction(new String(tx));
        }

        BlockchainServerRunnable serverRunnable = new BlockchainServerRunnable(null, new Blockchain());

        byte[] data = (transaction + "\n" + pb).getBytes();
        resetServerInput(data);
        serverRunnable.serverHandler(toServer, fromServer);

        byte[] output = fromServer.toByteArray();
        System.out.println("Accepted\n\n" + blockchain.toString());
        System.out.println(new String(output));
        assertTrue(("Accepted\n\nAccepted\n\nAccepted\n\n" + blockchain.toString()).trim().equals(new String(output).trim()), "pb command to server is not producing the correct blockchain");
    }

    // Helper methods

    public void resetServerInput(byte[] data) {
        toServer = null;
        toServer = new ByteArrayInputStream(data);
    }

    public void closeServerInput() {
        try {
            toServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeServerOutput() {
        try {
            fromServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}