package com.example.andrei.abn_quizapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.andrei.abn_quizapp.Questions;
import com.example.andrei.abn_quizapp.R;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    /*
    0: initial
    1: review(after submit button is pressed)
    2: reload(when the app is reset)
     */
    int phase = 0;

    int questionNumber = 0; //number of questions
    int activeQuestion = 0; //current displayed question(from 0 to questionNumber)

    Questions Q[] = new Questions[50]; //array of 50 questions

    //used for action listeners on images
    RadioGroup rg;
    CheckBox[] cb;

    TextView question;
    EditText answerQ3;

    Button btn_submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try { //has to use try-catch because of the IOException.
            readQuestions();
        } catch(IOException e){ //if the file is not found, this error shows up
            System.out.println(R.string.err_read);
        }

        //initialize all elements needed
        initialize();
    }

    public void pressedNext(View v) {
        if(phase != 0){
            activeQuestion++;
            if (activeQuestion == questionNumber) {
                activeQuestion = questionNumber - 1;
                return;
            }
            checkAnswers();
        }
        else {
            saveAnswers();

            //move to the next question if there is one
            activeQuestion++;
            if (activeQuestion == questionNumber) {
                activeQuestion = questionNumber - 1;
                return;
            }
        }

        updateQNo();
        enableQuestion();
    }

    public void pressedPrev(View v) {
        if(phase != 0){
            activeQuestion--;
            if(activeQuestion < 0){
                activeQuestion = 0;
                return;
            }
            checkAnswers();
        }
        else{
            saveAnswers();

            //move to the previous question if there is one
            activeQuestion--;
            if(activeQuestion < 0){
                activeQuestion = 0;
                return;
            }
        }

        updateQNo();
        enableQuestion();
    }

    public void submitAnswers(View v) {

        saveAnswers();
        if(phase == 0){
            phase = 1; //review phase

            //send the user to the first question to start review
            activeQuestion = 0;
            updateQNo();
            enableQuestion();

            //change the text on the button and prepare for reset
            btn_submit.setText(getResources().getString(R.string.reset));

            //calculate the score
            calculateScore();

            //check all the answer of active question
            checkAnswers();
        } else{
            phase = 0; //active phase

            resetAll();
            activeQuestion = 0;
            updateQNo();
            enableQuestion();

            btn_submit.setText(getResources().getString(R.string.submit));
        }
    }

    private void saveAnswers(){
        if(Q[activeQuestion].cath == 1){
            for(int i = 1; i <= 4; i++)
                if(cb[i].isChecked())
                    Q[activeQuestion].userAnswers[i] = true;
                else
                    Q[activeQuestion].userAnswers[i] = false;
        }
        else if(Q[activeQuestion].cath == 4){
            for(int i = 0; i < rg.getChildCount(); i++){
                RadioButton rb = (RadioButton) rg.getChildAt(i);
                if(rb.isChecked())
                    Q[activeQuestion].userAnswers[i+1] = true;
                else
                    Q[activeQuestion].userAnswers[i+1] = false;
            }
        }
        else if(Q[activeQuestion].cath == 3){
            Q[activeQuestion].userAnswerCath3 = String.valueOf(answerQ3.getText());
        }
    }

    private void calculateScore(){
        int count = 0;
        for(int i = 0; i < questionNumber; i++){
            if(answerIsCorrect(i)) count++;
        }

        Toast toast = Toast.makeText(getApplicationContext(), "You answered " + count + " correct questions", Toast.LENGTH_LONG);
        toast.show();
    }

    private boolean answerIsCorrect(int val){
        if(Q[val].cath == 1 || Q[val].cath == 4){
            for(int i = 1; i <= 4; i++){
                if(Q[val].userAnswers[i] != Q[val].correct[i]){
                    return false;
                }
            }
        }
        else if(Q[val].cath == 3){
            if(Q[val].userAnswerCath3.trim().toLowerCase()
                    .compareTo(Q[val].answ[0].trim().toLowerCase()) != 0)
                return false;
        }
        return true;
    }

    private void checkAnswers(){
        if(answerIsCorrect(activeQuestion)) {
            question.setBackgroundColor(getResources().getColor(R.color.correct));
            return;
        }
        question.setBackgroundColor(getResources().getColor(R.color.incorrect));
    }

    private void initialize(){

        question = findViewById(R.id.question);

        rg = findViewById(R.id.answerQ4);
        cb = new CheckBox[5];
        cb[1] = findViewById(R.id.answer1);
        cb[2] = findViewById(R.id.answer2);
        cb[3] = findViewById(R.id.answer3);
        cb[4] = findViewById(R.id.answer4);

        answerQ3 = findViewById(R.id.answerQ3);

        btn_submit = findViewById(R.id.submit);

        enableQuestion();//set the first question
    }

    void readQuestions() throws IOException {
        String read = "";
        String aux = "";

        //get the file stream
        Scanner br = new Scanner(getResources().openRawResource(R.raw.questions));
        questionNumber = Integer.parseInt(br.nextLine());

        for(int i = 0; i < questionNumber; i++) {
            Q[i] = new Questions(); //initialize the question
            Q[i].question = br.nextLine(); //read actual question

            Q[i].cath = Integer.parseInt(br.nextLine()); //read the cathegory

            if(Q[i].cath == 1 || Q[i].cath == 4){
                for(int j = 1; j <= 4; j++){
                    read = br.nextLine();
                    aux = isCorrect(read);
                    if(aux.compareTo(read) == 0) Q[i].correct[j] = false;
                    else Q[i].correct[j] = true;

                    Q[i].answ[j] = aux;
                }
            }
            else if(Q[i].cath == 3){
                Q[i].answ[0] = br.nextLine();
            }

            if(br.hasNextLine())
                br.nextLine();//skip the empty line
        }
    }

    private String isCorrect(String s){
        if(s.startsWith("X")){ //cut the beginning and return the string
            return s.substring(2); //to check
        }
        return s;
    }

    private void enableQuestion(){

        question.setText(Q[activeQuestion].question);//set the correct question

        LinearLayout L1 = findViewById(R.id.question_type1);
        LinearLayout L2 = findViewById(R.id.question_type4);
        LinearLayout L3 = findViewById(R.id.question_type3);
        L1.setVisibility(View.GONE);
        L2.setVisibility(View.GONE);
        L3.setVisibility(View.GONE);

        if(Q[activeQuestion].cath == 1){
            L1.setVisibility(View.VISIBLE);
            fillQ1();
        }
        else if(Q[activeQuestion].cath == 4){
            L2.setVisibility(View.VISIBLE);
            fillQ4();
        }
        else if(Q[activeQuestion].cath == 3){
            L3.setVisibility(View.VISIBLE);
            fillQ3();
        }
    }

    private void fillQ1(){
        for(int i = 1; i <= 4; i++){
            cb[i].setText(Q[activeQuestion].answ[i]);

            //need to update the opacity of the images(if it was already selected by the user)
            if(Q[activeQuestion].userAnswers[i]){
                cb[i].setTextColor(getResources().getColor(R.color.neutral));
                cb[i].setChecked(true);
            }
            else {
                cb[i].setTextColor(getResources().getColor(R.color.active));
                cb[i].setChecked(false);
            }
        }
    }

    private void fillQ4(){
        rg.clearCheck();
        for(int i = 0; i < rg.getChildCount(); i++){
            RadioButton rb = (RadioButton) rg.getChildAt(i);

            rb.setText(Q[activeQuestion].answ[i+1]);

            //TODO: there is a small bug
            if(Q[activeQuestion].userAnswers[i+1]){
                rb.setChecked(true);
                rb.setTextColor(getResources().getColor(R.color.active));
            }
            else rb.setTextColor(getResources().getColor(R.color.neutral));
        }
    }

    //when type3 question appears, use this function to fill the fields
    private void fillQ3(){
        answerQ3.setText(Q[activeQuestion].userAnswerCath3);

        if(phase != 0){
            answerQ3.setEnabled(false);
        }
    }

    //reset all savings(but not the read elements from the file)
    private void resetAll() {
        //clear all user's answers
        for (int i = 0; i < questionNumber; i++) {
            for (int j = 0; j < 5; j++)
                Q[i].userAnswers[j] = false;
            Q[i].userAnswerCath3 = "";
        }
        activeQuestion = 0;

        //enable question3 field which will be disabled for feedback part
        answerQ3.setEnabled(true);

        question.setBackgroundColor(getResources().getColor(R.color.neutral));
    }

    //update the question number textView
    private void updateQNo(){
        TextView tw = findViewById(R.id.question_number);
        tw.setText((activeQuestion+1) + "/" + questionNumber);
    }

}
