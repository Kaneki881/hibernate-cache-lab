package com.example;

import com.example.service.DataInitService;
import com.example.service.PerformanceTestService;
import java.util.List;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hibernate-cache-pu");
        try {
            List<Long> ids = new DataInitService(emf).initData();
            PerformanceTestService tests = new PerformanceTestService(emf);
            System.out.println("N+1 (10 auteurs, 50 livres) : " + tests.testPerformanceSansCache());
            System.out.println("JOIN FETCH : " + tests.testPerformanceAvecJoinFetch());
            System.out.println("Entity Graph : " + tests.testPerformanceAvecEntityGraph());
            System.out.println("Cache L2, deuxième session : " + tests.testPerformanceAvecCache(ids.get(0)));
            System.out.println("Cache de requête, deuxième session : " + tests.testCacheRequete());
        } finally { emf.close(); }
    }
}
