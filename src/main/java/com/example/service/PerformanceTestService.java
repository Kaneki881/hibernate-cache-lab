package com.example.service;

import com.example.model.Auteur;
import java.util.List;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/** Each measurement uses a fresh EntityManager so its first-level cache cannot mask SQL. */
public class PerformanceTestService {
    private final EntityManagerFactory emf;
    private final SessionFactory sessionFactory;
    private final Statistics statistics;

    public PerformanceTestService(EntityManagerFactory emf) {
        this.emf = emf;
        this.sessionFactory = emf.unwrap(SessionFactory.class);
        this.statistics = sessionFactory.getStatistics();
    }

    public Result testPerformanceSansCache() {
        resetCaches();
        return measure(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                List<Auteur> auteurs = em.createQuery("SELECT a FROM Auteur a ORDER BY a.id", Auteur.class).getResultList();
                return countBooks(auteurs);
            } finally { em.close(); }
        });
    }

    public Result testPerformanceAvecJoinFetch() {
        resetCaches();
        return measure(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                List<Auteur> auteurs = em.createQuery(
                    "SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres ORDER BY a.id", Auteur.class).getResultList();
                return countBooks(auteurs);
            } finally { em.close(); }
        });
    }

    public Result testPerformanceAvecEntityGraph() {
        resetCaches();
        return measure(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                EntityGraph<?> graph = em.getEntityGraph("Auteur.livres");
                TypedQuery<Auteur> query = em.createQuery("SELECT DISTINCT a FROM Auteur a ORDER BY a.id", Auteur.class);
                query.setHint("javax.persistence.fetchgraph", graph);
                return countBooks(query.getResultList());
            } finally { em.close(); }
        });
    }

    /** A second-level entity cache hit must occur across two separate sessions. */
    public Result testPerformanceAvecCache(Long auteurId) {
        resetCaches();
        EntityManager first = emf.createEntityManager();
        try { first.find(Auteur.class, auteurId); }
        finally { first.close(); }
        statistics.clear();
        return measure(() -> {
            EntityManager second = emf.createEntityManager();
            try { return second.find(Auteur.class, auteurId) == null ? 0 : 1; }
            finally { second.close(); }
        });
    }

    /** Query cache is separate from the entity cache and requires an explicit cacheable hint. */
    public Result testCacheRequete() {
        resetCaches();
        runCacheableQuery();
        statistics.clear();
        return measure(this::runCacheableQuery);
    }

    private int runCacheableQuery() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT a FROM Auteur a ORDER BY a.id", Auteur.class)
                .setHint("org.hibernate.cacheable", true).getResultList().size();
        } finally { em.close(); }
    }

    private int countBooks(List<Auteur> auteurs) {
        int count = 0;
        for (Auteur auteur : auteurs) count += auteur.getLivres().size();
        return count;
    }

    private void resetCaches() {
        sessionFactory.getCache().evictAllRegions();
        statistics.clear();
    }

    private Result measure(Work work) {
        long start = System.nanoTime();
        int rows = work.execute();
        long nanos = System.nanoTime() - start;
        return new Result(rows, statistics.getPrepareStatementCount(),
            statistics.getSecondLevelCacheHitCount(), statistics.getQueryCacheHitCount(), nanos);
    }

    private interface Work { int execute(); }

    public static class Result {
        public final int rows;
        public final long sqlStatements;
        public final long secondLevelHits;
        public final long queryHits;
        public final long nanos;

        public Result(int rows, long sqlStatements, long secondLevelHits, long queryHits, long nanos) {
            this.rows = rows;
            this.sqlStatements = sqlStatements;
            this.secondLevelHits = secondLevelHits;
            this.queryHits = queryHits;
            this.nanos = nanos;
        }

        @Override public String toString() {
            return String.format("résultat=%d, SQL=%d, hits L2=%d, hits requête=%d, durée=%.3f ms",
                rows, sqlStatements, secondLevelHits, queryHits, nanos / 1_000_000.0);
        }
    }
}
