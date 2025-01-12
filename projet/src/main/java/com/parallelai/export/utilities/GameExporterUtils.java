package com.parallelai.export.utilities;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.parallelai.game.Board;
import com.parallelai.game.Disc;

/**
 * Classe utilitaire contenant les outils nécessaires à l'export des états de
 * jeu.
 * Fournit des implémentations pour:
 * - L'affichage de la progression
 * - La compression des états
 * - La mise en cache des états
 * - La gestion des données de jeu
 */
public class GameExporterUtils {

    /**
     * Barre de progression pour l'affichage en temps réel de l'avancement des
     * opérations.
     * Supporte l'affichage multi-thread avec plusieurs barres simultanées.
     */
    public static class ProgressBar {
        private final int total;
        private int current;
        private final int width;
        private final int threadId;
        private static final Object lock = new Object();

        public ProgressBar(int total, int threadId) {
            this.total = total;
            this.width = 30;
            this.current = 0;
            this.threadId = threadId;
        }

        public synchronized void update(int value) {
            // On utilise uniquement la valeur locale du thread, pas le compte global
            this.current = value;
            print();
        }

        private void print() {
            synchronized (lock) {
                float percent = (float) current / total;
                int progress = (int) (width * percent);

                // Ajouter +1 pour tenir compte du titre ajouté
                System.out.print(String.format("\033[%dH\033[K", threadId + 2));
                System.out.print(String.format("Thread %2d [", threadId));
                for (int i = 0; i < width; i++) {
                    if (i < progress)
                        System.out.print("=");
                    else if (i == progress)
                        System.out.print(">");
                    else
                        System.out.print(" ");
                }
                System.out.print(String.format("] %3d%% (%d/%d)",
                        (int) (percent * 100), current, total));
            }
        }

        /**
         * Initialise l'affichage pour plusieurs barres de progression.
         * Efface l'écran et prépare l'espace pour chaque thread.
         *
         * @param nbThreads Nombre de threads/barres de progression à afficher
         */
        public static void initDisplay(int nbThreads) {
            // Effacer l'écran et préparer l'espace pour chaque barre
            System.out.print("\033[2J"); // Effacer l'écran
            System.out.print("\033[H"); // Retour en haut
            System.out.println("Progression des parties :\n"); // Ajouter un titre et un saut de ligne
            for (int i = 0; i < nbThreads; i++) {
                System.out.println(); // Créer une ligne vide pour chaque thread
            }
        }
    }

    /**
     * Gestionnaire de buffer pour la compression et mise en cache des états de jeu.
     * Utilise un système de cache LRU pour optimiser les accès répétés.
     */
    public static class StateBuffer {
        @SuppressWarnings("unused")
        private final char[] buffer;
        private final LRUCache<Board, CompressedState> cache;

        public StateBuffer() {
            this.buffer = new char[64];
            this.cache = new LRUCache<>(1000); // Cache des 1000 derniers états
        }

        public CompressedState compressState(Board board) {
            CompressedState cached = cache.get(board);
            if (cached != null)
                return cached;

            Disc[][] grid = board.getGrid();
            int idx = 0;
            byte[] compressed = new byte[16]; // 64 positions = 16 bytes (4 positions par byte)

            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    int value = grid[i][j] == Disc.BLACK ? 1 : grid[i][j] == Disc.WHITE ? 2 : 0;
                    compressed[idx / 4] |= (value << ((idx % 4) * 2));
                    idx++;
                }
            }

            CompressedState state = new CompressedState(compressed);
            cache.put(board.copy(), state);
            return state;
        }
    }

    /**
     * Cache à capacité limitée utilisant une politique de remplacement LRU
     * (Least Recently Used).
     * 
     * @param <K> Type de la clé
     * @param <V> Type de la valeur
     */
    public static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public LRUCache(int maxSize) {
            super(16, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }

    /**
     * Représentation compressée d'un état de jeu.
     * Utilise 2 bits par case pour encoder l'état (vide/noir/blanc),
     * réduisant ainsi la taille mémoire nécessaire.
     */
    public static class CompressedState {
        private final byte[] data;

        public CompressedState(byte[] data) {
            this.data = data;
        }

        /**
         * Décompresse l'état en tableau de doubles.
         * 
         * @return Un tableau de 67 éléments contenant l'état du jeu et ses métadonnées
         */
        public double[] decompress() {
            double[] state = new double[67]; // Changer 66 en 67
            for (int i = 0; i < 64; i++) {
                int value = (data[i / 4] >> ((i % 4) * 2)) & 0x3;
                state[i] = value == 1 ? 1.0 : value == 2 ? -1.0 : 0.0;
            }
            return state;
        }

        @Override
        public String toString() {
            return Base64.getEncoder().encodeToString(data);
        }
    }

    /**
     * Conteneur pour stocker l'historique d'une partie et son résultat.
     * Permet de conserver l'ensemble des états traversés pendant une partie
     * ainsi que le résultat final.
     */
    public static class GameState {
        public final List<CompressedState> history;
        public final int result;

        public GameState(List<CompressedState> history, int result) {
            this.history = history;
            this.result = result;
        }
    }

}
