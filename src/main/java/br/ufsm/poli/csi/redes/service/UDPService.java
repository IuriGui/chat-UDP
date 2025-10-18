package br.ufsm.poli.csi.redes.service;

import br.ufsm.poli.csi.redes.model.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.SocketException;

public interface UDPService {

    /**
     * Envia uma mensagem para um destinatário
     * @param mensagem
     * @param destinatario
     * @param chatGeral
     */
    void enviarMensagem(String mensagem, Usuario destinatario, boolean chatGeral) throws JsonProcessingException;

    /**
     * Notifica que o próprio usuário foi alterado
     * @param usuario
     */
    void usuarioAlterado(Usuario usuario);

    /**
     * Adiciona um listener para indicar o recebimento de mensagens
     * @param listener
     */
    void addListenerMensagem(UDPServiceMensagemListener listener);

    /**
     * Método a ser chamado para encerrar o chat, quando o usuário clica para fechar o chat na interface
     * @param usuario
     */
    void fimChat(Usuario usuario);

    /**
     * Adiciona um listener para indicar recebimento e/ou alterações em usuários
     * @param listener
     */
    void addListenerUsuario(UDPServiceUsuarioListener listener);

    void iniciarThreads() throws SocketException;

}
