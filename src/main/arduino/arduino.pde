

#include "DHT.h"
#include <LiquidCrystal.h>

#define DHTPIN 2     // what digital pin we're connected to

#define DHTTYPE DHT22   // DHT 22  (AM2302), AM2321

DHT dht(DHTPIN, DHTTYPE);
LiquidCrystal lcd(12, 11, 8, 7, 6, 5);

// PINs
#define PIN_PWM 4
#define PIN_RPM 3
#define PIN_UP 9
#define PIN_DOWN 10
#define UPDATE_ZYKLUS 500 // update interval
const int ANZAHL_INTERRUPTS = 1; // amoount of interrupts per round

// Variablen
int counter_rpm = 0;
int rpm = 0;
unsigned long letzte_ausgabe = 0;
char characterInput;
String eingabe;
String display1;
String display2;
int dauer_low = 1;
int dauer_high = 9;
int baseTime = 10; // overall 10 ms

void setup() {
  lcd.begin(16,2);
  lcd.print("Loading...");
  lcd.setCursor(0, 1);
  lcd.print("Please Wait");
  Serial.begin(9600);
  dht.begin();
  pinMode(PIN_PWM, OUTPUT);
  pinMode(PIN_RPM, INPUT);
  pinMode(PIN_UP, INPUT);
  pinMode(PIN_DOWN, INPUT);
  digitalWrite(PIN_RPM, HIGH);
  attachInterrupt(digitalPinToInterrupt(PIN_RPM), rpm_fan, FALLING);
}

void loop() {
  if(dauer_low * 10 != 0){
    digitalWrite(PIN_PWM, LOW);
    delayMicroseconds(dauer_low * 10);
  }

  if(dauer_high * 10 != 0){
    digitalWrite(PIN_PWM, HIGH);
    delayMicroseconds(dauer_high * 10);
  }

  while (Serial.available()){ //read message
    characterInput = Serial.read();
    if(characterInput == '$') {
      if(eingabe.indexOf("PWM:") >= 0) {
        eingabe = eingabe.substring(4);
        int fan = eingabe.toInt(); // 0 - 9 
        if(fan == 0)
          fan = 10;
        else
          fan = 10 - fan;
        //set the pwm
        dauer_low = fan;
        dauer_high = baseTime - fan;
      }
      if(eingabe.indexOf("DISPLAY1:") >= 0) {
        display1 = eingabe.substring(9);
      }
      if(eingabe.indexOf("DISPLAY2:") >= 0) {
        display2 = eingabe.substring(9);
      }
      eingabe = "";
    } else {
      eingabe.concat(characterInput);
    }
  }

  if (millis() - letzte_ausgabe >= UPDATE_ZYKLUS){
     // detachin interrupt
    detachInterrupt(0);
    // calculate rpm
    rpm = counter_rpm * (60 / ANZAHL_INTERRUPTS);
    Serial.print("RPM:");
    Serial.print(rpm);
    Serial.print("$");
    int buttonUp = digitalRead(PIN_UP);
    int buttonDown = digitalRead(PIN_DOWN);
    if(buttonUp == HIGH) 
      Serial.print("BUTTONUP:ON$");
    if(buttonDown == HIGH)
      Serial.print("BUTTONDOWN:ON$");
    float val = analogRead(A0);
    float u = val * 5.0 / 1023.0;
    float r = 4700.0 * u /(5.0 - u);
    float e = pow(r/1000.0, -1.31022)*210.9143;
  
    // Reading temperature or humidity takes about 250 milliseconds!
    // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
    float h = dht.readHumidity();
    // Read temperature as Celsius (the default)
    float t = dht.readTemperature();
  
    Serial.print("HUMIDITY:");
    Serial.print(h);
    Serial.print("$");
    Serial.print("TEMP:");
    Serial.print(t);
    Serial.print("$");
    Serial.print("LUX:");
    Serial.print(e);
    Serial.print("$");
    
  
    // Counter 
    counter_rpm = 0;
    // new time for interrupt
    letzte_ausgabe = millis();
    //update display
    lcd.clear();
    lcd.print(display1);
    lcd.setCursor(0,1);
    lcd.print(display2);
    // reactivate interrupt
    attachInterrupt(0, rpm_fan, FALLING);
  } 
}

// Interrupt zaehlt den RPM-Counter hoch
void rpm_fan(){
  counter_rpm++;
}


