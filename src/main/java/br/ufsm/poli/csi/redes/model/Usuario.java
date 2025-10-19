package br.ufsm.poli.csi.redes.model;

import br.ufsm.poli.csi.redes.swing.ChatClientSwing;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {

    private String nome;
    private StatusUsuario status;
    private InetAddress endereco;
    private Timestamp lastSonda;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return nome.equals(usuario.nome);
    }

    public Usuario(String nome, StatusUsuario status, InetAddress endereco) {
        this.nome = nome;
        this.status = status;
        this.endereco = endereco;
    }

    @Override
    public int hashCode() {
        return nome.hashCode();
    }

    public String toString() {
        return this.getNome() + " (" + getStatus().toString() + ")";
    }

    public enum StatusUsuario {
        DISPONIVEL, NAO_PERTURBE, VOLTO_LOGO
    }

}
