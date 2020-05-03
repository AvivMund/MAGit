package magit.ui;

import magit.engine.Engine;

public class Main {
    public static void main(String[] args){
        Ui magitUi = new Ui(new Engine());
        magitUi.start();
    }
}
