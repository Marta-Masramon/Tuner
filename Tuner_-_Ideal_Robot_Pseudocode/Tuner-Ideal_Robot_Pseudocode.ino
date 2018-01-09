// THIS IS THE ARDUINO ROBOT PSEUDOCODE

void setup() {
}

void moveToNextString(){
  turn 360 degrees; // so the pick is on the other side of the string
  move right until touching next string;
  return;
}

void moveToPosition1(){
  position pick upwards
  center the arm left of strings; // position will depend on instrument
  position pick downwards;
  move right until touching string;
}

void pluckString(){
  turn pick -90 degrees;
  turn pick 180 degrees;
  wait 1 second;
  position pick downwards; // turn -90 degrees
}

void turnClockwise(int peg){
  turn peg 10 degrees;
}

void turnAntiClockwise(int peg){
  turn peg -10 degrees;
}

void loop() {
  moveToPosition1;
  int stringObjective = 0;
  int stringPosition = 1;
  boolean done = false;
  int order;
  
  if (order received){ // order from Android app
    
    stringObjective = order;
    while (stringPosition =! stringObjective) moveToNextString;
    
    while (done == false){
      send "listen" to app;
      pluckString;
      
      wait for order2;
      if (order2 == 0) done = true; // done tuning
      else if (order2 == 1) turnClockwise(stringObjective); // tuning too low 
      else if (order2 == 2) turnAntiClockwise(stringObjective); // tuning too high
    }
  }
}

