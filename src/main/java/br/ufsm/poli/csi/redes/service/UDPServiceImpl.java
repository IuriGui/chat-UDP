package br.ufsm.poli.csi.redes.service;

import br.ufsm.poli.csi.redes.model.Mensagem;
import br.ufsm.poli.csi.redes.model.TipoMensagem;
import br.ufsm.poli.csi.redes.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.*;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UDPServiceImpl implements UDPService {

    private Usuario usuario;
    private DatagramSocket socket;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Set<UDPServiceUsuarioListener> usuarioListeners = ConcurrentHashMap.newKeySet();
    private final Set<UDPServiceMensagemListener> mensagemListeners = ConcurrentHashMap.newKeySet();


    private final ConcurrentHashMap<InetAddress, Usuario> listaUsuarios = new ConcurrentHashMap<>();

    private class Listener implements Runnable {

        private final DatagramSocket socket;

        public Listener(DatagramSocket socket) {
            this.socket = socket;
        }

        @Override
        @SneakyThrows
        public void run() {
            byte[] buffer = new byte[2048];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                this.socket.receive(packet);
                InetAddress remetente = packet.getAddress();
                String strMensagem = new String(packet.getData(), 0, packet.getLength());
                Mensagem msg = mapper.readValue(strMensagem, Mensagem.class);

                if (isMyAddress(remetente)) {
                    continue;
                }
                //System.out.println("[UDPListener] Recebido de " + remetente.getHostAddress() + ":" + porta + " -> " + mensagem);

                processaPacotes(msg, remetente);


            }
        }
    }

    private class Sonda implements Runnable {


        public Sonda(DatagramSocket socket) {
        }
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


                sendToEveryone(bMensagem);

            }
        }
    }

    @SneakyThrows
    private void sendToEveryone(byte[] bMensagem) {
        for (NetworkInterface ni : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) continue;

            String nome = ni.getDisplayName().toLowerCase();
            if (nome.contains("virtual") || nome.contains("vmware") || nome.contains("hyper-v") ||
                    nome.contains("loopback") || nome.contains("miniport") || nome.contains("zerotier")) {
                continue;
            }

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

    public UDPServiceImpl() throws SocketException {
        this.iniciarThreads();
    }

    private boolean isMyAddress(InetAddress addr) {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress localAddr = ia.getAddress();
                    if (localAddr != null && localAddr.equals(addr)) {
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public void iniciarThreads() throws SocketException {

        this.socket = new DatagramSocket(8080);
        socket.setBroadcast(true);


        Thread threadEnviaSonda = new Thread(new Sonda(socket));
        threadEnviaSonda.setDaemon(true);
        threadEnviaSonda.start();

        Thread threadListener = new Thread(new Listener(socket));
        threadListener.setDaemon(true);
        threadListener.start();

        Thread threadUsuario = new Thread(new RemoveInativo());
        threadUsuario.setDaemon(true);
        threadUsuario.start();

    }


    private void processaPacotes(Mensagem msg, InetAddress remetente) throws IOException {
        switch (msg.getTipoMensagem()) {
            case sonda -> {

                Usuario u = new Usuario(msg.getUsuario(), Usuario.StatusUsuario.valueOf(msg.getStatus()), remetente, new Timestamp(System.currentTimeMillis()));

                listaUsuarios.put(remetente, u);

                for (UDPServiceUsuarioListener listener : usuarioListeners) {
                    listener.usuarioAdicionado(u);
                }

            }
            case msg_grupo -> {
                Usuario u = new Usuario();
                u.setNome(msg.getUsuario());
                u.setStatus(Usuario.StatusUsuario.valueOf(msg.getStatus()));
                for (UDPServiceMensagemListener listener : mensagemListeners) {
                    listener.mensagemRecebida(msg.getMsg(), u, true);
                }
            }
            case msg_individual -> {
                String nomeRemetente =  msg.getUsuario();
                Usuario u = null;
                for (Usuario user : listaUsuarios.values()) {
                    if (user.getNome().equals(nomeRemetente)) {
                        u = user;
                        break;
                    }
                }

                if (u == null) {
                    u = new Usuario();
                    u.setNome(nomeRemetente);
                    u.setStatus(Usuario.StatusUsuario.valueOf(msg.getStatus()));
                    u.setEndereco(remetente);
                } else {
                    // atualizar IP e status caso tenham mudado
                    u.setEndereco(remetente);
                    u.setStatus(Usuario.StatusUsuario.valueOf(msg.getStatus()));
                }

                for (UDPServiceMensagemListener listener : mensagemListeners){
                    listener.mensagemRecebida(msg.getMsg(), u, false);
                }
            }
            case fim_chat -> {

                String nomeRemetente =  msg.getUsuario();
                Usuario u = null;
                for (Usuario user : listaUsuarios.values()) {
                    if (user.getNome().equals(nomeRemetente)) {
                        u = user;
                        break;
                    }
                }
                for (UDPServiceMensagemListener listener : mensagemListeners){
                    listener.fimChatPelaOutraParte(u);
                }
            }

        }

    }


    private class RemoveInativo implements Runnable {

        private boolean inativo(Usuario usuario) {
            long agora = System.currentTimeMillis();
            long lastSonda = usuario.getLastSonda().getTime();
            //System.out.println("Tempo que " + usuario.getNome() + " está inativo: " + ((agora - lastSonda) / 1000));
            return agora - lastSonda > 30_000;

        }

        @Override
        public void run() {
            while (true) {
                for (Map.Entry<InetAddress, Usuario> entry : listaUsuarios.entrySet()) {
                    Usuario u = entry.getValue();
                    if (inativo(u)) {
                        for (UDPServiceUsuarioListener listener : usuarioListeners) {
                            listener.usuarioRemovido(u);
                        }

                        listaUsuarios.remove(entry.getKey());
                    }
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public void enviarMensagem(String mensagem, Usuario destinatario, boolean chatGeral) {

        Mensagem mensagemObj = new Mensagem();
        mensagemObj.setUsuario(usuario.getNome());
        mensagemObj.setStatus(usuario.getStatus().toString());
        mensagemObj.setTipoMensagem(chatGeral ? TipoMensagem.msg_grupo : TipoMensagem.msg_individual);
        mensagemObj.setMsg(mensagem);

        try {
            String strMensagem = mapper.writeValueAsString(mensagemObj);
            byte[] bMensagem = strMensagem.getBytes();
            if (chatGeral) {
                sendToEveryone(bMensagem);
            } else{
                socket.setBroadcast(false);
                System.out.println("[Mensagem] " + strMensagem);
                socket.send(new DatagramPacket(bMensagem, bMensagem.length, destinatario.getEndereco(), 8080));

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void usuarioAlterado(Usuario usuario) {
        //System.out.println("Endereco no momento que add user: " + usuario.getEndereco());
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

    @Override
    public void fimChat(Usuario usuario) {
        System.out.println("Fim chat");

        try {
            Mensagem mensagemObj = new Mensagem();
            mensagemObj.setUsuario(this.usuario.getNome());
            mensagemObj.setStatus(usuario.getStatus().toString());
            mensagemObj.setTipoMensagem(TipoMensagem.fim_chat);
            String strMensagem = mapper.writeValueAsString(mensagemObj);
            byte[] bMensagem = strMensagem.getBytes();


            socket.send(new DatagramPacket(bMensagem, bMensagem.length, usuario.getEndereco(), 8080));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
