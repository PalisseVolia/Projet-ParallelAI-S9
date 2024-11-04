/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.projet.game.apiJeux;

import com.projet.game.othello.JeuOthello;
import com.projet.game.othello.SituationOthello;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Random;

/**
 *
 * @author francois
 */
public class Utils {

    private static <Sit extends Situation> void generateUneLigneCSVOfSituations(
            Writer curWriter,
            Sit curSit, double res, int numCoup, int totCoup,
            boolean includeRes, boolean includeNumCoup, boolean includeTotCoup) throws IOException {
        curWriter.append(curSit.toCSV());
        if (includeRes) {
            curWriter.append("," + res);
        }
        if (includeNumCoup) {
            curWriter.append("," + numCoup);
        }
        if (includeTotCoup) {
            curWriter.append("," + totCoup);
        }
        curWriter.append("\n");
    }

    public static <Sit extends Situation, Co extends Coup> void generateCSVOfSituations(
            Writer outJ1, Writer outJ2,
            Jeu<Sit, Co> jeu, Oracle<Sit> j1, Oracle<Sit> j2,
            int nbrParties,
            boolean includeRes, boolean includeNumCoup, boolean includeTotCoup,
            Random rand) throws IOException {
        for (int i = 0; i < nbrParties; i++) {
            ResumeResultat<Co> resj = jeu.partie(j1,ChoixCoup.ORACLE_PONDERE,
                    j2, ChoixCoup.ORACLE_PONDERE, false, false, rand,false);
            // je rejoue la partie pour avoir les situations
            Sit curSit = jeu.situationInitiale();
            Writer curOut = outJ1;
            double curRes;
            if (resj.getStatutFinal() == StatutSituation.J1_GAGNE) {
                curRes = 1;
            } else if (resj.getStatutFinal() == StatutSituation.J2_GAGNE) {
                curRes = 0;
            } else if (resj.getStatutFinal() == StatutSituation.MATCH_NUL) {
                curRes = 0.5;
            } else {
                throw new Error("partie non finie");
            }
            int totCoups = resj.getCoupsJoues().size();
            int numCoup = 0;
            generateUneLigneCSVOfSituations(curOut, curSit, curRes, numCoup, totCoups, includeRes, includeNumCoup, includeTotCoup);
            Joueur curJoueur = Joueur.J1;
            for (Co curCoup : resj.getCoupsJoues()) {
                curSit = jeu.updateSituation(curSit, curJoueur, curCoup);
                if (curOut == outJ1) {
                    curOut = outJ2;
                } else {
                    curOut = outJ1;
                }
                curRes = 1 - curRes;
                numCoup++;
                generateUneLigneCSVOfSituations(curOut, curSit, curRes, numCoup, totCoups, includeRes, includeNumCoup, includeTotCoup);
                curJoueur = curJoueur.adversaire();
            }
        }
    }

    public static <Sit extends Situation, Co extends Coup> void generateCSVOfSituations(
            File outJ1, File outJ2,
            Jeu<Sit, Co> jeu, Oracle<Sit> j1, Oracle<Sit> j2,
            int nbrParties,
            boolean includeRes, boolean includeNumCoup, boolean includeTotCoup,
            Random rand) throws IOException {
        try (FileWriter wJ1 = new FileWriter(outJ1); FileWriter wJ2 = new FileWriter(outJ2)) {
            generateCSVOfSituations(wJ1, wJ2, jeu, j1, j2, nbrParties, includeRes, includeNumCoup, includeTotCoup,rand);
        }
    }

    public static void testAvecOthello(int nbr) {
        try {
            File dir = new File("projet\\src\\main\\java\\com\\projet\\game\\temp");
            generateCSVOfSituations(new File(dir, "noirs" + nbr +".csv"), new File(dir, "blancs"+nbr+".csv"),
                    new JeuOthello(), new OracleStupide<SituationOthello>(Joueur.J1), new OracleStupide<SituationOthello>(Joueur.J2),
                    nbr, true, true, true,new Random());
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }
    
    public static void main(String[] args) {
        testAvecOthello(10);
    }

}
