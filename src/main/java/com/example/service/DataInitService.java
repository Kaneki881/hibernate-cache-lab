package com.example.service;

import com.example.model.Auteur;
import com.example.model.Livre;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

/** Seeds the standalone RESOURCE_LOCAL persistence unit in one transaction. */
public class DataInitService {
    private final EntityManagerFactory emf;

    public DataInitService(EntityManagerFactory emf) { this.emf = emf; }

    public List<Long> initData() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            List<Long> ids = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                Auteur auteur = new Auteur("Auteur " + i);
                for (int j = 1; j <= 5; j++) {
                    auteur.ajouterLivre(new Livre("Livre " + j + " de l'auteur " + i));
                }
                em.persist(auteur);
                ids.add(auteur.getId());
            }
            tx.commit();
            return ids;
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
