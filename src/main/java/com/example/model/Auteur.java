package com.example.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "auteurs")
@NamedEntityGraph(name = "Auteur.livres", attributeNodes = @NamedAttributeNode("livres"))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Auteur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @OneToMany(mappedBy = "auteur", cascade = CascadeType.ALL, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Livre> livres = new ArrayList<>();

    public Auteur() { }

    public Auteur(String nom) { this.nom = nom; }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public List<Livre> getLivres() { return Collections.unmodifiableList(livres); }

    public void ajouterLivre(Livre livre) {
        if (livre == null) throw new IllegalArgumentException("livre requis");
        if (livre.getAuteur() != null && livre.getAuteur() != this) {
            livre.getAuteur().retirerLivre(livre);
        }
        if (!livres.contains(livre)) livres.add(livre);
        livre.setAuteur(this);
    }

    public void retirerLivre(Livre livre) {
        if (livres.remove(livre)) livre.setAuteur(null);
    }
}
