package com.chesscoach.app;
public enum Difficulty {
    BEGINNER("입문 · Skill 0",0,0),
    ELO_1320("목표 Elo 1320",1320,20),
    ELO_1600("목표 Elo 1600",1600,20),
    ELO_2000("목표 Elo 2000",2000,20),
    ELO_2400("목표 Elo 2400",2400,20),
    ELO_2800("목표 Elo 2800",2800,20),
    ELO_3190("목표 Elo 3190",3190,20),
    FULL("최강 · 실력 제한 없음",0,20);
    public final String label;public final int elo,skill;
    Difficulty(String label,int elo,int skill){this.label=label;this.elo=elo;this.skill=skill;}
    public static Difficulty from(String name){try{return valueOf(name);}catch(Exception e){return ELO_1600;}}
}
