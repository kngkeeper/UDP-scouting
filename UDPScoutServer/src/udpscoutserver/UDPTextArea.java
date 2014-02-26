/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package udpscoutserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JTextArea;

/**
 *
 * @author Alex
 */
public class UDPTextArea extends JTextArea {

    private ExecutorService es;
    private Listener ls;
    private TeamServer ts;

    public void startServer() {
        es = Executors.newCachedThreadPool();
        ls = new Listener();
        ts = new TeamServer();
        es.execute(ls);
        this.append("Started UDP listening\n");
        es.execute(ts);
        this.append("Started UDP teamserving\n");
    }
    
    public void stopServer() {
        ls.complete();
        ts.complete();
        es.shutdown();
        es = null;
    }

    private class Listener implements Runnable {
        
        private boolean done = false;

        @Override
        public void run() {
            try {
                //BufferedWriter uff = new BufferedWriter(new FileWriter(new File("data.csv"),true));
                while (!done) {
                    String text;
                    int server_port = 3710;
                    byte[] message = new byte[1500];
                    DatagramPacket p = new DatagramPacket(message, message.length);
                    DatagramSocket s;

                    s = new DatagramSocket(server_port);
                    s.receive(p);

                    text = new String(message, 0, p.getLength());
                    BufferedWriter uff = new BufferedWriter(new FileWriter(new File("data.csv"),true));
                    uff.write(text);
                    uff.newLine();
                    uff.close();
                    uff = null;
                    append("Recived data from: " + p.getAddress().toString() + "\n");
                    s.close();
                }
                //uff.close();
            } catch (Exception ex) {
                append("Error in listener\n");
            }
        }
        
        public void complete () {
            done = true;
        }
    }

    private class TeamServer implements Runnable {
        
        private boolean done = false;

        @Override
        public void run() {
            LinkedList<String> teams = new LinkedList<>();
            int it = 0;
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File(System.getProperty("user.dir") + "/teams.txt")));
                String read = br.readLine();
                while (read != null) {
                    teams.add(read);
                    read = br.readLine();
                }

            } catch (Exception exe) {
                append("Error reading teamfile\n");
            }

            try {
                LinkedList<String> used = new LinkedList<>();
                while (it < teams.size() && !done) {
                    String text;
                    int rserver_port = 3711;
                    byte[] rmessage = new byte[1500];
                    DatagramPacket rp = new DatagramPacket(rmessage, rmessage.length);
                    DatagramSocket rs;

                    rs = new DatagramSocket(rserver_port);
                    rs.receive(rp);

                    text = new String(rmessage, 0, rp.getLength());

                    if ("gogo".equals(text)) {
                        used.add(teams.peek());
                        String msg = teams.pop();
                        int server_port = 3712;
                        DatagramSocket s = new DatagramSocket();
                        InetAddress local = rp.getAddress();
                        int msg_length = msg.length();
                        byte[] message = msg.getBytes();
                        DatagramPacket p = new DatagramPacket(message, msg_length, local, server_port);
                        s.send(p);
                        s.close();
                        it++;
                        append("Gave team " + msg + " to " + rp.getAddress().toString() + "\n");
                    }

                    rs.close();
                }
                BufferedWriter br = new BufferedWriter(new FileWriter(new File("used.txt"),true));
                for (String d:used) {
                    br.write(d);
                    br.newLine();
                }
                br.close();
                br = null;
                BufferedWriter b = new BufferedWriter(new FileWriter(new File("teams.txt")));
                for (String da:teams) {
                    b.write(da);
                    b.newLine();
                }
                b.close();
            } catch (Exception e) {
                append("Error in teamserving process\n");
            }
        }
        
        public void complete() {
            done = true;
        }
    }
}
