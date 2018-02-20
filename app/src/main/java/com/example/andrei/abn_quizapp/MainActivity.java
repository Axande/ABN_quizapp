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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    ImageView[] im;
    TextView[] am;

    TextView question;

    //image alpha value for active and non-active images
    final int active = 255;
    final int passive = 100;

    //variable to determine which textview and imageview to change
    int fieldInFocus = 0;
    int toUpdate = 0;

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
            checkSpecialQ3();

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
            checkSpecialQ3();

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

        //check if the current element is a type3 question and save its data
        checkSpecialQ3();

        if(phase == 0){
            phase = 1; //review phase

            removeListeners();

            //send the user to the first question to start review
            activeQuestion = 0;
            updateQNo();
            enableQuestion();

            //change the text on the button and prepare for reset
            Button btn = findViewById(R.id.submit);
            btn.setText(getResources().getString(R.string.reset));

            //calculate the score
            calculateScore();

            //check all the answer of active question
            checkAnswers();
        } else{
            phase = 0; //active phase

            addListeners();
            resetAll();
            activeQuestion = 0;
            updateQNo();
            enableQuestion();

            Button btn = findViewById(R.id.submit);
            btn.setText(getResources().getString(R.string.submit));
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
        if(Q[val].cath == 1 || Q[val].cath == 2){
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

        if(Q[activeQuestion].cath == 1 || Q[activeQuestion].cath == 2){
            if(answerIsCorrect(activeQuestion))
                question.setBackgroundColor(getResources().getColor(R.color.correct));
                return;
        }
        else if(Q[activeQuestion].cath == 3){
            if(answerIsCorrect(activeQuestion))
                question.setBackgroundColor(getResources().getColor(R.color.correct));
                return;
        }
        question.setBackgroundColor(getResources().getColor(R.color.incorrect));
    }

    private void initialize(){

        question = findViewById(R.id.question);

        im = new ImageView[5];
        am = new TextView[5];

        for(int i = 1; i <= 4; i++) {
            im[i] = findViewById(getId("img" + i));
            am[i] = findViewById(getId("answer" + i + "_type1"));
        }

        addListeners();
        enableQuestion();//set the first question
    }

    private int getId(String s){
        if(s.compareTo("img1") == 0) return R.id.img1;
        if(s.compareTo("img2") == 0) return R.id.img2;
        if(s.compareTo("img3") == 0) return R.id.img3;
        if(s.compareTo("img4") == 0) return R.id.img4;

        if(s.compareTo("answer1_type1") == 0) return R.id.answer1_type1;
        if(s.compareTo("answer2_type1") == 0) return R.id.answer2_type1;
        if(s.compareTo("answer3_type1") == 0) return R.id.answer3_type1;
        if(s.compareTo("answer4_type1") == 0) return R.id.answer4_type1;

        return 0;
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

            if(Q[i].cath == 1 || Q[i].cath == 2){
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
        LinearLayout L2 = findViewById(R.id.question_type2);
        LinearLayout L3 = findViewById(R.id.question_type3);
        L1.setVisibility(View.GONE);
        L2.setVisibility(View.GONE);
        L3.setVisibility(View.GONE);

        if(Q[activeQuestion].cath == 1){
            L1.setVisibility(View.VISIBLE);
            fillQ1();
        }
        else if(Q[activeQuestion].cath == 2){
            L2.setVisibility(View.VISIBLE);
            fillQ2();
        }
        else if(Q[activeQuestion].cath == 3){
            L3.setVisibility(View.VISIBLE);
            fillQ3();
        }
    }

    private void fillQ1(){
        for(int i = 1; i <= 4; i++){
            am[i].setText(Q[activeQuestion].answ[i]);

            //need to update the opacity of the images(if it was already selected by the user)
            if(!Q[activeQuestion].userAnswers[i]) am[i].setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            else am[i].setBackgroundColor(getResources().getColor(R.color.colorAccent));
        }
    }

    private void fillQ2(){
        for(int i = 1; i <= 4; i++){
            //set the images according to the answer
            im[i].setImageResource(getImage(Q[activeQuestion].answ[i]));

            //need to update the opacity of images
            if(Q[activeQuestion].userAnswers[i]) im[i].setImageAlpha(active);
            else im[i].setImageAlpha(passive);
        }
    }

    /*
    Since I could not find a way to access a resource by using a string, I had to implement this
    method.
    */
    private int getImage(String s){
        if(s.compareTo("cow") == 0) return R.drawable.cow;
        if(s.compareTo("monkey") == 0) return R.drawable.monkey;
        if(s.compareTo("elephant") == 0) return R.drawable.elephant;
        if(s.compareTo("pig") == 0) return R.drawable.pig;
        if(s.compareTo("snail") == 0) return R.drawable.snail;
        if(s.compareTo("bear") == 0) return R.drawable.bear;
        if(s.compareTo("rabbit") == 0) return R.drawable.rabbit;

        return 0;
    }

    //when type3 question appears, use this function to fill the fields
    private void fillQ3(){
        EditText et = findViewById(R.id.answerQ3);
        et.setText(Q[activeQuestion].userAnswerCath3);

        if(phase != 0){
            et.setEnabled(false);
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
        EditText et = findViewById(R.id.answerQ3);
        et.setEnabled(true);

        question.setBackgroundColor(getResources().getColor(R.color.neutral));
    }

    //this is a case to save the data given in a type 3 question. Otherwise data won`t be saved
    private void checkSpecialQ3(){
        if(Q[activeQuestion].cath == 3){
            EditText et = findViewById(R.id.answerQ3);
            Q[activeQuestion].userAnswerCath3 = String.valueOf(et.getText());
        }
    }

    //update the question number textView
    private void updateQNo(){
        TextView tw = findViewById(R.id.question_number);
        tw.setText((activeQuestion+1) + "/" + questionNumber);
    }

    //when a click on textView occurs, this is triggered
    private void changeTextField(){
        int i;

        Log.println(Log.INFO, "", "fieldInFocus = " + fieldInFocus);
        for(i = 1; i <= 4; i++){
            if(am[i].getId() == fieldInFocus){
                toUpdate = i;
                Log.println(Log.INFO, "", "which = " + i);
                break;
            }
        }
        if(i == 5) return;

        if(!Q[activeQuestion].userAnswers[toUpdate]){
            Q[activeQuestion].userAnswers[toUpdate] = true;
            am[toUpdate].setBackgroundColor(getResources().getColor(R.color.colorAccent));
        } else{
            Q[activeQuestion].userAnswers[toUpdate] = false;
            am[toUpdate].setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    //when a click on image occurs, this is triggered
    private void changeImageField(){
        int i;

        Log.println(Log.INFO, "", "fieldInFocus = " + fieldInFocus);
        for(i = 1; i <= 4; i++){
            if(im[i].getId() == fieldInFocus){
                toUpdate = i;
                Log.println(Log.INFO, "", "which = " + i);
                break;
            }
        }
        if(i == 5) return;

        for(i = 1; i <= 4; i++) {
            im[i].setImageAlpha(passive);
            Q[activeQuestion].userAnswers[i] = false;
        }

        im[toUpdate].setImageAlpha(active);
        Q[activeQuestion].userAnswers[toUpdate] = true;
    }

    /*
    Add listeners to the 4 images and text views defined in the xml file. The elements that are not
    active at the moment are hidden, but still need to have handlers implemented.
     */
    private void addListeners(){

        for(int i = 1; i <= 4; i++){

            am[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fieldInFocus = view.getId();
                    changeTextField();
                }
            });

            im[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fieldInFocus = view.getId();
                    changeImageField();
                }
            });
        }
    }

    //detach all listeners set before. used for verification phase
    private void removeListeners(){
        for(int i = 1; i <= 4; i++){
            am[i].setOnClickListener(null);
            im[i].setOnClickListener(null);
        }
    }
}
