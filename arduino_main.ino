#include <SoftwareSerial.h>

#define EOL 125
#define SOL 123

#define RPIN 3
#define GPIN 5
#define BPIN 6

const int led = 13;
char val;
char buffer[3];

char r = 0;
char g = 0;
char b = 0;

float const convert = 255.0/100.0;
float const ALPHA = .99;

float r_curr = 0;
float g_curr = 0;
float b_curr = 0;

int pwm_r = 0;
int pwm_g = 0;
int pwm_b = 0;

int loc = -1;
char next_read = 0;


void setup() {
  Serial.begin(9600);
  pinMode(led,OUTPUT);
  pinMode(RPIN,OUTPUT);
  pinMode(GPIN,OUTPUT);
  pinMode(BPIN,OUTPUT);
}

void loop() {
  if(Serial.available())
  {
    val = Serial.read();
    next_read = 1;
  }
  if( next_read == 1)
  {
    if (val == SOL)
    {
      loc = -1;
    }
    if (loc != -1 && loc < 2)
    {
      buffer[loc] = val;
    }
    loc++;
    next_read = 0;
  }
  r = buffer[0];
  g = buffer[1];
  b = buffer[2];
  
  r_curr = ALPHA*r_curr + (1.0-ALPHA)*((float) r);
  g_curr = ALPHA*g_curr + (1.0-ALPHA)*((float) g);
  b_curr = ALPHA*b_curr + (1.0-ALPHA)*((float) b);
  
  pwm_r = (int)(convert*r_curr);
  pwm_g = (int)(convert*g_curr);
  pwm_b = (int)(convert*b_curr);
  
  analogWrite(RPIN,pwm_r);
  analogWrite(GPIN,pwm_g);
  analogWrite(BPIN,pwm_b);
  
  if (r == 'H') {
    digitalWrite(led,HIGH);
  } else {
    digitalWrite(led,LOW);
  }

}

