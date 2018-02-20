package com.example.andrei.abn_quizapp;

/**
 * Created by Andrei on 2/18/2018.
 */

public class Questions {
    public String question = "";
    public boolean[] correct = new boolean[5]; //true = correct, false = incorrect
    public String[] answ = new String[5];
    public int cath = 0; //cathegory

    public boolean[] userAnswers = new boolean[5];
    public String userAnswerCath3 = "";

    public void Questions() {

    }

    /*
    Input file example:
    2    :number of questions
    1    :type of question
    aaaa :actual question
    X a1 : X means the answer is correct
    a2
    a3
    a4   :actual answers
    3    :type of the question
    a    :actual answer

    For type1 questions: multiple answer can be correct
    For type2 questions: just one answer shall be correct
    For type3 questions: an answer is correct given in answ[0]
     */
}
