/*
 Copyright 2000-2011 Francois de Bertrand de Beuvron

 This file is part of CoursBeuvron.

 CoursBeuvron is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 CoursBeuvron is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.projet.game.othelloSimpleV2;


/**
 * Cette classe représente le contenu d'une case. 
 * <pre>
 * <p> défini comme tous les enums un ordre arbitraire {@code NOIR < BLANC < VIDE} qui pourra nous
 * servir pour trouver un représentant unique de l'équivalence des damiers
 * par symetries
 * </p>
 * </pre>
 */
public enum Case{
    NOIR,BLANC,VIDE;
    
    public String toString() {
        if (this==VIDE) {
            return ".";
        } else if (this == NOIR) {
            return "X";
        } else {
            return "O";
        }
    }    
    
    /** 
     * pour certains tests.
     * @return un contenu NOIR,BLANC ou vide avec une proba 1/3 1/3 1/3
     */
    public static Case caseAlea() {
        double rand = Math.random();
        if (rand < 1.0/3) {
            return Case.NOIR;
        } else if (rand < 2.0/3) {
            return Case.BLANC;
        } else {
            return Case.VIDE;
        }
    }
}
