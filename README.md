# TP7 — Cache Hibernate, N+1 et optimisation JPA

Ce projet Java 8+ utilise JPA 2.2, Hibernate 5.4, Ehcache 3 (via JCache) et H2 en mémoire. Il crée 10 auteurs et 50 livres puis mesure les requêtes SQL pour quatre scénarios. La base est recréée à chaque exécution.

## Lancer

Dans ce dossier, exécuter `mvn clean test` puis `mvn exec:java` (ou lancer `com.example.App` depuis l'IDE). Il faut un JDK et Maven. Les durées affichées dépendent de la machine ; les compteurs SQL et de hits Hibernate constituent la vérification fiable.

Résultats attendus : chargement paresseux = 11 SQL (N+1), `JOIN FETCH` = 1 SQL, Entity Graph = 1 SQL, deuxième lecture d'un auteur par identifiant dans un nouvel `EntityManager` = 0 SQL et au moins un hit L2, deuxième requête explicitement cacheable = 0 SQL et au moins un hit du cache de requête. Les tests JUnit vérifient ces résultats.

`hibernate-ehcache` concerne Ehcache 2. Pour Ehcache 3, ce projet utilise `hibernate-jcache` de la même version qu'`hibernate-core`. H2 2.3 est configuré en mode LEGACY pour l'ancienne syntaxe d'identité produite par Hibernate 5.4. Le cache de second niveau fonctionne pour les lectures par identifiant et les collections configurées ; une requête JPQL n'est mise en cache que si le hint `org.hibernate.cacheable` est activé. Chaque scénario ouvre son propre `EntityManager` pour éviter que le cache de premier niveau masque les effets mesurés.
