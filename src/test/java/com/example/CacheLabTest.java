package com.example;

import static org.junit.jupiter.api.Assertions.*;

import com.example.model.Auteur;
import com.example.model.Livre;
import com.example.service.DataInitService;
import com.example.service.PerformanceTestService;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CacheLabTest {
    private static EntityManagerFactory emf;
    private static List<Long> ids;
    private static PerformanceTestService service;

    @BeforeAll static void setup() {
        emf = Persistence.createEntityManagerFactory("hibernate-cache-pu");
        ids = new DataInitService(emf).initData();
        service = new PerformanceTestService(emf);
    }

    @AfterAll static void tearDown() { if (emf != null) emf.close(); }

    @Test void fixtureAndBidirectionalRelation() {
        assertEquals(10, ids.size());
        EntityManager em = emf.createEntityManager();
        try {
            Auteur auteur = em.find(Auteur.class, ids.get(0));
            assertEquals(5, auteur.getLivres().size());
            for (Livre livre : auteur.getLivres()) assertSame(auteur, livre.getAuteur());
        } finally { em.close(); }
    }

    @Test void nPlusOneAndFetchOptimisations() {
        PerformanceTestService.Result baseline = service.testPerformanceSansCache();
        PerformanceTestService.Result join = service.testPerformanceAvecJoinFetch();
        PerformanceTestService.Result graph = service.testPerformanceAvecEntityGraph();
        assertEquals(50, baseline.rows);
        assertEquals(11, baseline.sqlStatements);
        assertEquals(50, join.rows);
        assertEquals(1, join.sqlStatements);
        assertEquals(50, graph.rows);
        assertEquals(1, graph.sqlStatements);
    }

    @Test void secondLevelCacheAcrossSessions() {
        PerformanceTestService.Result result = service.testPerformanceAvecCache(ids.get(0));
        assertEquals(1, result.rows);
        assertEquals(0, result.sqlStatements);
        assertTrue(result.secondLevelHits > 0);
    }

    @Test void queryCacheAcrossSessions() {
        PerformanceTestService.Result result = service.testCacheRequete();
        assertEquals(10, result.rows);
        assertEquals(0, result.sqlStatements);
        assertTrue(result.queryHits > 0);
    }
}
