package com.thomasdh.roosterpgplus.Models;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("UnusedDeclaration")
public class Klas implements Serializable {
    private static final long serialVersionUID = 102947212471347250L;
    public String klas;
    public ArrayList<Leerling> leerlingen;

    public void setLeerlingen(Leerling leerling) {
        if(leerlingen == null) { leerlingen = new ArrayList<>(); }
        leerlingen.add(leerling);
    }

    public void setLeerlingen(ArrayList<Leerling> leerlingen) {
        if(this.leerlingen == null) { this.leerlingen = new ArrayList<>(); }
        this.leerlingen.addAll(leerlingen);
    }

    public Klas(String naam) {
        klas = naam;
    }
}
