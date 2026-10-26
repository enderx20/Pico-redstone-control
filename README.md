
# Pico redstone control

This project is all about using a tact switch to power on your redstone in Minecraft and also you can use Minecraft to power on leds more deeply about the functionality later.

I made this project beacuse i thought that it will be cool to use a real life button to trigger Minecraft redstone.

The code is written half in java and half in micro Python it uses Http server to connect your Pc and Pico together.

Here is a quick guide on how to set it up:




## Setup

For this project you will need:

* A micro controller that can connect to WiFi (For this guide i used Rasberry Pi pico 2w)

* Tact switches

* Led strip i used 15 led one

* Some wires to connect evrything together 

* Minecraft Java Edtion with fabric loader installed on version 26.3 you can search a guide how to do that

1.Connect evrything as shown on the image


![Circuit](https://raw.githubusercontent.com/enderx20/Pico-redstone-control/refs/heads/main/circuit_image.png)

Black wire goes to ground and blue to gpio 5 you can also connect it to 3v3 and then later in the code you will need to change PULL_UP to PULL_DOWN

2.Next connect the led strip 

![Circuit2](https://raw.githubusercontent.com/enderx20/Pico-redstone-control/refs/heads/main/circuit_image%20(1).png)

Connect the red wire to the Vbus(5v) the black one to ground and the yellow one to gpio 2 ,make sure to first check what is the layout of your led strip pins becuase it might be diffrent from the one presented and you can burn your led.

3.Download RedstoneControl.jar and pico.py from the [release page](https://github.com/enderx20/Pico-redstone-control/releases/tag/1.0) 

4.Open pico.py and change this things:

* In the wlan.connect function type in your WiFi information

* Change the LED_COUNT to whatever leds you have on your strip

* And lastly change "your ip" in variable BASE to your pc ip adress
**NOTE that both devices need to be in the same network**.

5.Put the mod in your mod folder and launch the game then place a jigsaw block and a stone button on it.Press the button connected to the rasberry pi and the button should get powered in game (if the button does not work check the logs in your launcher or micro python code editor)

6.For the leds you need to build a line of 15 blocks of whatever concrete color you want (doesn't matter how many leds you have becuase i didn't add a command to change it) and put a copper bulb at the start of the line then turn it on and observe the leds liting up.

### Examples

![blocks](https://raw.githubusercontent.com/enderx20/Pico-redstone-control/refs/heads/main/led%20in%20blocks.png)

![strip](https://raw.githubusercontent.com/enderx20/Pico-redstone-control/refs/heads/main/leds%20strip.jpeg)
    
