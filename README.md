# Architecture du package Java pour le projet Othello/ML

## Structure du package

``` java
com.projetothello
├── game
│   ├── Othello.java
│   ├── Plateau.java
│   ├── Joueur.java
│   └── Coup.java
├── ai
│   ├── Oracle.java
│   ├── NeuralNetwork.java
│   └── Evaluator.java
├── learning
│   ├── GameRecorder.java
│   ├── TrainingManager.java
│   └── OracleImprover.java
├── util
│   ├── MatrixOperations.java
│   └── RandomGenerator.java
├── parallel
│   ├── GameRunner.java
│   └── DistributedLearning.java
└── Main.java
```

## Explication des packages

### 1. Package `game`

Ce package contient les classes liées à la logique du jeu Othello.

- **Othello.java** : Classe principale qui gère le déroulement d'une partie.
- **Plateau.java** : Représente le plateau de jeu, avec ses méthodes pour placer les pions et vérifier les coups valides.
- **Joueur.java** : Classe abstraite ou interface représentant un joueur, qui peut être soit humain soit une IA.
- **Coup.java** : Représente un coup joué, avec les coordonnées et éventuellement d'autres informations.

### 2. Package `ai`

Ce package contient les classes liées à l'intelligence artificielle.

- **Oracle.java** : Interface pour les fonctions d'évaluation, définissant la méthode d'évaluation d'une situation de jeu.
- **NeuralNetwork.java** : Implémentation du réseau de neurones utilisé comme Oracle.
- **Evaluator.java** : Utilise l'Oracle pour évaluer les situations et choisir le meilleur coup.

### 3. Package `learning`

Ce package gère l'apprentissage et l'amélioration des Oracles.

- **GameRecorder.java** : Enregistre le déroulement des parties pour l'analyse ultérieure.
- **TrainingManager.java** : Gère le processus d'entraînement global des Oracles.
- **OracleImprover.java** : Implémente la logique pour améliorer les Oracles basée sur les résultats des parties.

### 4. Package `util`

Ce package contient des classes utilitaires pour le projet.

- **MatrixOperations.java** : Fournit des méthodes pour les opérations sur les matrices, utilisées dans les réseaux de neurones.
- **RandomGenerator.java** : Implémente la génération de nombres aléatoires pour la sélection de coups pendant l'apprentissage.

### 5. Package `parallel`

Ce package gère l'exécution parallèle et distribuée du système.

- **GameRunner.java** : Responsable de l'exécution de plusieurs parties en parallèle.
- **DistributedLearning.java** : Gère l'aspect distribué de l'apprentissage, permettant de répartir la charge sur plusieurs machines.

### 6. Classe `Main.java`

Point d'entrée du programme, initialise les composants nécessaires et lance le processus d'apprentissage.

## Conclusion

Cette structure de package permet une organisation claire et modulaire du projet. Elle sépare les différentes responsabilités (jeu, IA, apprentissage) tout en fournissant une base solide pour l'implémentation des fonctionnalités parallèles et distribuées. Cette architecture facilitera le développement, la maintenance et l'extension future du projet.
