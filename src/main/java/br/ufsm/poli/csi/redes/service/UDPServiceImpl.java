package br.ufsm.poli.csi.redes.service;

import br.ufsm.poli.csi.redes.model.Mensagem;
import br.ufsm.poli.csi.redes.model.TipoMensagem;
import br.ufsm.poli.csi.redes.model.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UDPServiceImpl implements UDPService {

    private Usuario usuario;
    private DatagramSocket socket;
    private Thread threadEnviaSonda;
    private Thread threadListener;

    private final Set<UDPServiceUsuarioListener> usuarioListeners = ConcurrentHashMap.newKeySet();
    private final Set<UDPServiceMensagemListener> mensagemListeners = ConcurrentHashMap.newKeySet();


    private final ConcurrentHashMap<InetAddress, Usuario> listaUsuarios = new ConcurrentHashMap<>();

    private class Listener implements Runnable {

        private final DatagramSocket socket;

        public Listener(DatagramSocket socket) { this.socket = socket; }

        @Override
        @SneakyThrows
        public void run() {
            byte[] buffer = new byte[2048];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                this.socket.receive(packet);

                String mensagem = new String(packet.getData(), 0, packet.getLength());
                InetAddress remetente = packet.getAddress();
                int porta = packet.getPort();

                System.out.println("[UDPListener] Recebido de " + remetente.getHostAddress() + ":" + porta + " -> " + mensagem);

                processaPacotes(packet);


            }
        }
    }

    private class Sonda implements Runnable {

        private final DatagramSocket socket;
        private final ObjectMapper mapper = new ObjectMapper();

        public Sonda(DatagramSocket socket) { this.socket = socket; }


        @Override
        @SneakyThrows
        public void run() {
            while (true) {
                Thread.sleep(5000);
                if (usuario == null) {
                    System.out.println("Usuario null");
                    continue;
                }
                Mensagem mensagem = new Mensagem();
                mensagem.setTipoMensagem(TipoMensagem.sonda);
                mensagem.setUsuario(usuario.getNome());
                mensagem.setStatus(usuario.getStatus().toString());
                String strMensagem = mapper.writeValueAsString(mensagem);
                byte[] bMensagem = strMensagem.getBytes();


                for (NetworkInterface ni : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    if (ni.isLoopback() || !ni.isUp()) continue;

                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        InetAddress broadcast = ia.getBroadcast();

                        if (broadcast == null) continue;

                        //System.out.println("[UDPService] Enviando broadcast para: " + broadcast.getHostAddress());

                        socket.setBroadcast(true);
                        socket.send(new DatagramPacket(bMensagem, bMensagem.length, broadcast, 8080));

                        //System.out.println("[UDPService] Mensagem enviada com sucesso para " + broadcast.getHostAddress());
                    }
                }
            }
        }
    }

    public UDPServiceImpl() throws SocketException {
        this.iniciarThreads();
    }

    @Override
    public void iniciarThreads() throws SocketException {

        this.socket = new DatagramSocket(8080);
        socket.setBroadcast(true);


        threadEnviaSonda = new Thread(new Sonda(socket));
        threadEnviaSonda.setDaemon(true);
        threadEnviaSonda.start();

        threadListener = new Thread(new Listener(socket));
        threadListener.setDaemon(true);
        threadListener.start();

    }


    private void processaPacotes(DatagramPacket packet) throws IOException {
        String mensagem = new String(packet.getData(), 0, packet.getLength());
        ObjectMapper mapper = new ObjectMapper();
        Mensagem msg = mapper.readValue(mensagem, Mensagem.class);

        InetAddress remetente = packet.getAddress();

       switch (msg.getTipoMensagem()){
           case sonda -> {
               System.out.println("📡 Recebida sonda de " + msg.getUsuario());

               Usuario u = new Usuario();
               u.setNome(msg.getUsuario());
               u.setStatus(Usuario.StatusUsuario.valueOf(msg.getStatus()));
               listaUsuarios.put(remetente, u);

               for (UDPServiceUsuarioListener listener : usuarioListeners) {
                   listener.usuarioAdicionado(u);
               }

               //System.out.println("Usuário registrado/atualizado: " + msg.getUsuario());
               //imprimirUsuarios();
           }
           case msg_grupo -> {
               Usuario u = new Usuario();
               u.setNome(msg.getUsuario());
               u.setStatus(Usuario.StatusUsuario.valueOf(msg.getStatus()));
               for(UDPServiceMensagemListener listener : mensagemListeners){
                   listener.mensagemRecebida(msg.getMsg(), u, true);
               }
           }
           case msg_individual -> {
               System.out.println("msg individual");
           }
           case fim_chat -> {
               System.out.println("fim chat");
           }

       }





    }

    private void imprimirUsuarios() {
        System.out.println("👥 Usuários ativos:");
        listaUsuarios.forEach((ip, usuario) -> {
            System.out.println(" - " + usuario.getNome() + " (" + ip.getHostAddress() + ") " + usuario.getStatus());
        });
    }


    @Override
    public void enviarMensagem(String mensagem, Usuario destinatario, boolean chatGeral) throws JsonProcessingException {
            DatagramPacket packet;
            ObjectMapper mapper = new ObjectMapper();

            Mensagem mensagemGrupo = new Mensagem();
            mensagemGrupo.setUsuario(destinatario.getNome());
            mensagemGrupo.setStatus(usuario.getStatus().toString());
            mensagemGrupo.setTipoMensagem(TipoMensagem.msg_grupo);
            mensagemGrupo.setMsg(mensagem);

            try {
                String strMensagem = mapper.writeValueAsString(mensagemGrupo);
                byte[] bMensagem = strMensagem.getBytes();
                InetAddress broadcast = InetAddress.getByName("255.255.255.255");
                socket.setBroadcast(true);

                packet = new DatagramPacket(bMensagem, bMensagem.length, broadcast, 8080);
                socket.send(packet);

            } catch (Exception e){
                e.printStackTrace();
            }



    }

    @Override
    public void usuarioAlterado(Usuario usuario) {
        this.usuario = usuario;
    }


    @Override
    public void addListenerUsuario(UDPServiceUsuarioListener listener) {
        usuarioListeners.add(listener);
    }

    @Override
    public void addListenerMensagem(UDPServiceMensagemListener listener) {
        mensagemListeners.add(listener);
    }

    public void fimChat(Usuario usuario) {

    }

}
