#include <LiquidCrystal.h>
#define LCD_LIGHT_PIN 10
#define LCD_WIDTH  16
#define btnRIGHT  0
#define btnUP     1
#define btnDOWN   2
#define btnLEFT   3
#define btnSELECT 4
#define btnNONE   5

// IMPORTANT!!!!
//
//
// REQUIRES TO WORK: ARDUINO UNO BOARD + LCD KEYPAD
//
//
//
//

LiquidCrystal lcd(8, 9, 4, 5, 6, 7);

String valueTitle     = "USB CONNECTED:  ";
String valueKeyUp     = "KEY UP          ";
String valueKeyDown   = "KEY DOWN        ";
String valueKeyRight  = "KEY RIGHT       ";
String valueKeyLeft   = "KEY LEFT        ";
String valueKeySelect = "KEY SELECT      ";

/* SPANISH VALUES
String valueTitle     = "USB CONECTADO:  ";
String valueKeyUp     = "TECLA ARRIBA    ";
String valueKeyDown   = "TECLA ABAJO     ";
String valueKeyRight  = "TECLA DERECHA   ";
String valueKeyLeft   = "TECLA IZQUIERDA ";
String valueKeySelect = "TECLA SELECT    ";
*/

/* PORTUGUESE VALUES
String valueTitle     = "USB CONECTADO:  ";
String valueKeyUp     = "CHAVE ACIMA     ";
String valueKeyDown   = "CHAVE BAIXO     ";
String valueKeyRight  = "CHAVE DIREITO   ";
String valueKeyLeft   = "CHAVE ESQUERDA  ";
String valueKeySelect = "CHAVE SELECT    ";
*/

/* ITALIAN VALUES
String valueTitle     = "USB CONNESSO:   ";
String valueKeyUp     = "CHIAVE SOPRA    ";
String valueKeyDown   = "CHIAVE GIU      ";
String valueKeyRight  = "CHIAVE DESTRA   ";
String valueKeyLeft   = "CHIAVE SINISTRA ";
String valueKeySelect = "CHIAVE SELECT   ";
*/

boolean screenIsOn = true;
int lcd_key     = 0;
int adc_key_in  = 0;
String content  = "";
  
void setup()
  {
  Serial.begin(9600);
  lcd.begin(16, 2);
  }
  
void loop()
  {
  lcd.setCursor(0,0);
  lcd.print(valueTitle);
  lcd.setCursor(0,1);
  lcd.print("                ");
  lcd_key = read_LCD_buttons();
  
  switch (lcd_key)
    {
    case btnRIGHT:
      {
      sendText(valueKeyRight,"arduinokeyright");
      break;
      }

    case btnLEFT:
      {
      sendText(valueKeyLeft,"arduinokeyleft");
      break;
      }

    case btnUP:
      {
      sendText(valueKeyUp,"arduinokeyup");
      break;
      }
      
    case btnDOWN:
      {
      sendText(valueKeyDown,"arduinokeydown");
      break;
      }
      
    case btnSELECT:
      {
      if (screenIsOn==true)
        {
        lcd.noDisplay();
        analogWrite(LCD_LIGHT_PIN,0);
        screenIsOn=false;
        delay(500);
        }
        else
        {
        lcd.display();
        analogWrite(LCD_LIGHT_PIN,255);
        screenIsOn=true;
        delay(500);
        }
      //sendText(valueKeySelect,"arduinokeyselect");
      break;
      }
    }
    
  int incomingByte = 0;
  char character;

  while(Serial.available())
    {
    character = Serial.read();
    content.concat(character);
    }

  //CHECKING FOR SEPARATOR
  if (content.indexOf("|||")>0)
    {
    //DEFINES A STRING TO STRIP
    String value = content.substring(0,content.indexOf("|||"));

    //THE LCD TURNS ON
    lcd.display();
    analogWrite(LCD_LIGHT_PIN,255);
    
    //WE GET THE FIRST LINE
    String line1 = value.substring(0,value.indexOf(":") + 1);
    if (line1.length()<LCD_WIDTH)
      {
      for(int counter = line1.length() ; counter<LCD_WIDTH; counter++)
        {
        line1 = line1 + " ";
        }
      }
      else
      {
      line1 = line1.substring(0,LCD_WIDTH);
      }

    //WE GET THE SECOND LINE
    String line2 = value.substring(value.indexOf(":") + 1,value.length());
    if (line2.length()<LCD_WIDTH)
      {
      for(int counter = line2.length() ; counter<LCD_WIDTH; counter++)
        {
        line2 = line2 + " ";
        }
      }
      else
      {
      line2 = line2.substring(0,LCD_WIDTH);
      }
    
    //DATA IS PRINTED
    lcd.setCursor(0,0);
    lcd.print(line1);
    lcd.setCursor(0,1);
    lcd.print(line2);

    //PUT THE REST OF THE DATA (IF ANY) IN THE VARIABLE
    content = content.substring(content.indexOf("|||") + 3,content.length());

    //WAIT A FEW MOMENTS FOR USER TO READ THE NOTIFICATION
    delay(4000);
    
    //IF THE SCREEN WAS OFF, GOES BACK TO OFF
    if (screenIsOn==false)
      {
      lcd.noDisplay();
      analogWrite(LCD_LIGHT_PIN,0);
      }
    lcd.clear();
    }
  }
  
int read_LCD_buttons()
  {
  adc_key_in = analogRead(0);
  if (adc_key_in > 1000) return btnNONE;
  if (adc_key_in < 50)   return btnRIGHT;  
  if (adc_key_in < 250)  return btnUP; 
  if (adc_key_in < 450)  return btnDOWN; 
  if (adc_key_in < 650)  return btnLEFT; 
  if (adc_key_in < 850)  return btnSELECT;  
  return btnNONE;
  }

void sendText(String a, String b)
  {
  lcd.setCursor(0,1);
  lcd.print(a);
  Serial.println(b);
  delay(200);
  }
