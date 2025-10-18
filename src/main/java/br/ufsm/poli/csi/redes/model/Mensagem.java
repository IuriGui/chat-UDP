package br.ufsm.poli.csi.redes.model;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Mensagem {

    private String usuario;
    private String status;
    private TipoMensagem tipoMensagem;
    private String msg;

}
