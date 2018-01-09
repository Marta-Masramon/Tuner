#include <Servo.h>
Servo myServo;
char read_bt;
boolean pick;

void setup() {
  myServo.write(0);
  myServo.attach(11);
  pinMode(2,OUTPUT);
  pinMode(3,OUTPUT);
  pinMode(4,OUTPUT);
  pinMode(5,OUTPUT);
  pinMode(6,OUTPUT);
  pinMode(7,OUTPUT);
  pinMode(8,OUTPUT);
  pinMode(9,OUTPUT);
  pinMode(10,OUTPUT);
  Serial.begin(38400);   
}

void loop() { 
  if (Serial.available() > 0) {
    read_bt = Serial.read();
    //Serial.print(read_bt);
    
    if(read_bt=='P') turnAndPick();
    else for(int i=2; i<11; i++) digitalWrite(i, LOW);
    
    if(read_bt=='1') digitalWrite(2, HIGH);  
    else if(read_bt=='2') digitalWrite(3, HIGH); 
    else if(read_bt=='3') digitalWrite(4, HIGH); 
    else if(read_bt=='4') digitalWrite(5, HIGH); 
    else if(read_bt=='5') digitalWrite(6, HIGH); 
    else if(read_bt=='6') digitalWrite(7, HIGH); 
  
    else if(read_bt=='L') {
      digitalWrite(8, HIGH); 
      turnAndPick();
    }
    else if(read_bt=='D') digitalWrite(9, HIGH);  
    else if(read_bt=='H') {
      digitalWrite(10, HIGH); 
      turnAndPick();
    }
  }
}

void turnAndPick() {
  if(pick==true){
    myServo.write(0);
    pick = false;
    Serial.println("picked");
  }
  else{
    myServo.write(90);
    pick = true;
    Serial.println("picked");
  } 
}



