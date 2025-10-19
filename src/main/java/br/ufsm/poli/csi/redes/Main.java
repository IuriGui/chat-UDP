package br.ufsm.poli.csi.redes;

import br.ufsm.poli.csi.redes.service.UDPService;
import br.ufsm.poli.csi.redes.service.UDPServiceImpl;
import br.ufsm.poli.csi.redes.swing.ChatClientSwing;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws UnknownHostException, SocketException {
        new ChatClientSwing();

//        for (NetworkInterface ni : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
//            System.out.println("\nInterface: " + ni.getDisplayName());
//            for (InterfaceAddress ia : ni
//            .getInterfaceAddresses()) {
//                System.out.println("  IP: " + ia.getAddress().getHostAddress());
//                System.out.println("  Broadcast: " + ia.getBroadcast());
//            }
//        }
    }


}