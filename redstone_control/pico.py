# Imports
from machine import Pin
from neopixel import NeoPixel
import neopixel
import time
import urequests
import network

wlan = network.WLAN(network.STA_IF)
wlan.active(True)
wlan.connect("WiFi<Dom", "qnet1392ef")


while not wlan.isconnected():
    time.sleep(0.5)
    print("Connecting...")
print("Connected to WiFi")

button = Pin(5, Pin.IN, Pin.PULL_UP)
LED_COUNT = 15
np = neopixel.NeoPixel(Pin(2), LED_COUNT,timing=(350, 900, 800, 450))
BRIGHTNESS = 1.0
last_poll = time.ticks_ms()
last_state = ""

BASE = "your pc ip:9090"
def led_off():
    for i in range(LED_COUNT):
        np[i] = 0,0,0
    np.write()
def led_colors(text):
    parts = text.split(",")
    for i in range(LED_COUNT):
        h = parts[i]
        r = int(int(h[0:2], 16)* BRIGHTNESS)
        g = int(int(h[2:4], 16)* BRIGHTNESS)
        b = int(int(h[4:6], 16)* BRIGHTNESS)
        np[i] = (g, b, r)
    np.write()

led_off()


while True:
    if button.value() == 0:
        print("Sending request")
        try:
            r = urequests.get(BASE + "/press", headers={"Connection": "close"})
            r.close()
        except Exception as e:
            print("Button error:", e)
        time.sleep(0.5)

    if time.ticks_diff(time.ticks_ms(), last_poll) > 500:
        last_poll = time.ticks_ms()
        try:
            r = urequests.get(BASE + "/leds", headers={"Connection": "close"})
            state = r.text.strip()
            r.close()
            if state != last_state:
                last_state = state
                if state == "off":
                    led_off()
                else:
                    led_colors(state)
                print("LED:", state)
        except Exception as e:
            print("LED poll error:", e)

    time.sleep(0.05)


        






