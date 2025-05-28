package com.example.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class ServerLauncher {
    public static void main(String[] args) {
        try {
            // Get and display all IP addresses
            System.out.println("Available network interfaces:");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr.getHostAddress().contains(".")) { // Only show IPv4 addresses
                            System.out.println("Interface: " + ni.getDisplayName());
                            System.out.println("IP Address: " + addr.getHostAddress());
                        }
                    }
                }
            }

            GameServer server = new GameServer();
            server.start();
            System.out.println("\nServer is running. Press Ctrl+C to stop.");
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 