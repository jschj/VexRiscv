//#include "stddefs.h"
#include <stdint.h>

#include "murax.h"

void print(const char*str){
	while(*str){
		uart_write(UART,*str);
		str++;
	}
}
void println(const char*str){
	print(str);
	uart_write(UART,'\n');
}

void delay(uint32_t loops){
	for(int i=0;i<loops;i++){
		int tmp = GPIO_A->OUTPUT;
	}
}

void main() {
    GPIO_A->OUTPUT_ENABLE = 0x000000FF;
	GPIO_A->OUTPUT = 0x00000001;
    
	// SEVEN_SEGMENT[0].value = 1;
	// SEVEN_SEGMENT[1].value = 3;
	// SEVEN_SEGMENT[2].value = 3;
	// SEVEN_SEGMENT[3].value = 7;

	//println("hello world arty a7 v1");
    
	// const int nleds = 4;
	// const int nloops = 2000000; // too slow for simulation
	const int nloops = 20000;

	// uint32_t s[4] = {0};

	// uint8_t ledByte = 0x1;
	// uint8_t shiftDir = 1;

    while(1){
		for (unsigned int i = 0; i < 8; ++i) {
			GPIO_A->OUTPUT = 1 << i;
			delay(nloops);
		}

		for (unsigned int i = 0; i < 8; ++i) {
			GPIO_A->OUTPUT = 0x80 >> i;
			delay(nloops);
		}

		// SEVEN_SEGMENT[0].value = s[0];
		// SEVEN_SEGMENT[1].value = s[1];
		// SEVEN_SEGMENT[2].value = s[2];
		// SEVEN_SEGMENT[3].value = s[3];

    	// s[0] = (s[0] + 1) % 8;
		// s[1] = (s[1] + 1) % 8;
		// s[2] = (s[2] + 1) % 8;
		// s[3] = (s[3] + 1) % 8;

		// delay(nloops);
    }
}

void irqCallback(){
}
