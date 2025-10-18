package br.ufsm.poli.csi.redes.swing;

import br.ufsm.poli.csi.redes.model.Usuario;
import br.ufsm.poli.csi.redes.service.UDPService;
import br.ufsm.poli.csi.redes.service.UDPServiceImpl;
import br.ufsm.poli.csi.redes.service.UDPServiceMensagemListener;
import br.ufsm.poli.csi.redes.service.UDPServiceUsuarioListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * User: Rafael
 * Date: 13/10/14
 * Time: 10:28
 *
 */
public class ChatClientSwing extends JFrame {

    private Usuario meuUsuario;
    private JList listaChat;
    private DefaultListModel<Usuario> dfListModel;
    private JTabbedPane tabbedPane = new JTabbedPane();
    private Set<Usuario> chatsAbertos = new HashSet<>();
    private UDPService udpService = new UDPServiceImpl();
    private Usuario USER_GERAL = new Usuario("Geral", null, null);

    public ChatClientSwing() throws UnknownHostException, SocketException {
        setLayout(new GridBagLayout());
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Status");

        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(Usuario.StatusUsuario.DISPONIVEL.name());
        rbMenuItem.setSelected(true);
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(Usuario.StatusUsuario.DISPONIVEL);
                udpService.usuarioAlterado(meuUsuario);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(Usuario.StatusUsuario.NAO_PERTURBE.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(Usuario.StatusUsuario.NAO_PERTURBE);
                udpService.usuarioAlterado(meuUsuario);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(Usuario.StatusUsuario.VOLTO_LOGO.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(Usuario.StatusUsuario.VOLTO_LOGO);
                udpService.usuarioAlterado(meuUsuario);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        menuBar.add(menu);
        this.setJMenuBar(menuBar);

        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popupMenu =  new JPopupMenu();
                    final int tab = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());
                    JMenuItem item = new JMenuItem("Fechar");
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PainelChatPVT painel = (PainelChatPVT) tabbedPane.getComponentAt(tab);
                            tabbedPane.remove(tab);
                            chatsAbertos.remove(painel.getUsuario());
                            udpService.fimChat(painel.getUsuario());
                        }
                    });
                    popupMenu.add(item);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        add(new JScrollPane(criaLista()), new GridBagConstraints(0, 0, 1, 1, 0.1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        add(tabbedPane, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        setSize(800, 600);
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screenSize.width - this.getWidth()) / 2;
        final int y = (screenSize.height - this.getHeight()) / 2;
        this.setLocation(x, y);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Chat P2P - Redes de Computadores");
        String nomeUsuario = JOptionPane.showInputDialog(this, "Digite seu nome de usuario: ");
        this.meuUsuario = new Usuario(nomeUsuario, Usuario.StatusUsuario.DISPONIVEL, InetAddress.getLocalHost());
        udpService.usuarioAlterado(meuUsuario);
        udpService.addListenerMensagem(new MensagemListener());
        udpService.addListenerUsuario(new UsuarioListener());
        setVisible(true);
    }

    private JComponent criaLista() {
        dfListModel = new DefaultListModel();
        //dfListModel.addElement(new Usuario("Fulano", Usuario.StatusUsuario.NAO_PERTURBE, null));
        //dfListModel.addElement(new Usuario("Cicrano", Usuario.StatusUsuario.DISPONIVEL, null));
        listaChat = new JList(dfListModel);
        listaChat.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList) evt.getSource();
                if (evt.getClickCount() == 2) {
                    int index = list.locationToIndex(evt.getPoint());
                    Usuario user = (Usuario) list.getModel().getElementAt(index);
                    if (chatsAbertos.add(user)) {
                        tabbedPane.add(user.toString(), new PainelChatPVT(user, false));
                    }
                }
            }
        });
        chatsAbertos.add(USER_GERAL);
        tabbedPane.add("Geral", new PainelChatPVT(USER_GERAL, true));
        return listaChat;
    }


    @Getter
    class PainelChatPVT extends JPanel {

        JTextArea areaChat;
        JTextField campoEntrada;
        Usuario usuario;
        boolean chatGeral = false;

        PainelChatPVT(Usuario usuario, boolean chatGeral) {
            setLayout(new GridBagLayout());
            areaChat = new JTextArea();
            this.usuario = usuario;
            areaChat.setEditable(false);
            campoEntrada = new JTextField();
            this.chatGeral = chatGeral;
            campoEntrada.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((JTextField) e.getSource()).setText("");
                    areaChat.append(meuUsuario.getNome() + "> " + e.getActionCommand() + "\n");
                    try {
                        udpService.enviarMensagem(e.getActionCommand(), usuario, chatGeral);
                    } catch (JsonProcessingException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
            add(new JScrollPane(areaChat), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            add(campoEntrada, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        }


    }

    private class UsuarioListener implements UDPServiceUsuarioListener {

        @Override
        public void usuarioAdicionado(Usuario usuario) {
            dfListModel.removeElement(usuario);
            dfListModel.addElement(usuario);
        }

        @Override
        public void usuarioRemovido(Usuario usuario) {
            dfListModel.removeElement(usuario);
        }

        @Override
        public void usuarioAlterado(Usuario usuario) {
            dfListModel.removeElement(usuario);
            dfListModel.addElement(usuario);
        }
    }

    private class MensagemListener implements UDPServiceMensagemListener {

        @Override
        public void mensagemRecebida(String mensagem, Usuario remetente, boolean chatGeral) {
            PainelChatPVT painel = null;
            if (chatGeral) {
                painel = (PainelChatPVT) tabbedPane.getComponentAt(0);
            } else {
                for (int i = 1; i < tabbedPane.getTabCount(); i++) {
                    PainelChatPVT p = (PainelChatPVT) tabbedPane.getComponentAt(i);
                    if (p.getUsuario().equals(remetente)) {
                        painel = p;
                        break;
                    }
                }
            }
            if (painel != null) {
                painel.getAreaChat().append(remetente.getNome() + "> " + mensagem + "\n");
            } else {
                if (chatsAbertos.add(remetente)) {
                    PainelChatPVT painelChatPVT = new PainelChatPVT(remetente, false);
                    tabbedPane.add(remetente.toString(), painelChatPVT);
                    painelChatPVT.getAreaChat().append(remetente.getNome() + "> " + mensagem + "\n");
                }
            }
        }

        @Override
        public void fimChatPelaOutraParte(Usuario remetente) {
            PainelChatPVT painel = null;
            for (int i = 1; i < tabbedPane.getTabCount(); i++) {
                PainelChatPVT p = (PainelChatPVT) tabbedPane.getComponentAt(i);
                if (p.getUsuario().equals(remetente)) {
                    tabbedPane.remove(p);
                    chatsAbertos.remove(p.getUsuario());
                    break;
                }
            }
        }
    }





}